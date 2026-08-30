import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import AppShell from '../components/AppShell.jsx'
import RequestService from '../services/RequestService.js'
import TaskService from '../services/TaskService.js'
import ManagerFinanceService from '../services/ManagerFinanceService.js'
import AuthService from '../services/AuthService.js'
import { statusInfo, formatDate } from '../utils/status.js'
import './Dashboard.css'

export default function Dashboard() {
  const [requests, setRequests] = useState(null)
  const [tasks, setTasks] = useState(null)

  const [taskHistory, setTaskHistory] = useState(null)
  const [financeManager, setFinanceManager] = useState(null)
  const [financeManagerId, setFinanceManagerId] = useState('')
  const [financeManagerMessage, setFinanceManagerMessage] = useState('')
  const [financeManagerError, setFinanceManagerError] = useState('')
  const [savingFinanceManager, setSavingFinanceManager] = useState(false)
  const isManager = (AuthService.getCurrentUser()?.roles || []).some((role) => role.endsWith('_MANAGER'))

  useEffect(() => {
    RequestService.listMine().then(setRequests).catch(() => setRequests([]))
    TaskService.listMine().then(setTasks).catch(() => setTasks([]))
    // Everything this user has ever acted on, so we can show how many
    // requests they've approved/rejected — not just what's still pending.
    TaskService.listMine('ALL').then(setTaskHistory).catch(() => setTaskHistory([]))
    if (isManager) {
      ManagerFinanceService.getAssignment().then((assignment) => {
        setFinanceManager(assignment)
        setFinanceManagerId(assignment.financeManagerId || '')
      }).catch(() => setFinanceManagerError('Could not load your Finance Manager assignment.'))
    }
  }, [])

  async function saveFinanceManager(event) {
    event.preventDefault()
    setFinanceManagerError('')
    setFinanceManagerMessage('')
    if (!financeManagerId || Number(financeManagerId) < 1) {
      setFinanceManagerError('Enter a valid Finance Manager user ID.')
      return
    }
    setSavingFinanceManager(true)
    try {
      const assignment = await ManagerFinanceService.updateAssignment(Number(financeManagerId))
      setFinanceManager(assignment)
      setFinanceManagerId(assignment.financeManagerId)
      setFinanceManagerMessage(`Budget requests will now be delegated to ${assignment.financeManagerName}.`)
    } catch (error) {
      setFinanceManagerError(error.response?.data?.message || 'Enter the user ID of an active Finance Manager.')
    } finally {
      setSavingFinanceManager(false)
    }
  }

  const pending = requests?.filter((r) => r.status.startsWith('PENDING') || r.status === 'SUBMITTED').length ?? '—'
  const approved = requests?.filter((r) => r.status === 'APPROVED' || r.status === 'COMPLETED').length ?? '—'
  const rejected = requests?.filter((r) => r.status === 'REJECTED').length ?? '—'
  const myApprovals = tasks?.length ?? '—'
  const myDecidedApproved = taskHistory?.filter((t) => t.status === 'APPROVED').length ?? '—'
  const myDecidedRejected = taskHistory?.filter((t) => t.status === 'REJECTED').length ?? '—'

  return (
      <AppShell
          title="Dashboard"
          actions={<Link to="/requests/new" className="btn btn-primary">New request</Link>}
      >
        <div className="stats-grid">
          <StatCard label="My pending requests" value={pending} />
          <StatCard label="Approved" value={approved} />
          <StatCard label="Rejected" value={rejected} />
          <StatCard label="Awaiting my approval" value={myApprovals} accent to="/approvals" />
          <StatCard label="Approved by you" value={myDecidedApproved} to="/approvals?tab=approved" />
          <StatCard label="Rejected by you" value={myDecidedRejected} to="/approvals?tab=rejected" />
        </div>

        {isManager && <section className="card finance-assignment-card">
          <div>
            <p className="section-title">Budget Finance Manager</p>
            <p className="field-hint">Choose the active Finance Manager who receives budget requests from your team after your approval. This selection stays in effect until you change it.</p>
          </div>
          <form className="finance-assignment-form" onSubmit={saveFinanceManager} noValidate>
            <label className="field-label" htmlFor="financeManagerId">Finance Manager user ID</label>
            <div className="finance-assignment-controls">
              <input id="financeManagerId" type="number" min="1" className="field-input" value={financeManagerId} onChange={(event) => setFinanceManagerId(event.target.value)} />
              <button className="btn btn-primary" disabled={savingFinanceManager}>{savingFinanceManager ? 'Saving…' : 'Save'}</button>
            </div>
            {financeManager?.financeManagerName && <p className="field-hint">Current Finance Manager: <strong>{financeManager.financeManagerName}</strong> (user ID: {financeManager.financeManagerId})</p>}
            {financeManagerError && <p className="form-message form-message--error">{financeManagerError}</p>}
            {financeManagerMessage && <p className="form-message form-message--success">{financeManagerMessage}</p>}
          </form>
        </section>}

        <div className="dash-columns">
          <section className="card dash-section">
            <div className="dash-section-header">
              <p className="section-title">Recent requests</p>
              <Link to="/requests" className="dash-section-link">View all</Link>
            </div>
            {requests === null && <p className="empty-state">Loading…</p>}
            {requests && requests.length === 0 && <p className="empty-state">No requests yet.</p>}
            {requests && requests.slice(0, 5).map((r) => {
              const s = statusInfo(r.status)
              return (
                  <Link to={`/requests/${r.requestId}`} className="dash-row" key={r.requestId}>
                    <span className="dash-row-title">{r.title}</span>
                    <span className={`badge ${s.className}`}>{s.label}</span>
                  </Link>
              )
            })}
          </section>

          <section className="card dash-section">
            <div className="dash-section-header">
              <p className="section-title">Waiting on you</p>
              <Link to="/approvals" className="dash-section-link">View all</Link>
            </div>
            {tasks === null && <p className="empty-state">Loading…</p>}
            {tasks && tasks.length === 0 && <p className="empty-state">Nothing pending.</p>}
            {tasks && tasks.slice(0, 5).map((t) => (
                <Link to={`/requests/${t.requestId}`} className="dash-row" key={t.taskId}>
                  <span className="dash-row-title">{t.requestTitle}</span>
                  <span className="dash-row-sub">{formatDate(t.createdAt)}</span>
                </Link>
            ))}
          </section>
        </div>
      </AppShell>
  )
}

function StatCard({ label, value, accent, to }) {
  const content = (
      <>
        <p className="stat-value">{value}</p>
        <p className="stat-label">{label}</p>
      </>
  )
  const className = `card stat-card ${accent ? 'stat-card--accent' : ''}`
  return to ? (
      <Link to={to} className={className}>{content}</Link>
  ) : (
      <div className={className}>{content}</div>
  )
}
