import { get, put, post } from '@/utils/request';

export const getAdminShopInfoApi = () => get('/api/admin/shop/info');
export const updateAdminShopInfoApi = (data: any) => put('/api/admin/shop/info', data);
export const publishAdminMessageApi = (data: any) => post('/api/admin/messages', data);
export const uploadAdminFileApi = (formData: FormData) => post('/api/admin/files/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
