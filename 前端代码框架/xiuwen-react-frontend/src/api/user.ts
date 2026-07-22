import { get, post, put, del } from '@/utils/request';

export const getUserProfileApi = () => get('/api/user/profile');
export const updateUserProfileApi = (data: any) => put('/api/user/profile', data);
export const uploadAvatarApi = (formData: FormData) => post('/api/user/avatar', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
export const updatePasswordApi = (data: any) => put('/api/user/password', data);

export const getAddressListApi = () => get('/api/user/addresses');
export const createAddressApi = (data: any) => post('/api/user/addresses', data);
export const updateAddressApi = (addressId: number | string, data: any) => put(`/api/user/addresses/${addressId}`, data);
export const deleteAddressApi = (addressId: number | string) => del(`/api/user/addresses/${addressId}`);
export const setDefaultAddressApi = (addressId: number | string) => put(`/api/user/addresses/${addressId}/default`);
