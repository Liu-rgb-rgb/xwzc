import { computed, reactive } from 'vue';

type UserInfo = { id?: number; username?: string; nickname?: string; role?: string };

const savedUser = localStorage.getItem('xiuwen_user');
export const authState = reactive<{ token: string; user: UserInfo | null }>({
  token: localStorage.getItem('xiuwen_token') || '',
  user: savedUser ? JSON.parse(savedUser) : null
});

export const isLoggedIn = computed(() => Boolean(authState.token));
export const isAdmin = computed(() =>
  ['ADMIN', 'MERCHANT_ADMIN'].includes(authState.user?.role || '')
);

export function setLogin(token: string, user: UserInfo) {
  authState.token = token;
  authState.user = user;
  localStorage.setItem('xiuwen_token', token);
  localStorage.setItem('xiuwen_user', JSON.stringify(user));
}

export function logout() {
  authState.token = '';
  authState.user = null;
  localStorage.removeItem('xiuwen_token');
  localStorage.removeItem('xiuwen_user');
}
