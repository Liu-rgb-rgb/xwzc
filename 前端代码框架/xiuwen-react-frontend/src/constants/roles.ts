import type { UserRole } from '@/types/api';

export const ROLE = {
  USER: 'USER',
  MERCHANT_ADMIN: 'MERCHANT_ADMIN',
  ADMIN: 'ADMIN'
} as const;

export const isAdminRole = (role?: UserRole) => role === ROLE.MERCHANT_ADMIN || role === ROLE.ADMIN;
