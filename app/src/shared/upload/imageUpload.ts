import * as ImagePicker from 'expo-image-picker';
import * as FileSystem from 'expo-file-system/legacy';
import { API_BASE_URL } from '@/shared/config/api';
import { getAccessToken } from '@/shared/auth/session';
import { refreshSession, notifySessionExpired } from '@/shared/api/client';

/** 남은 선택 가능 장수(0 미만이면 0). */
export function remainingSlots(current: number, max: number): number {
  return Math.max(0, max - current);
}

/** files 업로드 응답 엔벨로프에서 접근 url을 꺼낸다(없으면 throw). */
export function extractUploadedUrl(envelope: { success: boolean; data?: { url?: string } }): string {
  const url = envelope?.data?.url;
  if (!url) throw new Error('업로드 응답에 url이 없습니다.');
  return url;
}

/** 선택된 사진 한 장. assetId는 같은 사진 중복 추가를 막는 데 쓴다(기기에 따라 null일 수 있음). */
export type PickedAsset = { uri: string; assetId: string | null };

/** 갤러리에서 최대 remaining장 선택. 권한 거부/취소 시 빈 배열. 반환은 (uri, assetId) 목록. */
export async function pickImages(remaining: number): Promise<PickedAsset[]> {
  if (remaining <= 0) return [];
  const perm = await ImagePicker.requestMediaLibraryPermissionsAsync();
  if (!perm.granted) return [];
  const result = await ImagePicker.launchImageLibraryAsync({
    mediaTypes: ['images'],
    allowsMultipleSelection: true,
    selectionLimit: remaining,
    quality: 0.7,
  });
  if (result.canceled) return [];
  return result.assets.map((a) => ({ uri: a.uri, assetId: a.assetId ?? null }));
}

/**
 * 로컬 uri들을 POST /api/files(multipart)로 올려 접근 url 목록을 반환(순서 보존).
 *
 * RN 0.85/Expo SDK 56의 fetch는 레거시 FormData 파일 파트({uri,name,type})를 지원하지 않으므로
 * (ERR:unsupported FormData part implementation), expo-file-system의 uploadAsync(MULTIPART)로 올린다.
 * fieldName='file'은 백엔드 @RequestParam("file")와 일치한다.
 */
export async function uploadImages(uris: string[], token?: string): Promise<string[]> {
  const urls: string[] = [];
  for (const uri of uris) {
    urls.push(await uploadOne(uri, token));
  }
  return urls;
}

/**
 * 한 장 업로드. 세션 토큰 요청(token 미지정)이 401이면 refresh 후 1회 재시도하고,
 * refresh까지 실패하면 세션 만료 처리(조용한 로그아웃) 후 에러를 던진다 —
 * client.ts request()의 401 정책을 request() 밖 경로에도 동일 적용.
 * 온보딩 토큰(token 명시)은 refresh 대상이 아니다(아직 로그인 전).
 */
async function uploadOne(uri: string, token: string | undefined, retried = false): Promise<string> {
  const usingSession = token === undefined; // 세션 access 사용 여부
  const authToken = token ?? getAccessToken();
  const res = await FileSystem.uploadAsync(`${API_BASE_URL}/api/files`, uri, {
    httpMethod: 'POST',
    uploadType: FileSystem.FileSystemUploadType.MULTIPART,
    fieldName: 'file',
    mimeType: 'image/jpeg',
    headers: authToken ? { Authorization: `Bearer ${authToken}` } : {},
  }).catch(() => {
    throw new Error('사진 업로드에 실패했어요. 잠시 후 다시 시도해주세요.');
  });

  // 세션 토큰 업로드가 401이면 refresh 후 1회 재시도. refresh 실패면 세션 만료(조용한 로그아웃).
  if (res.status === 401 && usingSession && !retried) {
    try {
      await refreshSession();
    } catch {
      notifySessionExpired();
      throw new Error('사진 업로드에 실패했어요. 잠시 후 다시 시도해주세요.');
    }
    return uploadOne(uri, token, true);
  }

  if (res.status < 200 || res.status >= 300) {
    // 서버가 사람이 읽을 수 있는 이유를 주면(용량 초과 등) 그대로 보여준다 — "실패했어요"만 띄우면
    // 사용자는 다시 시도해야 할지 다른 사진을 골라야 할지 알 수 없다.
    let serverMessage: string | undefined;
    try {
      serverMessage = JSON.parse(res.body)?.error?.message;
    } catch {
      /* 본문이 JSON이 아님(프록시 에러 페이지 등) — 기본 메시지로 떨어진다 */
    }
    throw new Error(serverMessage || '사진 업로드에 실패했어요. 잠시 후 다시 시도해주세요.');
  }
  let envelope: { success: boolean; data?: { url?: string } };
  try {
    envelope = JSON.parse(res.body);
  } catch {
    throw new Error('사진 업로드 응답을 해석하지 못했어요.');
  }
  if (!envelope?.success) throw new Error('사진 업로드에 실패했어요.');
  return extractUploadedUrl(envelope);
}
