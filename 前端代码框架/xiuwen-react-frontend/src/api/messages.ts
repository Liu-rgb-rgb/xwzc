import { get, put, del } from '@/utils/request';
import type { PageParams } from '@/types/api';

export const getMessagesApi = (params?: PageParams) => get('/api/messages', params);
export const getUnreadMessageCountApi = () => get('/api/messages/unread-count');
export const readMessageApi = (messageId: number | string) => put(`/api/messages/${messageId}/read`);
export const readAllMessagesApi = () => put('/api/messages/read-all');
export const deleteMessageApi = (messageId: number | string) => del(`/api/messages/${messageId}`);
