import * as ImagePicker from 'expo-image-picker';
import * as FileSystem from 'expo-file-system/legacy';
import { API_BASE_URL } from '@/shared/config/api';
import { getAccessToken } from '@/shared/auth/session';

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

/** 갤러리에서 최대 remaining장 선택. 권한 거부/취소 시 빈 배열. 반환은 로컬 uri 목록. */
export async function pickImages(remaining: number): Promise<string[]> {
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
  return result.assets.map((a) => a.uri);
}

/**
 * 로컬 uri들을 POST /api/files(multipart)로 올려 접근 url 목록을 반환(순서 보존).
 *
 * RN 0.85/Expo SDK 56의 fetch는 레거시 FormData 파일 파트({uri,name,type})를 지원하지 않으므로
 * (ERR:unsupported FormData part implementation), expo-file-system의 uploadAsync(MULTIPART)로 올린다.
 * fieldName='file'은 백엔드 @RequestParam("file")와 일치한다.
 */
export async function uploadImages(uris: string[]): Promise<string[]> {
  const token = getAccessToken();
  const urls: string[] = [];
  for (const uri of uris) {
    const res = await FileSystem.uploadAsync(`${API_BASE_URL}/api/files`, uri, {
      httpMethod: 'POST',
      uploadType: FileSystem.FileSystemUploadType.MULTIPART,
      fieldName: 'file',
      mimeType: 'image/jpeg',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    }).catch(() => {
      throw new Error('사진 업로드에 실패했어요. 잠시 후 다시 시도해주세요.');
    });
    if (res.status < 200 || res.status >= 300) {
      throw new Error('사진 업로드에 실패했어요. 잠시 후 다시 시도해주세요.');
    }
    let envelope: { success: boolean; data?: { url?: string } };
    try {
      envelope = JSON.parse(res.body);
    } catch {
      throw new Error('사진 업로드 응답을 해석하지 못했어요.');
    }
    if (!envelope?.success) throw new Error('사진 업로드에 실패했어요.');
    urls.push(extractUploadedUrl(envelope));
  }
  return urls;
}
