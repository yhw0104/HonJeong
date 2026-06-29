import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  addPlaceToGroup,
  createFavoriteGroup,
  deleteFavoriteGroup,
  fetchFavoriteGroupDetail,
  fetchFavoriteGroups,
  fetchFavoriteStatus,
  removePlaceFromGroup,
  updateFavoriteGroup,
  type CreateGroupBody,
  type UpdateGroupBody,
} from './api';

export function useFavoriteGroups() {
  return useQuery({ queryKey: ['favorite-groups'], queryFn: fetchFavoriteGroups });
}

export function useFavoriteGroupDetail(groupId: number | null) {
  return useQuery({
    queryKey: ['favorite-group', groupId],
    queryFn: () => fetchFavoriteGroupDetail(groupId as number),
    enabled: groupId != null,
  });
}

export function useFavoriteStatus(placeId: number) {
  return useQuery({
    queryKey: ['favorite-status', placeId],
    queryFn: () => fetchFavoriteStatus(placeId),
  });
}

export function useCreateFavoriteGroup() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateGroupBody) => createFavoriteGroup(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['favorite-groups'] }),
  });
}

export function useUpdateFavoriteGroup() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (vars: { groupId: number; body: UpdateGroupBody }) =>
      updateFavoriteGroup(vars.groupId, vars.body),
    onSuccess: (_res, vars) => {
      qc.invalidateQueries({ queryKey: ['favorite-groups'] });
      qc.invalidateQueries({ queryKey: ['favorite-group', vars.groupId] });
    },
  });
}

export function useDeleteFavoriteGroup() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (groupId: number) => deleteFavoriteGroup(groupId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['favorite-groups'] }),
  });
}

export function useAddPlaceToGroup(placeId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (groupId: number) => addPlaceToGroup(groupId, placeId),
    onSuccess: (_res, groupId) => {
      qc.invalidateQueries({ queryKey: ['favorite-status', placeId] });
      qc.invalidateQueries({ queryKey: ['favorite-groups'] });
      qc.invalidateQueries({ queryKey: ['favorite-group', groupId] });
    },
  });
}

export function useRemovePlaceFromGroup(placeId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (groupId: number) => removePlaceFromGroup(groupId, placeId),
    onSuccess: (_res, groupId) => {
      qc.invalidateQueries({ queryKey: ['favorite-status', placeId] });
      qc.invalidateQueries({ queryKey: ['favorite-groups'] });
      qc.invalidateQueries({ queryKey: ['favorite-group', groupId] });
    },
  });
}
