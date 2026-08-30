import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import AppShell from '../components/AppShell.jsx'
import TaskService from '../services/TaskService.js'
import { formatDate, statusInfo, REQUEST_TYPES } from '../utils/status.js'
import './Approvals.css'

// Each tab maps to the `status` filter sent to GET /api/tasks/my.
// `null` means "no filter" -> the backend's default pending-only inbox.
const TABS = [
  { key: 'pending', label: 'Pending', statusParam: null },
  { key: 'approved', label: 'Approved', statusParam: 'APPROVED' },
  { key: 'rejected', label: 'Rejected', statusParam: 'REJECTED' },
  { key: 'all', label: 'All', statusParam: 'ALL' },
]

export default function Approvals() {
  const [searchParams, setSearchParams] = useSearchParams()
  const activeTab = TABS.find((t) => t.key === searchParams.get('tab')) || TABS[0]

  const [tasks, setTasks] = useState(null)
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)

  function load(tab) {
    setTasks(null)
    TaskService.listMine(tab.statusParam)
        .then(setTasks)
        .catch(() => setError('Could not load your approvals.'))
  }

  useEffect(() => { setError(''); load(activeTab) }, [activeTab.key])

  function selectTab(tab) {
    setSearchParams(tab.key === TABS[0].key ? {} : { tab: tab.key })
  }

  async function handleDecision(taskId, decision) {
    setBusyId(taskId)
    try {
      if (decision === 'approve') await TaskService.approve(taskId, '')
      else await TaskService.reject(taskId, '')
      load(activeTab)
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
        <div className="approvals-tabs">
          {TABS.map((tab) => (
              <button
                  key={tab.key}
                  className={`approvals-tab ${tab.key === activeTab.key ? 'approvals-tab--active' : ''}`}
                  onClick={() => selectTab(tab)}
              >
                {tab.label}
              </button>
          ))}
        </div>

        {error && <div className="banner banner--error">{error}</div>}

        {tasks === null && !error && <p className="empty-state">Loading…</p>}

        {tasks && tasks.length === 0 && (
            <div className="card empty-state">
              {activeTab.key === 'pending' ? 'Nothing waiting on you right now.' : 'Nothing here yet.'}
            </div>
        )}

        {tasks && tasks.length > 0 && (
            <div className="approvals-list">
              {tasks.map((t) => {
                const isPending = t.status === 'PENDING'
                const s = statusInfo(t.status)
                return (
                    <div className="card approval-card" key={t.taskId}>
                      <div className="approval-info">
                        <div className="approval-title-row">
                          <Link to={`/requests/${t.requestId}`} className="approval-title">{t.requestTitle}</Link>
                          <span className={`badge ${s.className}`}>{s.label}</span>
                        </div>
                        <p className="approval-meta">
                          {typeLabel(t.requestType)} · from {t.requestedBy} · {t.stepName} · {formatDate(t.createdAt)}
                        </p>
                        {!isPending && (
                            <p className="approval-meta approval-decision">
                              {s.label} {formatDate(t.completedAt)}
                              {t.comment ? ` · "${t.comment}"` : ''}
                            </p>
                        )}
                      </div>
                      <div className="approval-actions">
                        {isPending ? (
                            <>
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
                            </>
                        ) : null}
                        <Link to={`/requests/${t.requestId}`} className="btn">Details</Link>
                      </div>
                    </div>
                )
              })}
            </div>
        )}
      </AppShell>
  )
}