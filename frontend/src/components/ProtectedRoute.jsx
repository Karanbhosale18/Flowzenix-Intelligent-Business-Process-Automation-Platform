import { Navigate } from 'react-router-dom'
import AuthService from '../services/AuthService'

export default function ProtectedRoute({ children, requiredRole }) {
  if (!AuthService.isAuthenticated()) {
    return <Navigate to="/login" replace />
  }
  if (requiredRole) {
    const user = AuthService.getCurrentUser()
    const roles = user?.roles || []
    if (!roles.includes(requiredRole)) {
      return <Navigate to="/dashboard" replace />
    }
  }
  return children
}
