import { get, post, put, del } from '@/utils/request';

export const getCartItemsApi = () => get('/api/cart/items');
export const addCartItemApi = (data: any) => post('/api/cart/items', data);
export const updateCartItemApi = (cartItemId: number | string, data: any) => put(`/api/cart/items/${cartItemId}`, data);
export const deleteCartItemApi = (cartItemId: number | string) => del(`/api/cart/items/${cartItemId}`);
export const clearCartItemsApi = () => del('/api/cart/items');
