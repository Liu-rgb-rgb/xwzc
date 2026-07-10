import { get, post, put, del } from '@/utils/request';
import type { PageParams } from '@/types/api';

export const getAdminProductCategoriesApi = (params?: PageParams) => get('/api/admin/product-categories', params);
export const createAdminProductCategoryApi = (data: any) => post('/api/admin/product-categories', data);
export const updateAdminProductCategoryApi = (categoryId: number | string, data: any) => put(`/api/admin/product-categories/${categoryId}`, data);
export const deleteAdminProductCategoryApi = (categoryId: number | string) => del(`/api/admin/product-categories/${categoryId}`);

export const getAdminProductsApi = (params?: PageParams & { status?: string; categoryId?: number | string }) => get('/api/admin/products', params);
export const createAdminProductApi = (data: any) => post('/api/admin/products', data);
export const getAdminProductDetailApi = (productId: number | string) => get(`/api/admin/products/${productId}`);
export const updateAdminProductApi = (productId: number | string, data: any) => put(`/api/admin/products/${productId}`, data);
export const updateAdminProductStatusApi = (productId: number | string, data: { status: string }) => put(`/api/admin/products/${productId}/status`, data);
export const deleteAdminProductApi = (productId: number | string) => del(`/api/admin/products/${productId}`);

export const getAdminCustomDesignsApi = (params?: PageParams) => get('/api/admin/custom-designs', params);
export const getAdminCustomDesignDetailApi = (customDesignId: number | string) => get(`/api/admin/custom-designs/${customDesignId}`);
export const downloadAdminCustomDesignApi = (customDesignId: number | string) => get(`/api/admin/custom-designs/${customDesignId}/download`);
