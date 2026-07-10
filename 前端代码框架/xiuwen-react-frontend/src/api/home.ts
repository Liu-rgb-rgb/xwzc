import { get } from '@/utils/request';

export const getHomeApi = () => get('/api/home');
export const getHomeBannersApi = () => get('/api/home/banners');
export const getHomeRecommendsApi = (params?: any) => get('/api/home/recommends', params);
export const searchApi = (params: { keyword: string; type?: string; page?: number; pageSize?: number }) => get('/api/search', params);
export const getShopInfoApi = () => get('/api/shop/info');
