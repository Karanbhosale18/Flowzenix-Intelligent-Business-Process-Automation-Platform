import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AppShell from '../../components/AppShell.jsx'
import WorkflowService from '../../services/WorkflowService.js'
import AuthService from '../../services/AuthService.js'
import '../../components/AuthForm.css'
import './Workflows.css'

export default function Workflows() {
  const navigate = useNavigate()
  const [workflows, setWorkflows] = useState(null)
  const [error, setError] = useState('')
  const user = AuthService.getCurrentUser()

  useEffect(() => {
    if (!(user?.roles || []).includes('ROLE_ADMIN')) return
    WorkflowService.list(true).then(setWorkflows).catch((err) => setError(err.response?.data?.message || 'Could not load workflows.'))
  }, [])

  if (!(user?.roles || []).includes('ROLE_ADMIN')) {
    return <AppShell title="Workflow management"><div className="card"><p>You need an administrator account to manage workflows.</p></div></AppShell>
  }

  async function toggle(workflow) {
    try {
      const updated = workflow.active ? await WorkflowService.deactivate(workflow.id) : await WorkflowService.activate(workflow.id)
      setWorkflows((items) => items.map((item) => item.id === updated.id ? updated : item))
    } catch (err) { setError(err.response?.data?.message || 'Could not change workflow status.') }
  }

  return (
    <AppShell title="Workflow management" actions={<Link to="/admin/workflows/new" className="btn btn-primary">Create workflow</Link>}>
      {error && <div className="banner banner--error" role="alert">{error}</div>}
      <div className="workflow-list card">
        <div className="workflow-list-heading"><div><p className="section-title">Definitions</p><p className="field-hint">Configure the approval routes available to your team.</p></div><span className="workflow-count">{workflows?.length ?? '—'} total</span></div>
        {workflows === null && <p className="empty-state">Loading workflows…</p>}
        {workflows?.length === 0 && <p className="empty-state">No workflows yet. Create your first route.</p>}
        {workflows?.map((workflow) => (
          <div className="workflow-list-row" key={workflow.id}>
            <div className="workflow-list-icon">{workflow.steps?.length || 0}</div>
            <div className="workflow-list-copy"><Link to={`/admin/workflows/${workflow.id}`}><strong>{workflow.name}</strong></Link><span>{workflow.workflowType} · {workflow.steps?.length || 0} {(workflow.steps?.length || 0) === 1 ? 'step' : 'steps'}</span></div>
            <span className={`badge ${workflow.active ? 'badge-approved' : 'badge-neutral'}`}>{workflow.active ? 'Active' : 'Draft'}</span>
            <button type="button" className="btn workflow-toggle" onClick={() => toggle(workflow)}>{workflow.active ? 'Deactivate' : 'Activate'}</button>
          </div>
        ))}
      </div>
    </AppShell>
  )
}
