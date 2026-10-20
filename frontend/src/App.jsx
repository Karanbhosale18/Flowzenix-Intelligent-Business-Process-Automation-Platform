import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Login from './pages/Login.jsx'
import Dashboard from './pages/Dashboard.jsx'
import NewRequest from './pages/NewRequest.jsx'
import MyRequests from './pages/MyRequests.jsx'
import RequestDetail from './pages/RequestDetail.jsx'
import Approvals from './pages/Approvals.jsx'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import Workflows from './pages/admin/Workflows.jsx'
import CreateWorkflow from './pages/admin/CreateWorkflow.jsx'
import AdminDashboard from './pages/admin/AdminDashboard.jsx'
import UserManagement from './pages/admin/UserManagement.jsx'
import Profile from './pages/Profile.jsx'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Navigate to="/login" replace />} />
        <Route path="/dashboard" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
        <Route path="/requests/new" element={<ProtectedRoute><NewRequest /></ProtectedRoute>} />
        <Route path="/requests" element={<ProtectedRoute><MyRequests /></ProtectedRoute>} />
        <Route path="/requests/:id" element={<ProtectedRoute><RequestDetail /></ProtectedRoute>} />
        <Route path="/approvals" element={<ProtectedRoute><Approvals /></ProtectedRoute>} />
        <Route path="/profile" element={<ProtectedRoute><Profile /></ProtectedRoute>} />
        <Route path="/admin/users" element={<ProtectedRoute requiredRole="ROLE_ADMIN"><UserManagement /></ProtectedRoute>} />
        <Route path="/admin/workflows" element={<ProtectedRoute requiredRole="ROLE_ADMIN"><Workflows /></ProtectedRoute>} />
        <Route path="/admin" element={<ProtectedRoute requiredRole="ROLE_ADMIN"><AdminDashboard /></ProtectedRoute>} />
        <Route path="/admin/workflows/new" element={<ProtectedRoute requiredRole="ROLE_ADMIN"><CreateWorkflow /></ProtectedRoute>} />
        <Route path="/admin/workflows/:id" element={<ProtectedRoute requiredRole="ROLE_ADMIN"><CreateWorkflow /></ProtectedRoute>} />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
