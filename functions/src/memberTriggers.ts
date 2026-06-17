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

  const requesterIsOwner = requesterMember.isOwner === true || requesterMember.owner === true;
  const targetIsOwner = targetMember.isOwner === true || targetMember.owner === true;

  // Owner koruması — Owner'a kimse dokunamaz
  if (targetIsOwner) {
    throw new HttpsError(
      "permission-denied",
      "Proje sahibinin yetkileri değiştirilemez."
    );
  }

  const requesterPerms: string[] = requesterMember.permissions || [];
  const requesterDelegablePerms: string[] = requesterMember.delegablePermissions || [];
  const newDelegablePermissions: string[] = request.data.newDelegablePermissions || [];

  // 1. MANAGE_MEMBERS kontrolü
  if (!requesterIsOwner && !requesterPerms.includes("MANAGE_MEMBERS")) {
    throw new HttpsError(
      "permission-denied",
      "Üye yetkilerini düzenleme izniniz yok (MANAGE_MEMBERS eksik)."
    );
  }

  // 2. Subset Kontrolü: Requester, sahip olmadığı bir permission'ı başkasına veremez
  if (!requesterIsOwner) {
    const invalidPerms = (newPermissions as string[]).filter((p) => !requesterDelegablePerms.includes(p));
    if (invalidPerms.length > 0) {
      throw new HttpsError(
        "permission-denied",
        `Devredilebilir yetkilerinizde olmayan izinleri atayamazsınız: ${invalidPerms.join(", ")}`
      );
    }
  }

  // 3. Subset Kontrolü: Requester, sahip olmadığı bir delegablePermission'ı başkasına veremez
  if (!requesterIsOwner) {
    const invalidDelegablePerms = newDelegablePermissions.filter((p) => !requesterDelegablePerms.includes(p));
    if (invalidDelegablePerms.length > 0) {
      throw new HttpsError(
        "permission-denied",
        `Devredilebilir yetkilerinizde olmayan devir izinlerini atayamazsınız: ${invalidDelegablePerms.join(", ")}`
      );
    }
  }

  // Scope Subset Kontrolü
  if (!requesterIsOwner) {
    const requesterScopes = requesterMember.scopes || { isRestricted: false, nodeCategories: {} };
    const reqScopes = newScopes || targetMember.scopes || { isRestricted: false, nodeCategories: {} };

    if (requesterScopes.isRestricted) {
      if (!reqScopes.isRestricted) {
        throw new HttpsError("permission-denied", "Kısıtlı erişime sahip olduğunuz için tam yetkili kapsam veremezsiniz.");
      }

      const reqNodes = Object.keys(reqScopes.nodeCategories || {});
      for (const nodeId of reqNodes) {
        const reqCats: string[] = reqScopes.nodeCategories[nodeId] || [];
        let allowedCats: string[] | null = null;

        if (requesterScopes.nodeCategories && requesterScopes.nodeCategories[nodeId]) {
          allowedCats = requesterScopes.nodeCategories[nodeId];
        } else {
          const nodeSnap = await db.collection("projects").doc(projectId).collection("nodes").doc(nodeId).get();
          if (nodeSnap.exists) {
            const ancestors: string[] = nodeSnap.data()?.ancestors || [];
            for (const ancId of [...ancestors].reverse()) {
              if (requesterScopes.nodeCategories && requesterScopes.nodeCategories[ancId]) {
                allowedCats = requesterScopes.nodeCategories[ancId];
                break;
              }
            }
          }
        }

        if (allowedCats === null) {
          throw new HttpsError("permission-denied", "Kapsam ihlali: Yetkiniz olmayan bir mülke erişim veremezsiniz.");
        }

        if (allowedCats.length > 0) {
          if (reqCats.length === 0) {
            throw new HttpsError("permission-denied", "Kapsam ihlali: Tüm işlere yetki veremezsiniz, yetkiniz kısıtlı.");
          }
          const invalidCats = reqCats.filter((c: string) => !allowedCats!.includes(c));
          if (invalidCats.length > 0) {
            throw new HttpsError("permission-denied", `Kapsam ihlali: Yetkiniz olmayan işler: ${invalidCats.join(", ")}`);
          }
        }
      }
    }
  }

  // Güncellemeyi yap
  const updatedMember = {
    ...targetMember,
    permissions: newPermissions,
    delegablePermissions: newDelegablePermissions,
    scopes: newScopes || targetMember.scopes || {isRestricted: false, nodeCategories: {}},
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

  const requesterIsOwner = requesterMember.isOwner === true || requesterMember.owner === true;
  const targetIsOwner = targetMember.isOwner === true || targetMember.owner === true;

  // Owner koruması
  if (targetIsOwner) {
    throw new HttpsError(
      "permission-denied",
      "Proje sahibi projeden çıkarılamaz."
    );
  }

  const requesterPerms: string[] = requesterMember.permissions || [];

  // MANAGE_MEMBERS yetkisi kontrolü
  if (!requesterIsOwner && !requesterPerms.includes("MANAGE_MEMBERS")) {
    throw new HttpsError(
      "permission-denied",
      "Üye yönetimi yetkiniz bulunmuyor."
    );
  }

  // Subset kontrolü — Hedefin tüm yetkileri requester'da olmalı
  if (!requesterIsOwner) {
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
