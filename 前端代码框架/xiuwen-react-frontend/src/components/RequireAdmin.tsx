import { Navigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';
import { isAdminRole } from '@/constants/roles';

export default function RequireAdmin({ children }: { children: JSX.Element }) {
  const { token, userInfo } = useAuthStore();
  if (!token) return <Navigate to="/login" replace />;
  if (!isAdminRole(userInfo?.role)) return <Navigate to="/home" replace />;
  return children;
}
