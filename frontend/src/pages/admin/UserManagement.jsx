import { useState, useEffect } from 'react'
import AppShell from '../../components/AppShell.jsx'
import AdminUserService from '../../services/AdminUserService.js'
import AuthService from '../../services/AuthService.js'
import './UserManagement.css'

const DEPARTMENTS = [
  'Engineering',
  'Finance & Accounting',
  'Human Resources',
  'Operations',
  'Marketing',
  'Sales',
  'Legal'
]

const ALL_ROLES = [
  { id: 'ROLE_EMPLOYEE', label: 'Employee' },
  { id: 'ROLE_MANAGER', label: 'Manager' },
  { id: 'ROLE_FINANCE_MANAGER', label: 'Finance Manager' },
  { id: 'ROLE_ADMIN', label: 'Administrator' }
]

export default function UserManagement() {
  const currentUser = AuthService.getCurrentUser()
  const isAdmin = (currentUser?.roles || []).includes('ROLE_ADMIN')

  const [users, setUsers] = useState([])
  const [managers, setManagers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  // Filters
  const [search, setSearch] = useState('')
  const [roleFilter, setRoleFilter] = useState('')
  const [deptFilter, setDeptFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')

  // Modals state
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [showEditModal, setShowEditModal] = useState(false)
  const [showReportsModal, setShowReportsModal] = useState(false)

  // Current editing user
  const [selectedUser, setSelectedUser] = useState(null)
  const [directReports, setDirectReports] = useState([])
  const [loadingReports, setLoadingReports] = useState(false)

  // Forms state
  const [createForm, setCreateForm] = useState({
    username: '',
    email: '',
    password: '',
    department: 'Engineering',
    managerId: '',
    roles: ['ROLE_EMPLOYEE']
  })

  const [editForm, setEditForm] = useState({
    department: '',
    managerId: '',
    roles: [],
    enabled: true
  })
  const [actionError, setActionError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (isAdmin) {
      loadUsers()
      loadManagers()
    }
  }, [isAdmin, search, roleFilter, deptFilter, statusFilter])

  async function loadUsers() {
    setLoading(true)
    setError('')
    try {
      const params = {}
      if (search.trim()) params.search = search.trim()
      if (roleFilter) params.role = roleFilter
      if (deptFilter) params.department = deptFilter
      if (statusFilter !== '') params.enabled = statusFilter === 'active'
      const res = await AdminUserService.getUsers(params)
      setUsers(res.data)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load user accounts.')
    } finally {
      setLoading(false)
    }
  }

  async function loadManagers() {
    try {
      const res = await AdminUserService.getManagers()
      setManagers(res.data)
    } catch (err) {
      console.error('Failed to load eligible managers', err)
    }
  }

  function handleOpenCreate() {
    setActionError('')
    setCreateForm({
      username: '',
      email: '',
      password: '',
      department: 'Engineering',
      managerId: '',
      roles: ['ROLE_EMPLOYEE']
    })
    setShowCreateModal(true)
  }

  async function handleCreateUser(e) {
    e.preventDefault()
    setActionError('')
    setSubmitting(true)
    try {
      const payload = {
        username: createForm.username.trim(),
        email: createForm.email.trim(),
        password: createForm.password,
        department: createForm.department,
        managerId: createForm.managerId ? Number(createForm.managerId) : null,
        roles: createForm.roles
      }
      await AdminUserService.createUser(payload)
      setShowCreateModal(false)
      setSuccessMessage(`User '${payload.username}' successfully created.`)
      loadUsers()
      loadManagers()
    } catch (err) {
      setActionError(err.response?.data?.message || 'Failed to create user.')
    } finally {
      setSubmitting(false)
    }
  }

  function handleOpenEdit(user) {
    setSelectedUser(user)
    setActionError('')
    setEditForm({
      department: user.department || '',
      managerId: user.managerId ? String(user.managerId) : '',
      roles: [...(user.roles || [])],
      enabled: user.enabled ?? true
    })
    setShowEditModal(true)
  }

  async function handleUpdateUser(e) {
    e.preventDefault()
    setActionError('')
    setSubmitting(true)
    try {
      const payload = {
        department: editForm.department,
        managerId: editForm.managerId ? Number(editForm.managerId) : null,
        roles: editForm.roles,
        enabled: editForm.enabled
      }
      await AdminUserService.updateUser(selectedUser.id, payload)
      setShowEditModal(false)
      setSuccessMessage(`Account '${selectedUser.username}' updated successfully.`)
      loadUsers()
      loadManagers()
    } catch (err) {
      setActionError(err.response?.data?.message || 'Failed to update user.')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleOpenReports(user) {
    setSelectedUser(user)
    setShowReportsModal(true)
    setLoadingReports(true)
    setDirectReports([])
    try {
      const res = await AdminUserService.getDirectReports(user.id)
      setDirectReports(res.data)
    } catch (err) {
      console.error('Failed to load direct reports', err)
    } finally {
      setLoadingReports(false)
    }
  }

  function toggleRole(roleId, isCreate = false) {
    if (isCreate) {
      const current = createForm.roles
      const next = current.includes(roleId)
        ? current.filter(r => r !== roleId)
        : [...current, roleId]
      setCreateForm({ ...createForm, roles: next.length > 0 ? next : ['ROLE_EMPLOYEE'] })
    } else {
      const current = editForm.roles
      const next = current.includes(roleId)
        ? current.filter(r => r !== roleId)
        : [...current, roleId]
      setEditForm({ ...editForm, roles: next.length > 0 ? next : ['ROLE_EMPLOYEE'] })
    }
  }

  if (!isAdmin) {
    return (
      <AppShell title="User Management">
        <div className="card">
          <p>Access denied. Administrator privileges are required to view this page.</p>
        </div>
      </AppShell>
    )
  }

  return (
    <AppShell
      title="User Management"
      actions={
        <button className="btn btn-primary" onClick={handleOpenCreate}>
          Create User
        </button>
      }
    >
      {successMessage && (
        <div className="form-message form-message--success" style={{ marginBottom: 16 }}>
          {successMessage}
          <button
            onClick={() => setSuccessMessage('')}
            style={{ float: 'right', background: 'none', border: 0, color: 'inherit', cursor: 'pointer' }}
          >
            ✕
          </button>
        </div>
      )}

      {error && (
        <div className="form-message form-message--error" style={{ marginBottom: 16 }}>
          {error}
        </div>
      )}

      {/* Filters Bar */}
      <div className="card" style={{ marginBottom: 16, padding: '16px 20px' }}>
        <div className="user-mgmt-filters">
          <div>
            <input
              type="text"
              className="field-input"
              placeholder="Search by username or email..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <div>
            <select
              className="field-input"
              value={roleFilter}
              onChange={(e) => setRoleFilter(e.target.value)}
            >
              <option value="">All Roles</option>
              {ALL_ROLES.map(r => (
                <option key={r.id} value={r.id}>{r.label}</option>
              ))}
            </select>
          </div>
          <div>
            <select
              className="field-input"
              value={deptFilter}
              onChange={(e) => setDeptFilter(e.target.value)}
            >
              <option value="">All Departments</option>
              {DEPARTMENTS.map(d => (
                <option key={d} value={d}>{d}</option>
              ))}
            </select>
          </div>
          <div>
            <select
              className="field-input"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="">All Statuses</option>
              <option value="active">Active Only</option>
              <option value="inactive">Inactive Only</option>
            </select>
          </div>
        </div>
      </div>

      {/* User Table */}
      <div className="card user-table-card">
        {loading ? (
          <div style={{ padding: '36px', textAlign: 'center', color: 'var(--text-muted)' }}>
            Loading users...
          </div>
        ) : users.length === 0 ? (
          <div style={{ padding: '36px', textAlign: 'center', color: 'var(--text-muted)' }}>
            No users match the given filter criteria.
          </div>
        ) : (
          <table className="user-table">
            <thead>
              <tr>
                <th>User</th>
                <th>Roles</th>
                <th>Department</th>
                <th>Reporting Manager</th>
                <th>Status</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => {
                const isUserAdmin = u.roles?.includes('ROLE_ADMIN')
                const isUserManager = u.roles?.includes('ROLE_MANAGER')
                const isUserFinance = u.roles?.includes('ROLE_FINANCE_MANAGER') || u.roles?.includes('ROLE_FINANCE')

                return (
                  <tr key={u.id}>
                    <td>
                      <div className="user-info-cell">
                        <span className="user-info-name">{u.username}</span>
                        <span className="user-info-email">{u.email}</span>
                      </div>
                    </td>
                    <td>
                      <div className="user-roles-list">
                        {(u.roles || []).map((r) => {
                          let badgeClass = 'role-badge'
                          if (r === 'ROLE_ADMIN') badgeClass += ' role-badge--admin'
                          else if (r.endsWith('_MANAGER')) badgeClass += ' role-badge--manager'
                          else if (r.includes('FINANCE')) badgeClass += ' role-badge--finance'
                          return (
                            <span key={r} className={badgeClass}>
                              {r.replace('ROLE_', '')}
                            </span>
                          )
                        })}
                      </div>
                    </td>
                    <td>{u.department || '—'}</td>
                    <td>{u.managerUsername || 'None'}</td>
                    <td>
                      <span className={`status-badge ${u.enabled ? 'status-badge--active' : 'status-badge--inactive'}`}>
                        {u.enabled ? 'Active' : 'Disabled'}
                      </span>
                    </td>
                    <td>
                      <div className="user-actions" style={{ justifyContent: 'flex-end' }}>
                        {isUserManager && (
                          <button
                            className="btn btn-sm"
                            title="View Direct Reports"
                            onClick={() => handleOpenReports(u)}
                          >
                            Reports
                          </button>
                        )}
                        <button
                          className="btn btn-sm btn-primary"
                          onClick={() => handleOpenEdit(u)}
                        >
                          Edit
                        </button>
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </div>

      {/* CREATE USER MODAL */}
      {showCreateModal && (
        <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">Create New User</h2>
              <button className="modal-close" onClick={() => setShowCreateModal(false)}>✕</button>
            </div>
            <form onSubmit={handleCreateUser}>
              <div className="modal-body">
                {actionError && <div className="error-banner">{actionError}</div>}

                <div className="form-group">
                  <label className="field-label">Username</label>
                  <input
                    type="text"
                    required
                    className="field-input"
                    value={createForm.username}
                    onChange={(e) => setCreateForm({ ...createForm, username: e.target.value })}
                  />
                </div>

                <div className="form-group">
                  <label className="field-label">Email Address</label>
                  <input
                    type="email"
                    required
                    className="field-input"
                    value={createForm.email}
                    onChange={(e) => setCreateForm({ ...createForm, email: e.target.value })}
                  />
                </div>

                <div className="form-group">
                  <label className="field-label">Initial Password</label>
                  <input
                    type="password"
                    required
                    minLength={6}
                    className="field-input"
                    value={createForm.password}
                    onChange={(e) => setCreateForm({ ...createForm, password: e.target.value })}
                  />
                </div>

                <div className="form-group">
                  <label className="field-label">Department</label>
                  <select
                    className="field-input"
                    value={createForm.department}
                    onChange={(e) => setCreateForm({ ...createForm, department: e.target.value })}
                  >
                    {DEPARTMENTS.map(d => (
                      <option key={d} value={d}>{d}</option>
                    ))}
                  </select>
                </div>

                <div className="form-group">
                  <label className="field-label">Reporting Manager</label>
                  <select
                    className="field-input"
                    value={createForm.managerId}
                    onChange={(e) => setCreateForm({ ...createForm, managerId: e.target.value })}
                  >
                    <option value="">None (Top-Level)</option>
                    {managers.map(m => (
                      <option key={m.id} value={m.id}>{m.username} ({m.department || 'No dept'})</option>
                    ))}
                  </select>
                </div>

                <div className="form-group">
                  <label className="field-label">Assigned Roles</label>
                  <div className="checkbox-group">
                    {ALL_ROLES.map(r => (
                      <label key={r.id} className="checkbox-label">
                        <input
                          type="checkbox"
                          checked={createForm.roles.includes(r.id)}
                          onChange={() => toggleRole(r.id, true)}
                        />
                        {r.label}
                      </label>
                    ))}
                  </div>
                </div>

                {createForm.roles.includes('ROLE_ADMIN') && (
                  <div className="warning-banner">
                    ⚠️ Granting Administrator privileges allows this user full platform access.
                  </div>
                )}
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn"
                  onClick={() => setShowCreateModal(false)}
                  disabled={submitting}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={submitting}
                >
                  {submitting ? 'Creating...' : 'Create Account'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* EDIT USER MODAL */}
      {showEditModal && selectedUser && (
        <div className="modal-overlay" onClick={() => setShowEditModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">Edit User: {selectedUser.username}</h2>
              <button className="modal-close" onClick={() => setShowEditModal(false)}>✕</button>
            </div>
            <form onSubmit={handleUpdateUser}>
              <div className="modal-body">
                {actionError && <div className="error-banner">{actionError}</div>}

                <div className="form-group">
                  <label className="field-label">Department</label>
                  <select
                    className="field-input"
                    value={editForm.department}
                    onChange={(e) => setEditForm({ ...editForm, department: e.target.value })}
                  >
                    <option value="">Unassigned</option>
                    {DEPARTMENTS.map(d => (
                      <option key={d} value={d}>{d}</option>
                    ))}
                  </select>
                </div>

                <div className="form-group">
                  <label className="field-label">Reporting Manager</label>
                  <select
                    className="field-input"
                    value={editForm.managerId}
                    onChange={(e) => setEditForm({ ...editForm, managerId: e.target.value })}
                  >
                    <option value="">None (Top-Level)</option>
                    {managers
                      .filter(m => m.id !== selectedUser.id)
                      .map(m => (
                        <option key={m.id} value={m.id}>{m.username} ({m.department || 'No dept'})</option>
                      ))}
                  </select>
                  <span className="field-hint" style={{ fontSize: 11 }}>
                    Circular reporting chains are strictly detected and rejected.
                  </span>
                </div>

                <div className="form-group">
                  <label className="field-label">Assigned Roles</label>
                  <div className="checkbox-group">
                    {ALL_ROLES.map(r => (
                      <label key={r.id} className="checkbox-label">
                        <input
                          type="checkbox"
                          checked={editForm.roles.includes(r.id)}
                          onChange={() => toggleRole(r.id, false)}
                        />
                        {r.label}
                      </label>
                    ))}
                  </div>
                </div>

                {editForm.roles.includes('ROLE_ADMIN') && !selectedUser.roles?.includes('ROLE_ADMIN') && (
                  <div className="warning-banner">
                    ⚠️ Granting Administrator role provides full administrative access.
                  </div>
                )}

                <div className="form-group">
                  <label className="field-label">Account Status</label>
                  <select
                    className="field-input"
                    value={editForm.enabled ? 'true' : 'false'}
                    onChange={(e) => setEditForm({ ...editForm, enabled: e.target.value === 'true' })}
                  >
                    <option value="true">Active</option>
                    <option value="false">Disabled (Suspended)</option>
                  </select>
                </div>

                {!editForm.enabled && (
                  <div className="warning-banner">
                    ⚠️ Disabling this account prevents the user from signing in and acting on approval tasks.
                  </div>
                )}
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn"
                  onClick={() => setShowEditModal(false)}
                  disabled={submitting}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={submitting}
                >
                  {submitting ? 'Saving...' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* DIRECT REPORTS MODAL */}
      {showReportsModal && selectedUser && (
        <div className="modal-overlay" onClick={() => setShowReportsModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">Direct Reports: {selectedUser.username}</h2>
              <button className="modal-close" onClick={() => setShowReportsModal(false)}>✕</button>
            </div>
            <div className="modal-body">
              {loadingReports ? (
                <p style={{ textAlign: 'center', color: 'var(--text-muted)' }}>Loading direct reports...</p>
              ) : directReports.length === 0 ? (
                <p style={{ textAlign: 'center', color: 'var(--text-muted)' }}>
                  No employees directly report to this manager yet.
                </p>
              ) : (
                <div className="reports-list">
                  {directReports.map(emp => (
                    <div key={emp.id} className="report-item">
                      <div>
                        <strong>{emp.username}</strong>
                        <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                          {emp.email} • {emp.department || 'No dept'}
                        </div>
                      </div>
                      <span className={`status-badge ${emp.enabled ? 'status-badge--active' : 'status-badge--inactive'}`}>
                        {emp.enabled ? 'Active' : 'Disabled'}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
            <div className="modal-footer">
              <button className="btn" onClick={() => setShowReportsModal(false)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </AppShell>
  )
}
