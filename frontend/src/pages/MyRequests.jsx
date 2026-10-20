import { useEffect, useState, useMemo } from 'react'
import { Link } from 'react-router-dom'
import AppShell from '../components/AppShell.jsx'
import RequestService from '../services/RequestService.js'
import { statusInfo, formatDate, REQUEST_TYPES } from '../utils/status.js'
import './MyRequests.css'

export default function MyRequests() {
  const [requests, setRequests] = useState(null)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [typeFilter, setTypeFilter] = useState('')

  useEffect(() => {
    RequestService.listMine()
      .then(setRequests)
      .catch((err) => setError(err.response?.data?.message || 'Could not load your requests.'))
  }, [])

  function typeLabel(type) {
    return REQUEST_TYPES.find((t) => t.value === type)?.label || type
  }

  const filteredRequests = useMemo(() => {
    if (!requests) return []
    return requests.filter((r) => {
      const matchSearch = !search.trim() ||
        r.title?.toLowerCase().includes(search.toLowerCase()) ||
        r.currentStepName?.toLowerCase().includes(search.toLowerCase())

      const matchStatus = !statusFilter || (
        statusFilter === 'PENDING' ? (r.status?.startsWith('PENDING') || r.status === 'SUBMITTED') :
        r.status === statusFilter
      )

      const matchType = !typeFilter || r.requestType === typeFilter

      return matchSearch && matchStatus && matchType
    })
  }, [requests, search, statusFilter, typeFilter])

  const hasActiveFilters = Boolean(search.trim() || statusFilter || typeFilter)

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
        <>
          <div className="card myrequests-filter-card">
            <div className="myrequests-filter-grid">
              <input
                type="text"
                className="field-input"
                placeholder="Search requests by title or step..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
              <select
                className="field-input"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <option value="">All Statuses</option>
                <option value="PENDING">All Pending</option>
                <option value="PENDING_INFORMATION">Needs Information</option>
                <option value="APPROVED">Approved</option>
                <option value="REJECTED">Rejected</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
              <select
                className="field-input"
                value={typeFilter}
                onChange={(e) => setTypeFilter(e.target.value)}
              >
                <option value="">All Types</option>
                {REQUEST_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>{t.label}</option>
                ))}
              </select>
            </div>
            <div className="myrequests-filter-footer">
              <span className="myrequests-count">
                Showing {filteredRequests.length} of {requests.length} requests
              </span>
              {hasActiveFilters && (
                <button
                  type="button"
                  className="btn btn-sm"
                  onClick={() => { setSearch(''); setStatusFilter(''); setTypeFilter('') }}
                >
                  Reset filters
                </button>
              )}
            </div>
          </div>

          <div className="card requests-table-card">
            {filteredRequests.length === 0 ? (
              <div className="empty-state">No requests match your filter criteria.</div>
            ) : (
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
                  {filteredRequests.map((r) => {
                    const s = statusInfo(r.status)
                    return (
                      <tr key={r.requestId}>
                        <td>
                          <Link className="requests-table-link" to={`/requests/${r.requestId}`}>{r.title}</Link>
                        </td>
                        <td className="requests-table-muted">{typeLabel(r.requestType)}</td>
                        <td className="requests-table-muted">{r.currentStepName || 'Completed'}</td>
                        <td><span className={`badge ${s.className}`}>{s.label}</span></td>
                        <td className="requests-table-muted">{formatDate(r.updatedAt)}</td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}
    </AppShell>
  )
}
