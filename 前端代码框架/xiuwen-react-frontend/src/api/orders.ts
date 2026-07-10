import { get, post, put } from '@/utils/request';
import type { PageParams } from '@/types/api';

export const createOrderApi = (data: any) => post('/api/orders', data);
export const mockPayOrderApi = (orderId: number | string) => post(`/api/orders/${orderId}/mock-pay`);
export const getMyOrdersApi = (params?: PageParams & { status?: string }) => get('/api/orders/my', params);
export const getOrderStatusCountApi = () => get('/api/orders/status-count');
export const getOrderDetailApi = (orderId: number | string) => get(`/api/orders/${orderId}`);
export const cancelOrderApi = (orderId: number | string) => put(`/api/orders/${orderId}/cancel`);
export const confirmOrderApi = (orderId: number | string) => put(`/api/orders/${orderId}/confirm`);
