import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import AppShell from '../../components/AppShell.jsx'
import AuthService from '../../services/AuthService.js'
import WorkflowService from '../../services/WorkflowService.js'
import AdminUserService from '../../services/AdminUserService.js'
import '../Dashboard.css'
import './Workflows.css'

export default function AdminDashboard() {
  const [workflows, setWorkflows] = useState([])
  const [userCount, setUserCount] = useState('—')
  const [managerCount, setManagerCount] = useState('—')

  const user = AuthService.getCurrentUser()
  const isAdmin = (user?.roles || []).includes('ROLE_ADMIN')

  useEffect(() => {
    if (isAdmin) {
      WorkflowService.list(true).then(setWorkflows).catch(() => {})
      AdminUserService.getUsers().then(res => {
        setUserCount(res.data.length)
      }).catch(() => {})
      AdminUserService.getManagers().then(res => {
        setManagerCount(res.data.length)
      }).catch(() => {})
    }
  }, [isAdmin])

  if (!isAdmin) {
    return (
      <AppShell title="Admin dashboard">
        <div className="card">
          <p>You need an administrator account to view this page.</p>
        </div>
      </AppShell>
    )
  }

  const active = workflows.filter((workflow) => workflow.active).length

  return (
    <AppShell
      title="Admin Dashboard"
      actions={
        <div style={{ display: 'flex', gap: 10 }}>
          <Link to="/admin/users" className="btn">Manage Users</Link>
          <Link to="/admin/workflows/new" className="btn btn-primary">Create Workflow</Link>
        </div>
      }
    >
      <div className="stats-grid">
        <div className="card stat-card">
          <p className="stat-value">{userCount}</p>
          <p className="stat-label">Total platform users</p>
        </div>
        <div className="card stat-card">
          <p className="stat-value">{managerCount}</p>
          <p className="stat-label">Eligible managers</p>
        </div>
        <div className="card stat-card">
          <p className="stat-value">{workflows.length}</p>
          <p className="stat-label">Workflow definitions</p>
        </div>
        <div className="card stat-card stat-card--accent">
          <p className="stat-value">{active}</p>
          <p className="stat-label">Active routes</p>
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 16, marginTop: 20 }}>
        <section className="card">
          <div className="workflow-list-heading" style={{ padding: 0, border: 0 }}>
            <div>
              <p className="section-title">User & Organization Management</p>
              <p className="field-hint">Manage accounts, assign roles, configure reporting managers, and view team direct reports.</p>
            </div>
            <Link to="/admin/users" className="btn btn-primary">Open User Management</Link>
          </div>
        </section>

        <section className="card">
          <div className="workflow-list-heading" style={{ padding: 0, border: 0 }}>
            <div>
              <p className="section-title">Workflow Engine Management</p>
              <p className="field-hint">Design, configure, reorder steps, version, and activate dynamic business approval processes.</p>
            </div>
            <Link to="/admin/workflows" className="btn">Open Workflow Management</Link>
          </div>
        </section>
      </div>
    </AppShell>
  )
}
