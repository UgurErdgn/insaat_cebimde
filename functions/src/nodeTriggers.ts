import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as logger from "firebase-functions/logger";

const db = getFirestore("default");

export const toggleNodeDelete = onCall(async (request) => {
  const { projectId, nodeId, isDeleted } = request.data;
  const uid = request.auth?.uid;

  if (!uid) {
    throw new HttpsError("unauthenticated", "User must be logged in.");
  }
  if (!projectId || !nodeId || typeof isDeleted !== "boolean") {
    throw new HttpsError("invalid-argument", "Missing required parameters.");
  }

  const nodeRef = db.collection(`projects/${projectId}/nodes`).doc(nodeId);
  const nodeSnap = await nodeRef.get();

  if (!nodeSnap.exists) {
    throw new HttpsError("not-found", "Node does not exist.");
  }

  const nodeData = nodeSnap.data();
  // Eğer zaten istenen durumdaysa işlem yapma
  if (nodeData?.isDeleted === isDeleted) {
    return { success: true, message: "Node is already in the requested state." };
  }

  const bulkWriter = db.bulkWriter();

  // 1. Kendisini güncelle
  bulkWriter.update(nodeRef, { isDeleted: isDeleted });

  // 1.1 Kendi altındaki işleri de güncelle
  const nodeJobsSnap = await nodeRef.collection("jobs").get();
  nodeJobsSnap.forEach((jobDoc) => {
    bulkWriter.update(jobDoc.ref, { isDeleted: isDeleted });
  });

  // 2. Tüm Alt Mülkleri (Descendants) bul ve güncelle
  const descendantsSnapshot = await db.collection(`projects/${projectId}/nodes`)
    .where("ancestors", "array-contains", nodeId)
    .get();

  for (const doc of descendantsSnapshot.docs) {
    bulkWriter.update(doc.ref, { isDeleted: isDeleted });

    // 2.1 Her bir alt mülkün işlerini de güncelle
    const descJobsSnap = await doc.ref.collection("jobs").get();
    descJobsSnap.forEach((jobDoc) => {
      bulkWriter.update(jobDoc.ref, { isDeleted: isDeleted });
    });
  }

  // 3. Ataların (Ancestors) Puanlarını Güncelle
  // Kural: Eğer siliniyorsa (isDeleted: true), puanları EKSİ (-) olarak düş.
  // Eğer geri getiriliyorsa (isDeleted: false), puanları ARTI (+) olarak geri ekle.
  const multiplier = isDeleted ? -1 : 1;
  const ancestors: string[] = nodeData?.ancestors || [];
  const parentAncestors = ancestors.filter((id) => id !== nodeId);

  // Mülkün o anki sahip olduğu (sadece altındaki) toplam veriler:
  let totalDescendantJobs = nodeData?.totalDescendantJobs || 0;
  let totalDescendantProgress = nodeData?.totalDescendantProgress || 0;
  const jobStats: any = nodeData?.jobStats ? JSON.parse(JSON.stringify(nodeData.jobStats)) : {};

  // Kendi işlerini hesapla ve dahil et
  // (Çünkü kendi işleri parents için "descendant" sayılır)
  nodeJobsSnap.forEach((jobDoc) => {
    const jobData = jobDoc.data();
    // Eğer node siliniyorsa (isDeleted=true), şu an aktif olanları (isDeleted=false) bulup eksiye çevireceğiz.
    // Eğer node geri getiriliyorsa (isDeleted=false), şu an silinmiş olanları (isDeleted=true) bulup artıya çevireceğiz.
    if (!!jobData.isDeleted !== isDeleted) {
      totalDescendantJobs += 1;
      totalDescendantProgress += (jobData.progress || 0);
      const jId = jobData.jobTemplateId || jobDoc.id;
      if (!jobStats[jId]) {
        jobStats[jId] = { count: 0, totalProgress: 0 };
      }
      jobStats[jId].count += 1;
      jobStats[jId].totalProgress += (jobData.progress || 0);
    }
  });

  if (parentAncestors.length > 0 && (totalDescendantJobs > 0 || totalDescendantProgress > 0)) {
    parentAncestors.forEach((ancestorId) => {
      const ancestorRef = db.collection(`projects/${projectId}/nodes`).doc(ancestorId);
      const updates: any = {};

      if (totalDescendantJobs > 0) {
        updates.totalDescendantJobs = FieldValue.increment(totalDescendantJobs * multiplier);
      }
      if (totalDescendantProgress > 0) {
        updates.totalDescendantProgress = FieldValue.increment(totalDescendantProgress * multiplier);
      }

      // jobStats güncellemesi
      for (const [jId, stats] of Object.entries(jobStats) as [string, any][]) {
        if (stats.count > 0) {
          updates[`jobStats.${jId}.count`] = FieldValue.increment(stats.count * multiplier);
        }
        if (stats.totalProgress > 0) {
          updates[`jobStats.${jId}.totalProgress`] = FieldValue.increment(stats.totalProgress * multiplier);
        }
      }

      bulkWriter.update(ancestorRef, updates);
    });
  }

  await bulkWriter.close();

  logger.info(`Node ${nodeId} isDeleted set to ${isDeleted} with ${descendantsSnapshot.size} descendants.`);
  return { success: true, updatedCount: descendantsSnapshot.size + 1 };
});
