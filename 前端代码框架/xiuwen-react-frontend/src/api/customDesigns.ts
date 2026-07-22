import { get, post, del } from '@/utils/request';
import type { PageParams } from '@/types/api';

export const createCustomDesignApi = (data: any) => post('/api/custom-designs', data);
export const getMyCustomDesignsApi = (params?: PageParams) => get('/api/custom-designs/my', params);
export const getCustomDesignDetailApi = (customDesignId: number | string) => get(`/api/custom-designs/${customDesignId}`);
export const deleteCustomDesignApi = (customDesignId: number | string) => del(`/api/custom-designs/${customDesignId}`);
