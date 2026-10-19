import { useEffect, useState, useMemo } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import AppShell from '../components/AppShell.jsx'
import TaskService from '../services/TaskService.js'
import { formatDate, statusInfo, REQUEST_TYPES } from '../utils/status.js'
import './Approvals.css'

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
  const [successMessage, setSuccessMessage] = useState('')
  const [search, setSearch] = useState('')
  const [busyId, setBusyId] = useState(null)

  // Decision Modal state
  const [decisionModal, setDecisionModal] = useState({
    open: false,
    task: null,
    action: 'approve', // 'approve' | 'reject'
    comment: '',
  })

  function load(tab) {
    setTasks(null)
    TaskService.listMine(tab.statusParam)
      .then(setTasks)
      .catch(() => setError('Could not load your approvals.'))
  }

  useEffect(() => {
    setError('')
    setSuccessMessage('')
    load(activeTab)
  }, [activeTab.key])

  function selectTab(tab) {
    setSearchParams(tab.key === TABS[0].key ? {} : { tab: tab.key })
  }

  function openDecisionModal(task, action) {
    setDecisionModal({
      open: true,
      task,
      action,
      comment: '',
    })
  }

  function closeDecisionModal() {
    setDecisionModal({ open: false, task: null, action: 'approve', comment: '' })
  }

  async function handleConfirmDecision(e) {
    e.preventDefault()
    if (!decisionModal.task) return
    const { task, action, comment } = decisionModal
    setBusyId(task.taskId)
    setError('')
    setSuccessMessage('')
    try {
      if (action === 'approve') {
        await TaskService.approve(task.taskId, comment.trim())
        setSuccessMessage(`Approved '${task.requestTitle}'.`)
      } else {
        await TaskService.reject(task.taskId, comment.trim())
        setSuccessMessage(`Rejected '${task.requestTitle}'.`)
      }
      closeDecisionModal()
      load(activeTab)
    } catch (err) {
      setError(err.response?.data?.message || 'Could not record that decision. Refresh and try again.')
    } finally {
      setBusyId(null)
    }
  }

  function typeLabel(type) {
    return REQUEST_TYPES.find((t) => t.value === type)?.label || type
  }

  const filteredTasks = useMemo(() => {
    if (!tasks) return []
    if (!search.trim()) return tasks
    const query = search.toLowerCase()
    return tasks.filter(
      (t) =>
        t.requestTitle?.toLowerCase().includes(query) ||
        t.requestedBy?.toLowerCase().includes(query) ||
        t.stepName?.toLowerCase().includes(query)
    )
  }, [tasks, search])

  return (
    <AppShell title="Approvals">
      <div className="approvals-header-controls">
        <div className="approvals-tabs">
          {TABS.map((tab) => (
            <button
              key={tab.key}
              type="button"
              className={`approvals-tab ${tab.key === activeTab.key ? 'approvals-tab--active' : ''}`}
              onClick={() => selectTab(tab)}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {tasks && tasks.length > 0 && (
          <div className="approvals-search-box">
            <input
              type="text"
              className="field-input"
              style={{ maxWidth: 260, fontSize: 13, padding: '6px 12px' }}
              placeholder="Search by title or requester..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
        )}
      </div>

      {successMessage && (
        <div className="banner banner--success" style={{ marginBottom: 16 }}>
          {successMessage}
          <button
            type="button"
            onClick={() => setSuccessMessage('')}
            style={{ float: 'right', background: 'none', border: 0, color: 'inherit', cursor: 'pointer' }}
          >
            ✕
          </button>
        </div>
      )}

      {error && <div className="banner banner--error" style={{ marginBottom: 16 }}>{error}</div>}

      {tasks === null && !error && <p className="empty-state">Loading…</p>}

      {tasks && tasks.length === 0 && (
        <div className="card empty-state">
          {activeTab.key === 'pending' ? 'Nothing waiting on you right now.' : 'Nothing here yet.'}
        </div>
      )}

      {tasks && tasks.length > 0 && (
        <div className="approvals-list">
          {filteredTasks.length === 0 ? (
            <div className="card empty-state">No approvals match your search query.</div>
          ) : (
            filteredTasks.map((t) => {
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
                      {typeLabel(t.requestType)} · from <strong>{t.requestedBy}</strong> · {t.stepName} · {formatDate(t.createdAt)}
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
                          type="button"
                          className="btn btn-primary"
                          disabled={busyId === t.taskId}
                          onClick={() => openDecisionModal(t, 'approve')}
                        >
                          Approve
                        </button>
                        <button
                          type="button"
                          className="btn btn-danger"
                          disabled={busyId === t.taskId}
                          onClick={() => openDecisionModal(t, 'reject')}
                        >
                          Reject
                        </button>
                      </>
                    ) : null}
                    <Link to={`/requests/${t.requestId}`} className="btn">Details</Link>
                  </div>
                </div>
              )
            })
          )}
        </div>
      )}

      {/* Decision Confirmation Modal */}
      {decisionModal.open && decisionModal.task && (
        <div className="modal-overlay" onClick={closeDecisionModal}>
          <div className="modal-content" style={{ maxWidth: 460 }} onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">
                {decisionModal.action === 'approve' ? 'Approve Request' : 'Reject Request'}
              </h2>
              <button type="button" className="modal-close" onClick={closeDecisionModal}>✕</button>
            </div>
            <form onSubmit={handleConfirmDecision}>
              <div className="modal-body">
                <p style={{ margin: '0 0 12px', fontSize: 14 }}>
                  {decisionModal.action === 'approve'
                    ? `Are you sure you want to approve "${decisionModal.task.requestTitle}"?`
                    : `Are you sure you want to reject "${decisionModal.task.requestTitle}"?`}
                </p>

                <div className="form-group">
                  <label className="field-label" htmlFor="decision-comment">
                    {decisionModal.action === 'reject' ? 'Rejection Reason / Comment' : 'Comment (optional)'}
                  </label>
                  <textarea
                    id="decision-comment"
                    className="field-input"
                    rows={3}
                    placeholder={
                      decisionModal.action === 'reject'
                        ? 'Explain why this request is being rejected...'
                        : 'Add any notes for the requester...'
                    }
                    value={decisionModal.comment}
                    onChange={(e) => setDecisionModal({ ...decisionModal, comment: e.target.value })}
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button type="button" className="btn" onClick={closeDecisionModal} disabled={busyId}>
                  Cancel
                </button>
                <button
                  type="submit"
                  className={decisionModal.action === 'approve' ? 'btn btn-primary' : 'btn btn-danger'}
                  disabled={busyId}
                >
                  {busyId ? 'Submitting…' : decisionModal.action === 'approve' ? 'Confirm Approval' : 'Confirm Rejection'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AppShell>
  )
}