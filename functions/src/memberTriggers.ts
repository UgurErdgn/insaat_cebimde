import {getFirestore} from "firebase-admin/firestore";
import {onCall, HttpsError} from "firebase-functions/v2/https";
import * as logger from "firebase-functions/logger";

const db = getFirestore("default");

/**
 * Üyenin yetkilerini günceller (Callable Function).
 *
 * Güvenlik Katmanı (Subset Math):
 * 1. İsteği atan kişinin MANAGE_MEMBERS yetkisi var mı?
 * 2. Eklenen yetkiler, isteği atanın kendi yetkilerinin alt kümesi mi?
 * 3. Çıkarılan yetkiler, isteği atanın kendi yetkilerinin alt kümesi mi?
 * 4. Hedef kişi Owner ise DOKUNMA!
 *
 * Şantiye Senaryosu:
 * Şef, boyadaki ustanın yetkilerini genişletmek istiyor ama kendisinde
 * "Maliyetleri Gör" yetkisi yok. Şef bu yetkiyi ustaya ekleyemez.
 */
export const updateMemberPermissions = onCall(async (request) => {
  const {projectId, requesterId, targetUserId, newPermissions, newScopes, newRoleName} =
    request.data;
  const uid = request.auth?.uid;

  if (!uid || uid !== requesterId) {
    throw new HttpsError("unauthenticated", "Kimlik doğrulaması başarısız.");
  }

  if (!projectId || !targetUserId || !newPermissions) {
    throw new HttpsError("invalid-argument", "Eksik parametreler.");
  }

  const projectSnap = await db.collection("projects").doc(projectId).get();
  if (!projectSnap.exists) {
    throw new HttpsError("not-found", "Proje bulunamadı.");
  }

  const projectData = projectSnap.data();
  const members = projectData?.members || {};

  const requesterMember = members[requesterId];
  const targetMember = members[targetUserId];

  if (!requesterMember) {
    throw new HttpsError("permission-denied", "Bu projenin üyesi değilsiniz.");
  }

  if (!targetMember) {
    throw new HttpsError("not-found", "Hedef kullanıcı bu projenin üyesi değil.");
  }

  // Owner koruması — Owner'a kimse dokunamaz
  if (targetMember.isOwner) {
    throw new HttpsError(
      "permission-denied",
      "Proje sahibinin yetkileri değiştirilemez."
    );
  }

  const requesterPerms: string[] = requesterMember.permissions || [];

  // MANAGE_MEMBERS yetkisi kontrolü (Owner tüm yetkilere sahip)
  if (!requesterMember.isOwner && !requesterPerms.includes("MANAGE_MEMBERS")) {
    throw new HttpsError(
      "permission-denied",
      "Üye yönetimi yetkiniz bulunmuyor."
    );
  }

  // Subset kontrolü — Yeni yetkilerin tamamı requester'ın yetkilerinde olmalı
  if (!requesterMember.isOwner) {
    const invalidPerms = (newPermissions as string[]).filter(
      (p: string) => !requesterPerms.includes(p)
    );
    if (invalidPerms.length > 0) {
      throw new HttpsError(
        "permission-denied",
        `Sahip olmadığınız yetkileri atayamazsınız: ${invalidPerms.join(", ")}`
      );
    }
  }

  // Güncellemeyi yap
  const updatedMember = {
    ...targetMember,
    permissions: newPermissions,
    scopes: newScopes || targetMember.scopes || {nodes: [], categories: []},
    roleName: newRoleName || targetMember.roleName,
  };

  await db.collection("projects").doc(projectId).update({
    [`members.${targetUserId}`]: updatedMember,
  });

  // Users dökümanını da güncelle
  await db.collection("users").doc(targetUserId).set({
    projectPermissions: {
      [projectId]: {
        role: newRoleName || targetMember.roleName,
        canDelegate: (newPermissions as string[]).includes("INVITE"),
      },
    },
  }, { merge: true });

  logger.info(
    `Yetki güncellendi: ${targetUserId} @ ${projectId} by ${requesterId}`
  );
  return {success: true};
});

/**
 * Üyeyi projeden çıkarır (Callable Function).
 *
 * Güvenlik Katmanı:
 * 1. İsteği atan MANAGE_MEMBERS yetkisine sahip mi?
 * 2. Hedef kişi Owner mı? (Owner ATILAMAZ!)
 * 3. Hedef kişinin tüm yetkileri, isteği atanın alt kümesi mi?
 *    (Müdür, Şefi atabilir ama Şef, Müdürü atamaz)
 */
export const removeMember = onCall(async (request) => {
  const {projectId, requesterId, targetUserId} = request.data;
  const uid = request.auth?.uid;

  if (!uid || uid !== requesterId) {
    throw new HttpsError("unauthenticated", "Kimlik doğrulaması başarısız.");
  }

  if (!projectId || !targetUserId) {
    throw new HttpsError("invalid-argument", "Eksik parametreler.");
  }

  if (requesterId === targetUserId) {
    throw new HttpsError(
      "invalid-argument",
      "Kendinizi projeden çıkaramazsınız."
    );
  }

  const projectSnap = await db.collection("projects").doc(projectId).get();
  if (!projectSnap.exists) {
    throw new HttpsError("not-found", "Proje bulunamadı.");
  }

  const projectData = projectSnap.data();
  const members = projectData?.members || {};

  const requesterMember = members[requesterId];
  const targetMember = members[targetUserId];

  if (!requesterMember) {
    throw new HttpsError("permission-denied", "Bu projenin üyesi değilsiniz.");
  }

  if (!targetMember) {
    throw new HttpsError("not-found", "Hedef kullanıcı bu projenin üyesi değil.");
  }

  // Owner koruması
  if (targetMember.isOwner) {
    throw new HttpsError(
      "permission-denied",
      "Proje sahibi projeden çıkarılamaz."
    );
  }

  const requesterPerms: string[] = requesterMember.permissions || [];

  // MANAGE_MEMBERS yetkisi kontrolü
  if (!requesterMember.isOwner && !requesterPerms.includes("MANAGE_MEMBERS")) {
    throw new HttpsError(
      "permission-denied",
      "Üye yönetimi yetkiniz bulunmuyor."
    );
  }

  // Subset kontrolü — Hedefin tüm yetkileri requester'da olmalı
  if (!requesterMember.isOwner) {
    const targetPerms: string[] = targetMember.permissions || [];
    const cantRemove = targetPerms.filter(
      (p: string) => !requesterPerms.includes(p)
    );
    if (cantRemove.length > 0) {
      throw new HttpsError(
        "permission-denied",
        "Sizden daha geniş yetkilere sahip bir üyeyi çıkaramazsınız."
      );
    }
  }

  // Firestore'dan sil (FieldValue.delete() kullanarak)
  const {FieldValue} = await import("firebase-admin/firestore");
  await db.collection("projects").doc(projectId).update({
    [`members.${targetUserId}`]: FieldValue.delete(),
  });

  // Users dökümanından da kaldır
  try {
    await db.collection("users").doc(targetUserId).update({
      [`projectPermissions.${projectId}`]: FieldValue.delete(),
    });
  } catch (error) {
    logger.warn(`Users dokumanindan silinirken hata olustu (Belki de zaten yoktu): ${error}`);
  }

  logger.info(
    `Üye çıkarıldı: ${targetUserId} @ ${projectId} by ${requesterId}`
  );
  return {success: true};
});
