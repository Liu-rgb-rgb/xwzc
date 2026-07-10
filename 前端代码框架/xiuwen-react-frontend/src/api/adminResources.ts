import { get, post, put, del } from '@/utils/request';
import type { PageParams } from '@/types/api';

export const getAdminResourcesApi = (params?: PageParams & { status?: string; resourceType?: string }) => get('/api/admin/resources', params);
export const createAdminResourceApi = (data: any) => post('/api/admin/resources', data);
export const getAdminResourceDetailApi = (resourceId: number | string) => get(`/api/admin/resources/${resourceId}`);
export const updateAdminResourceApi = (resourceId: number | string, data: any) => put(`/api/admin/resources/${resourceId}`, data);
export const updateAdminResourceStatusApi = (resourceId: number | string, data: { status: string }) => put(`/api/admin/resources/${resourceId}/status`, data);
export const deleteAdminResourceApi = (resourceId: number | string) => del(`/api/admin/resources/${resourceId}`);
