import {getFirestore, FieldValue} from "firebase-admin/firestore";
import {onDocumentWritten} from "firebase-functions/v2/firestore";
import * as logger from "firebase-functions/logger";

const db = getFirestore("default");

export const onNodeJobWrite = onDocumentWritten({
  document: "projects/{projectId}/nodes/{nodeId}/jobs/{jobId}",
  database: "default",
}, async (event) => {
  const {projectId, nodeId, jobId} = event.params;

  // Check if document was just created, updated, or deleted
  const beforeData = event.data?.before.exists ? event.data.before.data() : null;
  const afterData = event.data?.after.exists ? event.data.after.data() : null;

  const beforeProgress = beforeData?.progress || 0;
  const afterProgress = afterData?.progress || 0;
  const delta = afterProgress - beforeProgress;

  // If progress hasn't changed (e.g. some other field updated), we don't need to propagate
  if (delta === 0 && beforeData && afterData) {
    return;
  }

  const jobTemplateId = afterData?.jobTemplateId || beforeData?.jobTemplateId;
  if (!jobTemplateId) {
    logger.error("No jobTemplateId found for job", jobId);
    return;
  }

  // 1. Calculate new localProgress for the node
  const jobsSnapshot = await db.collection(`projects/${projectId}/nodes/${nodeId}/jobs`).get();
  let totalLocalProgress = 0;
  let localJobsCount = 0;

  jobsSnapshot.forEach((doc) => {
    const data = doc.data();
    if (data.isDeleted !== true) {
      const progress = data.progress || 0;
      totalLocalProgress += progress;
      localJobsCount++;
    }
  });

  const newLocalProgress = localJobsCount > 0 ? Math.round(totalLocalProgress / localJobsCount) : 0;

  // 2. Get the node to find its ancestors
  const nodeRef = db.collection(`projects/${projectId}/nodes`).doc(nodeId);
  const nodeSnap = await nodeRef.get();
  if (!nodeSnap.exists) {
    logger.error("Node not found", nodeId);
    return;
  }

  const nodeData = nodeSnap.data();
  // ancestors array includes the node itself. Propagate only to actual parents.
  const ancestors: string[] = nodeData?.ancestors || [];
  const parentAncestors = ancestors.filter((id) => id !== nodeId);

  const batch = db.batch();

  // Update node's localProgress
  batch.update(nodeRef, {localProgress: newLocalProgress});

  // Update all parent ancestors
  parentAncestors.forEach((ancestorId) => {
    const ancestorRef = db.collection(`projects/${projectId}/nodes`).doc(ancestorId);

    // We use FieldValue.increment to safely add the delta
    batch.update(ancestorRef, {
      totalDescendantProgress: FieldValue.increment(delta),
      [`jobStats.${jobTemplateId}.totalProgress`]: FieldValue.increment(delta),
    });
  });

  await batch.commit();
  logger.info(`Propagated progress delta ${delta} from job ${jobId} to ${parentAncestors.length} ancestors.`);
});
