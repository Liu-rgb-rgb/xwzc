import { get } from '@/utils/request';
import type { PageParams } from '@/types/api';

export const getProductCategoriesApi = () => get('/api/products/categories');
export const getProductsApi = (params?: PageParams & { categoryId?: number | string; sort?: string; minPrice?: number; maxPrice?: number }) => get('/api/products', params);
export const getProductDetailApi = (productId: number | string) => get(`/api/products/${productId}`);
export const getProductRecommendsApi = (params?: any) => get('/api/products/recommends', params);
