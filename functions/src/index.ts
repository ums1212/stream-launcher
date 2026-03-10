import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { getStorage } from "firebase-admin/storage";
import { onDocumentDeleted } from "firebase-functions/v2/firestore";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { logger } from "firebase-functions/v2";

initializeApp();

const PRESET_IMAGES_PREFIX = "preset_images/";
// 업로드 도중 취소된 고아 파일의 유예 시간 (1시간)
const ORPHAN_AGE_MS = 60 * 60 * 1000;

/**
 * Firestore presets 문서가 삭제될 때 연관된 Storage 파일 전체 삭제.
 * 사용자가 앱에서 프리셋을 삭제하거나 관리자가 문서를 지울 때 트리거.
 */
export const onPresetDeleted = onDocumentDeleted(
  "presets/{presetId}",
  async (event) => {
    const presetId = event.params.presetId;
    const data = event.data?.data();
    const authorUid = data?.authorUid as string | undefined;

    if (!authorUid) {
      logger.warn(`presetId=${presetId}: authorUid 없음, Storage 정리 건너뜀`);
      return;
    }

    const folderPath = `${PRESET_IMAGES_PREFIX}${authorUid}/${presetId}/`;
    await deleteStorageFolder(folderPath);
    logger.info(`presetId=${presetId}: Storage 폴더 삭제 완료 (${folderPath})`);
  }
);

/**
 * 매일 오전 3시(KST) 실행. Firestore 문서가 없는 고아 Storage 파일 정리.
 * 업로드 도중 취소되어 Firestore에 등록되지 못한 이미지를 제거.
 * ORPHAN_AGE_MS(1시간) 이상 된 파일만 삭제하여 진행 중인 업로드 보호.
 */
export const cleanupOrphanedPresetImages = onSchedule(
  { schedule: "0 18 * * *", timeZone: "UTC" }, // UTC 18:00 = KST 03:00
  async () => {
    const bucket = getStorage().bucket();
    const db = getFirestore();
    const now = Date.now();

    // preset_images/ 하위의 모든 uid 폴더 나열
    const [, , apiResponse] = await bucket.getFiles({
      prefix: PRESET_IMAGES_PREFIX,
      delimiter: "/",
      autoPaginate: false,
    });

    const uidPrefixes: string[] =
      (apiResponse as { prefixes?: string[] }).prefixes ?? [];

    for (const uidPrefix of uidPrefixes) {
      // preset_images/{uid}/ 하위의 presetId 폴더 나열
      const [, , innerResponse] = await bucket.getFiles({
        prefix: uidPrefix,
        delimiter: "/",
        autoPaginate: false,
      });

      const presetPrefixes: string[] =
        (innerResponse as { prefixes?: string[] }).prefixes ?? [];

      for (const presetPrefix of presetPrefixes) {
        // presetId 추출: "preset_images/{uid}/{presetId}/"
        const parts = presetPrefix.split("/");
        const presetId = parts[parts.length - 2];
        if (!presetId) continue;

        // Firestore에 해당 문서가 있는지 확인
        const doc = await db.collection("presets").doc(presetId).get();
        if (doc.exists) continue;

        // 문서가 없는 경우: 폴더 내 파일 중 오래된 것만 삭제
        const [files] = await bucket.getFiles({ prefix: presetPrefix });
        const oldFiles = files.filter((f) => {
          const updated = f.metadata.updated
            ? new Date(f.metadata.updated).getTime()
            : 0;
          return now - updated > ORPHAN_AGE_MS;
        });

        if (oldFiles.length === 0) continue;

        await Promise.all(oldFiles.map((f) => f.delete()));
        logger.info(
          `고아 파일 ${oldFiles.length}개 삭제: ${presetPrefix}`
        );
      }
    }

    logger.info("고아 파일 정리 완료");
  }
);

async function deleteStorageFolder(folderPath: string): Promise<void> {
  const bucket = getStorage().bucket();
  const [files] = await bucket.getFiles({ prefix: folderPath });
  if (files.length === 0) return;
  await Promise.all(files.map((f) => f.delete()));
}
