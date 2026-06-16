import {getFirestore} from "firebase-admin/firestore";
import {onDocumentCreated, onDocumentUpdated} from "firebase-functions/v2/firestore";
import * as logger from "firebase-functions/logger";

const db = getFirestore("default");

/**
 * Davet oluşturulduğunda güvenlik kontrolü yapar.
 *
 * Kontroller:
 * 1. Davet eden kişinin bu projede INVITE yetkisi var mı?
 * 2. Davet edenin vermek istediği yetkiler, kendi yetkilerinin alt kümesi mi? (Subset Math)
 * 3. İhlal varsa status = CANCELLED yapılır.
 *
 * Şantiye Senaryosu:
 * Taşeron Veli kendisinde olmayan "VIEW_FINANCIALS" yetkisini kalfası Hasan'a vermeye kalkarsa,
 * bu fonksiyon davetiyeyi anında iptal eder. Veli ekranında "İptal Edildi" yazar.
 */
export const onInvitationCreated = onDocumentCreated({
  document: "invitations/{invitationId}",
}, async (event) => {
  const snapshot = event.data;
  if (!snapshot) return;

  const invitationData = snapshot.data();
  const invitationId = event.params.invitationId;

  const {projectId, inviterId, grantedPermissions} = invitationData;

  if (!projectId || !inviterId) {
    logger.error("Davet verisi eksik", invitationId);
    await snapshot.ref.update({
      status: "CANCELLED",
      statusMessage: "Geçersiz davet verisi.",
      updatedAt: Date.now(),
    });
    return;
  }

  try {
    // 1. Projeyi oku ve davet edenin yetkilerini al
    const projectSnap = await db.collection("projects").doc(projectId).get();
    if (!projectSnap.exists) {
      await snapshot.ref.update({
        status: "CANCELLED",
        statusMessage: "Proje bulunamadı.",
        updatedAt: Date.now(),
      });
      return;
    }

    const projectData = projectSnap.data();
    const members = projectData?.members || {};
    const inviterMember = members[inviterId];

    if (!inviterMember) {
      await snapshot.ref.update({
        status: "CANCELLED",
        statusMessage: "Davet eden kişi bu projenin üyesi değil.",
        updatedAt: Date.now(),
      });
      return;
    }

    const inviterPermissions: string[] = inviterMember.permissions || [];

    // 2. INVITE yetkisi var mı?
    if (!inviterPermissions.includes("INVITE")) {
      await snapshot.ref.update({
        status: "CANCELLED",
        statusMessage: "Davet gönderme yetkiniz bulunmuyor.",
        updatedAt: Date.now(),
      });
      logger.warn(`Yetkisiz davet girişimi: ${inviterId} -> ${invitationId}`);
      return;
    }

    // 3. Subset kontrolü — Verilen yetkiler, davet edenin yetkilerinin alt kümesi mi?
    const requestedPerms: string[] = grantedPermissions || [];
    const invalidPerms = requestedPerms.filter(
      (p: string) => !inviterPermissions.includes(p)
    );

    if (invalidPerms.length > 0) {
      await snapshot.ref.update({
        status: "CANCELLED",
        statusMessage: `Sahip olmadığınız yetkiler: ${invalidPerms.join(", ")}`,
        updatedAt: Date.now(),
      });
      logger.warn(
        `Subset ihlali: ${inviterId} sahip olmadığı yetkileri vermeye çalıştı: ${invalidPerms}`
      );
      return;
    }

    // 4. Her şey güvenli, davetiye PENDING olarak kalır
    logger.info(`Davet onaylandı: ${invitationId} (${inviterId} -> invitee)`);
  } catch (error) {
    logger.error("Davet kontrolünde hata", error);
    await snapshot.ref.update({
      status: "CANCELLED",
      statusMessage: "Sistem hatası oluştu.",
      updatedAt: Date.now(),
    });
  }
});

/**
 * Davet durumu güncellendiğinde:
 * - ACCEPTED → Kullanıcıyı projenin members Map'ine ekle
 * - Diğer durumlar → Sadece logla
 *
 * Şantiye Senaryosu:
 * Kalfa Hasan daveti kabul ettiği an, bu fonksiyon onu
 * projects/{projectId} dökümanındaki members Map'ine kaydeder.
 * Hasan uygulamayı açtığında artık o projeyi görür.
 */
export const onInvitationUpdated = onDocumentUpdated({
  document: "invitations/{invitationId}",
}, async (event) => {
  const beforeData = event.data?.before.data();
  const afterData = event.data?.after.data();

  if (!beforeData || !afterData) return;

  // Sadece status değişikliklerini izle
  if (beforeData.status === afterData.status) return;

  const invitationId = event.params.invitationId;
  const newStatus = afterData.status;

  if (newStatus === "ACCEPTED") {
    const {
      projectId,
      inviteeId,
      inviteeName,
      grantedPermissions,
      grantedScopes,
      grantedRoleName,
    } = afterData;

    try {
      // Üyeyi projenin members Map'ine ekle
      const memberData = {
        uid: inviteeId,
        displayName: inviteeName || "",
        roleName: grantedRoleName || "Çalışan",
        permissions: grantedPermissions || [],
        scopes: grantedScopes || {nodes: [], categories: []},
        isOwner: false,
      };

      await db.collection("projects").doc(projectId).update({
        [`members.${inviteeId}`]: memberData,
      });

      // Kullanıcının users dökümanındaki projectPermissions'ı da güncelle
      await db.collection("users").doc(inviteeId).update({
        [`projectPermissions.${projectId}`]: {
          role: grantedRoleName || "Çalışan",
          canDelegate: (grantedPermissions || []).includes("INVITE"),
        },
      });

      logger.info(
        `Üye eklendi: ${inviteeId} -> proje ${projectId} (Davet: ${invitationId})`
      );
    } catch (error) {
      logger.error("Üye ekleme hatası", error);
    }
  } else {
    logger.info(`Davet ${invitationId} durumu: ${newStatus}`);
  }
});
