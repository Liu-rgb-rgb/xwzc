import type { UserInfo } from '@/types/api';

const TOKEN_KEY = 'xiuwen_token';
const USER_KEY = 'xiuwen_user_info';

export function getToken() { return localStorage.getItem(TOKEN_KEY) || ''; }
export function setToken(token: string) { localStorage.setItem(TOKEN_KEY, token); }
export function removeToken() { localStorage.removeItem(TOKEN_KEY); }

export function getUserInfo(): UserInfo | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try { return JSON.parse(raw); } catch { return null; }
}

export function setUserInfo(user: UserInfo) { localStorage.setItem(USER_KEY, JSON.stringify(user)); }
export function removeUserInfo() { localStorage.removeItem(USER_KEY); }

export function clearAuthStorage() {
  removeToken();
  removeUserInfo();
}
