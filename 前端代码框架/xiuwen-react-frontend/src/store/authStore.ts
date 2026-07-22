import { create } from 'zustand';
import type { UserInfo } from '@/types/api';
import { getToken, getUserInfo, setToken, setUserInfo, clearAuthStorage } from '@/utils/storage';

interface AuthState {
  token: string;
  userInfo: UserInfo | null;
  setLogin: (token: string, userInfo: UserInfo) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: getToken(),
  userInfo: getUserInfo(),
  setLogin: (token, userInfo) => {
    setToken(token);
    setUserInfo(userInfo);
    set({ token, userInfo });
  },
  logout: () => {
    clearAuthStorage();
    set({ token: '', userInfo: null });
  }
}));
