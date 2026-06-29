import type { CreateGroupBody } from './api';

// 폼 입력을 그룹 생성/수정 바디로. 이름은 trim, 설명은 비어있으면 제외.
export function buildGroupBody(input: { name: string; note: string; color: string }): CreateGroupBody {
  const note = input.note.trim();
  const body: CreateGroupBody = { name: input.name.trim(), color: input.color };
  if (note.length > 0) body.note = note;
  return body;
}
