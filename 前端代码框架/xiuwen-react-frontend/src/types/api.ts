export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
}

export interface PageResult<T = any> {
  total: number;
  page?: number;
  pageSize?: number;
  list: T[];
}

export interface PageParams {
  page?: number;
  pageSize?: number;
  keyword?: string;
  [key: string]: any;
}

export type UserRole = 'USER' | 'MERCHANT_ADMIN' | 'ADMIN';

export interface UserInfo {
  id: number;
  username: string;
  nickname?: string;
  avatar?: string | null;
  role: UserRole;
  phone?: string;
  email?: string;
  status?: number;
}

export interface LoginResponse {
  token: string;
  userInfo: UserInfo;
}
