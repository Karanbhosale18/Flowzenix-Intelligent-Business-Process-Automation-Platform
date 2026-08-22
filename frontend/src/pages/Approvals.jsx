import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import AppShell from '../components/AppShell.jsx'
import TaskService from '../services/TaskService.js'
import { formatDate, REQUEST_TYPES } from '../utils/status.js'
import './Approvals.css'

export default function Approvals() {
  const [tasks, setTasks] = useState(null)
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)

  function load() {
    TaskService.listMine()
      .then(setTasks)
      .catch(() => setError('Could not load your pending approvals.'))
  }

  useEffect(() => { load() }, [])

  async function handleDecision(taskId, decision) {
    setBusyId(taskId)
    try {
      if (decision === 'approve') await TaskService.approve(taskId, '')
      else await TaskService.reject(taskId, '')
      load()
    } catch {
      setError('Could not record that decision. Refresh and try again.')
    } finally {
      setBusyId(null)
    }
  }

  function typeLabel(type) {
    return REQUEST_TYPES.find((t) => t.value === type)?.label || type
  }

  return (
    <AppShell title="Approvals">
      {error && <div className="banner banner--error">{error}</div>}

      {tasks === null && !error && <p className="empty-state">Loading…</p>}

      {tasks && tasks.length === 0 && (
        <div className="card empty-state">Nothing waiting on you right now.</div>
      )}

      {tasks && tasks.length > 0 && (
        <div className="approvals-list">
          {tasks.map((t) => (
            <div className="card approval-card" key={t.taskId}>
              <div className="approval-info">
                <Link to={`/requests/${t.requestId}`} className="approval-title">{t.requestTitle}</Link>
                <p className="approval-meta">
                  {typeLabel(t.requestType)} · from {t.requestedBy} · {t.stepName} · {formatDate(t.createdAt)}
                </p>
              </div>
              <div className="approval-actions">
                <button
                  className="btn btn-primary"
                  disabled={busyId === t.taskId}
                  onClick={() => handleDecision(t.taskId, 'approve')}
                >
                  Approve
                </button>
                <button
                  className="btn btn-danger"
                  disabled={busyId === t.taskId}
                  onClick={() => handleDecision(t.taskId, 'reject')}
                >
                  Reject
                </button>
                <Link to={`/requests/${t.requestId}`} className="btn">Details</Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </AppShell>
  )
}
