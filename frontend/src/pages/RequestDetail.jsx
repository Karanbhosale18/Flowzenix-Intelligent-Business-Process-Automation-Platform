import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import AppShell from '../components/AppShell.jsx'
import RequestService from '../services/RequestService.js'
import TaskService from '../services/TaskService.js'
import { statusInfo, formatDate, REQUEST_TYPES } from '../utils/status.js'
import './RequestDetail.css'

export default function RequestDetail() {
  const { id } = useParams()
  const [detail, setDetail] = useState(null)
  const [error, setError] = useState('')
  const [comment, setComment] = useState('')
  const [acting, setActing] = useState(false)
  const [actionError, setActionError] = useState('')

  function load() {
    RequestService.getById(id)
      .then(setDetail)
      .catch((err) => {
        setError(err.response?.status === 403
          ? "You don't have access to this request."
          : 'Could not load this request.')
      })
  }

  useEffect(() => { load() }, [id])

  async function act(action) {
    if (!detail?.myPendingTaskId) return
    setActing(true)
    setActionError('')
    try {
      if (action === 'approve') await TaskService.approve(detail.myPendingTaskId, comment)
      else if (action === 'reject') await TaskService.reject(detail.myPendingTaskId, comment)
      else await TaskService.requestInformation(detail.myPendingTaskId, comment)
      setComment('')
      load()
    } catch (err) {
      setActionError(err.response?.data?.message || 'Could not record your decision.')
    } finally {
      setActing(false)
    }
  }

  if (error) {
    return (
      <AppShell title="Request">
        <div className="banner banner--error">{error}</div>
        <Link to="/requests" className="btn">Back to my requests</Link>
      </AppShell>
    )
  }

  if (!detail) {
    return (
      <AppShell title="Request">
        <p className="empty-state">Loading…</p>
      </AppShell>
    )
  }

  const s = statusInfo(detail.status)
  const typeLabel = REQUEST_TYPES.find((t) => t.value === detail.requestType)?.label || detail.requestType

  return (
    <AppShell title={detail.title}>
      <div className="request-detail-layout">
        <div className="request-detail-main">
          <div className="card">
            <div className="request-detail-header">
              <span className="request-detail-type">{typeLabel}</span>
              <span className={`badge ${s.className}`}>{s.label}</span>
            </div>

            {detail.description && <p className="request-detail-description">{detail.description}</p>}

            {detail.metadata && Object.keys(detail.metadata).length > 0 && (
              <dl className="request-detail-meta">
                {Object.entries(detail.metadata).map(([key, value]) => (
                  <div className="request-detail-meta-row" key={key}>
                    <dt>{key}</dt>
                    <dd>{String(value)}</dd>
                  </div>
                ))}
              </dl>
            )}

            <p className="request-detail-submitted">Submitted {formatDate(detail.createdAt)}</p>
          </div>

          {detail.myPendingTaskId && (
            <div className="card action-card">
              <p className="action-title">This request is waiting on your decision</p>
              {actionError && <div className="banner banner--error">{actionError}</div>}
              <textarea
                className="field-input action-comment"
                rows={2}
                placeholder="Optional comment"
                value={comment}
                onChange={(e) => setComment(e.target.value)}
              />
              <div className="action-buttons">
                <button className="btn btn-primary" disabled={acting} onClick={() => act('approve')}>Approve</button>
                <button className="btn btn-danger" disabled={acting} onClick={() => act('reject')}>Reject</button>
                <button className="btn" disabled={acting} onClick={() => act('info')}>Request information</button>
              </div>
            </div>
          )}

          <div className="card">
            <p className="section-title">Activity</p>
            <ol className="history-list">
              {detail.history.map((h, i) => (
                <li key={i} className="history-item">
                  <div className="history-dot" />
                  <div>
                    <p className="history-action">{h.action}</p>
                    <p className="history-meta">
                      {h.performedBy} · {formatDate(h.createdAt)}
                      {h.comment && <span className="history-comment"> — "{h.comment}"</span>}
                    </p>
                  </div>
                </li>
              ))}
            </ol>
          </div>
        </div>

        <aside className="card request-detail-timeline">
          <p className="section-title">Workflow progress</p>
          <ol className="step-list">
            {detail.steps.map((step) => (
              <li key={step.stepOrder} className="step-item">
                <span className={`step-marker ${step.completed ? 'step-marker--done' : step.current ? 'step-marker--current' : ''}`}>
                  {step.completed ? '\u2713' : step.current ? '\u25CF' : '\u25CB'}
                </span>
                <div>
                  <p className="step-name">{step.name}</p>
                  <p className="step-role">{step.assignedRole?.replace('ROLE_', '')}</p>
                </div>
              </li>
            ))}
          </ol>
        </aside>
      </div>
    </AppShell>
  )
}
