import { get, put } from '@/utils/request';
import type { PageParams } from '@/types/api';

export const getAdminUsersApi = (params?: PageParams & { status?: string; role?: string }) => get('/api/admin/users', params);
export const getAdminUserDetailApi = (userId: number | string) => get(`/api/admin/users/${userId}`);
export const updateAdminUserStatusApi = (userId: number | string, data: { status: number }) => put(`/api/admin/users/${userId}/status`, data);
