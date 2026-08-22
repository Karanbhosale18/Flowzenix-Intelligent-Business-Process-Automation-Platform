import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import AppShell from '../components/AppShell.jsx'
import RequestService from '../services/RequestService.js'
import { statusInfo, formatDate, REQUEST_TYPES } from '../utils/status.js'
import './MyRequests.css'

export default function MyRequests() {
  const [requests, setRequests] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    RequestService.listMine()
      .then(setRequests)
      .catch(() => setError('Could not load your requests.'))
  }, [])

  function typeLabel(type) {
    return REQUEST_TYPES.find((t) => t.value === type)?.label || type
  }

  return (
    <AppShell
      title="My requests"
      actions={<Link to="/requests/new" className="btn btn-primary">New request</Link>}
    >
      {error && <div className="banner banner--error">{error}</div>}

      {requests === null && !error && <p className="empty-state">Loading…</p>}

      {requests && requests.length === 0 && (
        <div className="card empty-state">
          <p>You haven't submitted any requests yet.</p>
          <Link to="/requests/new" className="btn btn-primary" style={{ display: 'inline-block', marginTop: 12 }}>
            Submit your first request
          </Link>
        </div>
      )}

      {requests && requests.length > 0 && (
        <div className="card requests-table-card">
          <table className="requests-table">
            <thead>
              <tr>
                <th>Request</th>
                <th>Type</th>
                <th>Current step</th>
                <th>Status</th>
                <th>Updated</th>
              </tr>
            </thead>
            <tbody>
              {requests.map((r) => {
                const s = statusInfo(r.status)
                return (
                  <tr key={r.requestId}>
                    <td>
                      <Link className="requests-table-link" to={`/requests/${r.requestId}`}>{r.title}</Link>
                    </td>
                    <td className="requests-table-muted">{typeLabel(r.requestType)}</td>
                    <td className="requests-table-muted">{r.currentStepName}</td>
                    <td><span className={`badge ${s.className}`}>{s.label}</span></td>
                    <td className="requests-table-muted">{formatDate(r.updatedAt)}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </AppShell>
  )
}
