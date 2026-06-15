import {getFirestore, FieldValue, AggregateField} from "firebase-admin/firestore";
import {onDocumentWritten} from "firebase-functions/v2/firestore";
import * as logger from "firebase-functions/logger";

const db = getFirestore("default");

export const onNodeTemplateAssign = onDocumentWritten({
  document: "projects/{projectId}/nodes/{nodeId}",
  database: "default",
}, async (event) => {
  const {projectId, nodeId} = event.params;

  const beforeData = event.data?.before.exists ? event.data.before.data() : null;
  const afterData = event.data?.after.exists ? event.data.after.data() : null;

  const oldTemplateId = beforeData?.propertyTemplateId;
  const newTemplateId = afterData?.propertyTemplateId;

  // We only care if the propertyTemplateId has changed
  if (oldTemplateId === newTemplateId) {
    return;
  }

  const ancestors: string[] = afterData?.ancestors || beforeData?.ancestors || [];
  const parentAncestors = ancestors.filter((id) => id !== nodeId);

  let newJobIds: string[] = [];
  if (newTemplateId) {
    const templateSnap = await db.collection(`projects/${projectId}/propertyTemplates`).doc(newTemplateId).get();
    if (templateSnap.exists) {
      newJobIds = templateSnap.data()?.jobTemplateIds || [];
    }
  }

  const existingJobsSnap = await db.collection(`projects/${projectId}/nodes/${nodeId}/jobs`).get();
  const existingJobsData: Record<string, any> = {};
  existingJobsSnap.forEach((doc) => {
    existingJobsData[doc.id] = doc.data();
  });

  const jobsToHide: string[] = [];
  const jobsToShow: string[] = [];
  const jobsToCreate: string[] = [];

  for (const [jId, jobData] of Object.entries(existingJobsData)) {
    const isCurrentlyDeleted = jobData.isDeleted === true;
    if (!newJobIds.includes(jId)) {
      if (!isCurrentlyDeleted) {
        jobsToHide.push(jId);
      }
    } else {
      if (isCurrentlyDeleted) {
        jobsToShow.push(jId);
      }
    }
  }

  for (const jId of newJobIds) {
    if (!existingJobsData[jId]) {
      jobsToCreate.push(jId);
    }
  }

  if (jobsToHide.length === 0 && jobsToShow.length === 0 && jobsToCreate.length === 0) {
    return;
  }

  const batch = db.batch();
  let nodeJobsCountDelta = 0;
  let nodeProgressDelta = 0;
  const nodeJobDeltas: Record<string, { count: number, progress: number, name?: string, category?: string }> = {};

  // 1. Hide jobs
  for (const jId of jobsToHide) {
    const jobRef = db.collection(`projects/${projectId}/nodes/${nodeId}/jobs`).doc(jId);
    batch.update(jobRef, { isDeleted: true });

    const progress = existingJobsData[jId].progress || 0;
    nodeJobsCountDelta -= 1;
    nodeProgressDelta -= progress;
    nodeJobDeltas[jId] = { count: -1, progress: -progress };
  }

  // 2. Show jobs
  for (const jId of jobsToShow) {
    const jobRef = db.collection(`projects/${projectId}/nodes/${nodeId}/jobs`).doc(jId);
    batch.update(jobRef, { isDeleted: false });

    const progress = existingJobsData[jId].progress || 0;
    nodeJobsCountDelta += 1;
    nodeProgressDelta += progress;
    nodeJobDeltas[jId] = {
      count: 1,
      progress: progress,
      name: existingJobsData[jId].name,
      category: existingJobsData[jId].category,
    };
  }

  // 3. Create jobs
  if (jobsToCreate.length > 0) {
    for (const jId of jobsToCreate) {
      const jobSnap = await db.collection(`projects/${projectId}/jobTemplates`).doc(jId).get();
      if (jobSnap.exists) {
        const jobData = jobSnap.data();
        const jobRef = db.collection(`projects/${projectId}/nodes/${nodeId}/jobs`).doc(jId);

        batch.set(jobRef, {
          id: jId,
          nodeId: nodeId,
          jobTemplateId: jId,
          category: jobData?.category || "",
          name: jobData?.name || "",
          progress: 0,
          isDeleted: false,
        });

        nodeJobsCountDelta += 1;
        nodeJobDeltas[jId] = {
          count: 1,
          progress: 0,
          name: jobData?.name || "",
          category: jobData?.category || "",
        };
      }
    }
  }

  // 4. Update node's localProgress
  let totalLocalProgress = 0;
  let localJobsCount = 0;

  for (const [jId, jobData] of Object.entries(existingJobsData)) {
    let isVisible = jobData.isDeleted !== true;
    if (jobsToHide.includes(jId)) isVisible = false;
    if (jobsToShow.includes(jId)) isVisible = true;

    if (isVisible) {
      totalLocalProgress += (jobData.progress || 0);
      localJobsCount++;
    }
  }
  localJobsCount += jobsToCreate.length;

  const newLocalProgress = localJobsCount > 0 ? Math.round(totalLocalProgress / localJobsCount) : 0;
  const nodeRef = db.collection(`projects/${projectId}/nodes`).doc(nodeId);
  batch.update(nodeRef, { localProgress: newLocalProgress });

  // 5. Update parent ancestors
  if (nodeJobsCountDelta !== 0 || nodeProgressDelta !== 0 || Object.keys(nodeJobDeltas).length > 0) {
    parentAncestors.forEach((ancestorId) => {
      const ancestorRef = db.collection(`projects/${projectId}/nodes`).doc(ancestorId);
      const updates: any = {};

      if (nodeJobsCountDelta !== 0) {
        updates.totalDescendantJobs = FieldValue.increment(nodeJobsCountDelta);
      }
      if (nodeProgressDelta !== 0) {
        updates.totalDescendantProgress = FieldValue.increment(nodeProgressDelta);
      }

      for (const [jId, delta] of Object.entries(nodeJobDeltas)) {
        if (delta.count !== 0) {
          updates[`jobStats.${jId}.totalCount`] = FieldValue.increment(delta.count);
        }
        if (delta.progress !== 0) {
          updates[`jobStats.${jId}.totalProgress`] = FieldValue.increment(delta.progress);
        }
        if (delta.name && delta.count > 0) {
          updates[`jobStats.${jId}.name`] = delta.name;
          updates[`jobStats.${jId}.category`] = delta.category;
        }
      }

      batch.update(ancestorRef, updates);
    });
  }

  await batch.commit();
  logger.info(`Processed template change for node ${nodeId}`);
});

export const onPropertyTemplateUpdate = onDocumentWritten({
  document: "projects/{projectId}/propertyTemplates/{templateId}",
  database: "default",
}, async (event) => {
  const { projectId, templateId } = event.params;

  const beforeData = event.data?.before.exists ? event.data.before.data() : null;
  const afterData = event.data?.after.exists ? event.data.after.data() : null;

  const oldJobIds: string[] = beforeData?.jobTemplateIds || [];
  const newJobIds: string[] = afterData?.jobTemplateIds || [];

  const addedJobIds = newJobIds.filter((id) => !oldJobIds.includes(id));
  const removedJobIds = oldJobIds.filter((id) => !newJobIds.includes(id));

  if (addedJobIds.length === 0 && removedJobIds.length === 0) {
    return;
  }

  const nodesSnap = await db.collection(`projects/${projectId}/nodes`)
    .where("propertyTemplateId", "==", templateId)
    .get();

  if (nodesSnap.empty) {
    return;
  }

  const jobTemplatesData: Record<string, any> = {};
  for (const jId of addedJobIds) {
    const jSnap = await db.collection(`projects/${projectId}/jobTemplates`).doc(jId).get();
    if (jSnap.exists) {
      jobTemplatesData[jId] = jSnap.data() || {};
    }
  }

  const batches = [db.batch()];
  let opCount = 0;

  const getBatch = () => {
    if (opCount >= 400) {
      batches.push(db.batch());
      opCount = 0;
    }
    opCount++;
    return batches[batches.length - 1];
  };

  for (const nodeDoc of nodesSnap.docs) {
    const nodeData = nodeDoc.data();
    const nodeId = nodeDoc.id;
    const ancestors = nodeData.ancestors || [];
    const parentAncestors = ancestors.filter((id: string) => id !== nodeId);

    let nodeJobsCountDelta = 0;
    let nodeProgressDelta = 0;
    const nodeJobDeltas: Record<string, { count: number, progress: number, name?: string, category?: string }> = {};

    // Process Additions
    for (const jId of addedJobIds) {
      const jobData = jobTemplatesData[jId];
      if (!jobData) continue;

      const jobRef = db.collection(`projects/${projectId}/nodes/${nodeId}/jobs`).doc(jId);
      const jobSnap = await jobRef.get();

      if (jobSnap.exists) {
        const currentJobData = jobSnap.data();
        if (currentJobData?.isDeleted) {
          getBatch().update(jobRef, { isDeleted: false });
          nodeJobsCountDelta += 1;
          nodeProgressDelta += (currentJobData.progress || 0);
          nodeJobDeltas[jId] = { count: 1, progress: currentJobData.progress || 0, name: currentJobData.name, category: currentJobData.category };
        }
      } else {
        getBatch().set(jobRef, {
          id: jId,
          nodeId: nodeId,
          jobTemplateId: jId,
          category: jobData.category || "",
          name: jobData.name || "",
          progress: 0,
          isDeleted: false,
        });
        nodeJobsCountDelta += 1;
        nodeJobDeltas[jId] = { count: 1, progress: 0, name: jobData.name || "", category: jobData.category || "" };
      }
    }

    // Process Removals
    for (const jId of removedJobIds) {
      const jobRef = db.collection(`projects/${projectId}/nodes/${nodeId}/jobs`).doc(jId);
      const jobSnap = await jobRef.get();

      if (jobSnap.exists) {
        const currentJobData = jobSnap.data();
        if (!currentJobData?.isDeleted) {
          getBatch().update(jobRef, { isDeleted: true });
          nodeJobsCountDelta -= 1;
          nodeProgressDelta -= (currentJobData?.progress || 0);
          nodeJobDeltas[jId] = { count: -1, progress: -(currentJobData?.progress || 0) };
        }
      }
    }

    // Recalculate localProgress using aggregation on the old state, plus deltas
    const aggSnap = await db.collection(`projects/${projectId}/nodes/${nodeId}/jobs`)
      .where("isDeleted", "==", false)
      .aggregate({
        totalProg: AggregateField.sum("progress"),
        count: AggregateField.count(),
      }).get();

    const oldTotalProg = aggSnap.data().totalProg || 0;
    const oldCount = aggSnap.data().count || 0;

    const newTotalProg = oldTotalProg + nodeProgressDelta;
    const newCount = oldCount + nodeJobsCountDelta;
    const newLocalProgress = newCount > 0 ? Math.round(newTotalProg / newCount) : 0;

    const nodeRefForLocal = db.collection(`projects/${projectId}/nodes`).doc(nodeId);
    getBatch().update(nodeRefForLocal, { localProgress: newLocalProgress });

    // Update Ancestors
    if (nodeJobsCountDelta !== 0 || nodeProgressDelta !== 0 || Object.keys(nodeJobDeltas).length > 0) {
      parentAncestors.forEach((ancestorId: string) => {
        const ancestorRef = db.collection(`projects/${projectId}/nodes`).doc(ancestorId);
        const updates: any = {};
        if (nodeJobsCountDelta !== 0) {
          updates.totalDescendantJobs = FieldValue.increment(nodeJobsCountDelta);
        }
        if (nodeProgressDelta !== 0) {
          updates.totalDescendantProgress = FieldValue.increment(nodeProgressDelta);
        }

        for (const [jId, delta] of Object.entries(nodeJobDeltas)) {
          if (delta.count !== 0) {
            updates[`jobStats.${jId}.totalCount`] = FieldValue.increment(delta.count);
          }
          if (delta.progress !== 0) {
            updates[`jobStats.${jId}.totalProgress`] = FieldValue.increment(delta.progress);
          }
          if (delta.name && delta.count > 0) {
            updates[`jobStats.${jId}.name`] = delta.name;
            updates[`jobStats.${jId}.category`] = delta.category;
          }
        }

        getBatch().update(ancestorRef, updates);
      });
    }
  }

  for (const b of batches) {
    await b.commit();
  }

  logger.info(`Processed propertyTemplate update for ${templateId}. Affected ${nodesSnap.size} nodes.`);
});
