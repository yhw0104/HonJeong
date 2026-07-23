import { apiGet } from '@/shared/api/client';
import type { BadgeStatus } from './badges';

/** 내 뱃지 현황(GET /users/me/badges) — 10종 전부(획득 플래그+시각). */
export const fetchBadges = () => apiGet<BadgeStatus[]>('/users/me/badges');
