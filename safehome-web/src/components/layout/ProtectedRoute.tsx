import { Navigate, Outlet } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import PermissionGuard from '@/components/PermissionGuard'

export default function ProtectedRoute() {
  const isLoggedIn = useAuthStore((s) => s.isLoggedIn)

  if (!isLoggedIn) return <Navigate to="/login" replace />

  return (
    <PermissionGuard>
      <Outlet />
    </PermissionGuard>
  )
}