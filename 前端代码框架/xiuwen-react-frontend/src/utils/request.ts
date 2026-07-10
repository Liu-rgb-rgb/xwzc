import axios, { AxiosError, AxiosRequestConfig } from 'axios';
import { message } from 'antd';
import { getToken, clearAuthStorage } from './storage';
import type { ApiResponse } from '@/types/api';

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 20000
});

request.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse;
    if (res && typeof res.code === 'number') {
      if (res.code === 200) return res.data;
      if (res.code === 401) {
        clearAuthStorage();
        message.error(res.message || '登录已失效，请重新登录');
        window.location.href = '/login';
        return Promise.reject(res);
      }
      message.error(res.message || '请求失败');
      return Promise.reject(res);
    }
    return response.data;
  },
  (error: AxiosError<any>) => {
    const status = error.response?.status;
    if (status === 401) {
      clearAuthStorage();
      message.error('登录已失效，请重新登录');
      window.location.href = '/login';
    } else if (status === 403) {
      message.error('无权限访问');
    } else {
      message.error(error.message || '网络异常');
    }
    return Promise.reject(error);
  }
);

export function get<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
  return request.get(url, { params, ...config });
}

export function post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return request.post(url, data, config);
}

export function put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return request.put(url, data, config);
}

export function del<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
  return request.delete(url, { params, ...config });
}

export default request;
