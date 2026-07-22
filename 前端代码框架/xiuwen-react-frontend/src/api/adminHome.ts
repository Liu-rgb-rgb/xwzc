import { get, post, put, del } from '@/utils/request';
import type { PageParams } from '@/types/api';

export const getAdminHomeBannersApi = (params?: PageParams) => get('/api/admin/home-banners', params);
export const createAdminHomeBannerApi = (data: any) => post('/api/admin/home-banners', data);
export const updateAdminHomeBannerApi = (bannerId: number | string, data: any) => put(`/api/admin/home-banners/${bannerId}`, data);
export const deleteAdminHomeBannerApi = (bannerId: number | string) => del(`/api/admin/home-banners/${bannerId}`);

export const getAdminHomeRecommendsApi = (params?: PageParams) => get('/api/admin/home-recommends', params);
export const createAdminHomeRecommendApi = (data: any) => post('/api/admin/home-recommends', data);
export const updateAdminHomeRecommendApi = (recommendId: number | string, data: any) => put(`/api/admin/home-recommends/${recommendId}`, data);
export const deleteAdminHomeRecommendApi = (recommendId: number | string) => del(`/api/admin/home-recommends/${recommendId}`);
