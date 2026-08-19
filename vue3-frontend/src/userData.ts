import { authState } from './auth';

export const userDataEvent = 'xiuwen-user-data-change';

function owner() {
  return String(authState.user?.id || authState.user?.username || 'guest');
}

export function userDataKey(name: string) {
  return `xiuwen_${owner()}_${name}`;
}

export function readUserData<T>(name: string, fallback: T): T {
  try {
    return JSON.parse(localStorage.getItem(userDataKey(name)) || '') as T;
  } catch {
    return fallback;
  }
}

export function writeUserData<T>(name: string, value: T) {
  localStorage.setItem(userDataKey(name), JSON.stringify(value));
  window.dispatchEvent(new CustomEvent(userDataEvent, { detail: { name } }));
}
