import { get, put } from '@/utils/request';
import type { PageParams } from '@/types/api';

export const getAdminOrdersApi = (params?: PageParams & { status?: string; startTime?: string; endTime?: string }) => get('/api/admin/orders', params);
export const getAdminOrderDetailApi = (orderId: number | string) => get(`/api/admin/orders/${orderId}`);
export const updateAdminOrderStatusApi = (orderId: number | string, data: { status: string; remark?: string }) => put(`/api/admin/orders/${orderId}/status`, data);
export const updateAdminOrderRemarkApi = (orderId: number | string, data: { remark: string }) => put(`/api/admin/orders/${orderId}/remark`, data);
