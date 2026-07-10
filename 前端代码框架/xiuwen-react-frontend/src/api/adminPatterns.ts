import { get, post, put, del } from '@/utils/request';
import type { PageParams } from '@/types/api';

export const getAdminPatternsApi = (params?: PageParams & { status?: string; style?: string }) => get('/api/admin/patterns', params);
export const getAdminPatternDetailApi = (patternId: number | string) => get(`/api/admin/patterns/${patternId}`);
export const recommendAdminPatternApi = (patternId: number | string, data?: any) => put(`/api/admin/patterns/${patternId}/recommend`, data || {});
export const updateAdminPatternStatusApi = (patternId: number | string, data: { status: string }) => put(`/api/admin/patterns/${patternId}/status`, data);
export const deleteAdminPatternApi = (patternId: number | string) => del(`/api/admin/patterns/${patternId}`);
export const getAdminPatternGenerationsApi = (params?: PageParams) => get('/api/admin/pattern-generations', params);

export const getAdminPromptTemplatesApi = (params?: PageParams) => get('/api/admin/prompt-templates', params);
export const createAdminPromptTemplateApi = (data: any) => post('/api/admin/prompt-templates', data);
export const updateAdminPromptTemplateApi = (templateId: number | string, data: any) => put(`/api/admin/prompt-templates/${templateId}`, data);
export const deleteAdminPromptTemplateApi = (templateId: number | string) => del(`/api/admin/prompt-templates/${templateId}`);
