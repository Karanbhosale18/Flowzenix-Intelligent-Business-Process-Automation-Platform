import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import AppShell from '../../components/AppShell.jsx'
import AuthService from '../../services/AuthService.js'
import WorkflowService from '../../services/WorkflowService.js'
import '../Dashboard.css'
import './Workflows.css'

/** Small admin landing page; workflow configuration is its first module. */
export default function AdminDashboard() {
  const [workflows, setWorkflows] = useState([])
  const user = AuthService.getCurrentUser()
  useEffect(() => { if ((user?.roles || []).includes('ROLE_ADMIN')) WorkflowService.list(true).then(setWorkflows).catch(() => {}) }, [])
  if (!(user?.roles || []).includes('ROLE_ADMIN')) return <AppShell title="Admin dashboard"><div className="card"><p>You need an administrator account to view this page.</p></div></AppShell>
  const active = workflows.filter((workflow) => workflow.active).length
  return (
    <AppShell title="Admin dashboard" actions={<Link to="/admin/workflows/new" className="btn btn-primary">Create workflow</Link>}>
      <div className="stats-grid"><div className="card stat-card"><p className="stat-value">{workflows.length}</p><p className="stat-label">Workflow definitions</p></div><div className="card stat-card stat-card--accent"><p className="stat-value">{active}</p><p className="stat-label">Active routes</p></div></div>
      <section className="card"><div className="workflow-list-heading" style={{ padding: 0, border: 0 }}><div><p className="section-title">Workflow management</p><p className="field-hint">Build and activate dynamic request approval routes.</p></div><Link to="/admin/workflows" className="btn">Open management</Link></div></section>
    </AppShell>
  )
}
