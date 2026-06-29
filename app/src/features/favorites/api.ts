import { apiGet, apiPost, apiPatch, apiDelete } from '@/shared/api/client';

export type FavoriteGroupSummary = {
  groupId: number;
  name: string;
  note: string | null;
  color: string;
  isDefault: boolean;
  placeCount: number;
};

export type FavoritePlace = {
  placeId: number;
  name: string;
  category: string | null;
  address: string | null;
  roadAddress: string | null;
  latitude: number;
  longitude: number;
  visited: boolean;
};

export type FavoriteGroupDetail = {
  groupId: number;
  name: string;
  note: string | null;
  color: string;
  isDefault: boolean;
  places: FavoritePlace[];
};

export type FavoriteStatus = {
  saved: boolean;
  groups: { groupId: number; name: string; color: string; contains: boolean }[];
};

export type CreateGroupBody = { name: string; note?: string; color?: string };
export type UpdateGroupBody = { name?: string; note?: string; color?: string };

export const fetchFavoriteGroups = () => apiGet<FavoriteGroupSummary[]>('/favorite-groups');

export const fetchFavoriteGroupDetail = (groupId: number) =>
  apiGet<FavoriteGroupDetail>(`/favorite-groups/${groupId}`);

export const createFavoriteGroup = (body: CreateGroupBody) =>
  apiPost<FavoriteGroupSummary>('/favorite-groups', body);

export const updateFavoriteGroup = (groupId: number, body: UpdateGroupBody) =>
  apiPatch<FavoriteGroupSummary>(`/favorite-groups/${groupId}`, body);

export const deleteFavoriteGroup = (groupId: number) =>
  apiDelete<null>(`/favorite-groups/${groupId}`);

export const addPlaceToGroup = (groupId: number, placeId: number) =>
  apiPost<null>(`/favorite-groups/${groupId}/places`, { placeId });

export const removePlaceFromGroup = (groupId: number, placeId: number) =>
  apiDelete<null>(`/favorite-groups/${groupId}/places/${placeId}`);

export const fetchFavoriteStatus = (placeId: number) =>
  apiGet<FavoriteStatus>(`/places/${placeId}/favorite-status`);
