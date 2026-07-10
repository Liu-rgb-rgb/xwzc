import { get } from '@/utils/request';
import type { PageParams } from '@/types/api';

export const getResourcesApi = (params?: PageParams & { resourceType?: string }) => get('/api/resources', params);
export const getResourceDetailApi = (resourceId: number | string) => get(`/api/resources/${resourceId}`);
export const downloadResourceApi = (resourceId: number | string) => get(`/api/resources/${resourceId}/download`);
