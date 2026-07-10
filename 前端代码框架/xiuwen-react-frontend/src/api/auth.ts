import { get, post } from '@/utils/request';
import type { LoginResponse, UserInfo } from '@/types/api';

export interface LoginParams { username: string; password: string; }
export interface RegisterParams { username: string; password: string; nickname?: string; phone?: string; email?: string; }

export const registerApi = (data: RegisterParams) => post('/api/auth/register', data);
export const loginApi = (data: LoginParams) => post<LoginResponse>('/api/auth/login', data);
export const logoutApi = () => post('/api/auth/logout');
export const getMeApi = () => get<UserInfo>('/api/auth/me');
