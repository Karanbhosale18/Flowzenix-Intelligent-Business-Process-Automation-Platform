import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AuthLayout from '../components/AuthLayout.jsx'
import AuthService from '../services/AuthService.js'
import '../components/AuthForm.css'

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const DEPARTMENT_ROLES = {
  'Research & Development': ['RND_MANAGER', 'RND_ENGINEER', 'RND_ANALYST'],
  'Finance & Accounting': ['FINANCE_MANAGER', 'ACCOUNTANT', 'FINANCE_ANALYST'],
  Marketing: ['MARKETING_MANAGER', 'MARKETING_SPECIALIST', 'MARKETING_ANALYST'],
  'Sales & Business Development': ['SALES_MANAGER', 'SALES_EXECUTIVE', 'BUSINESS_DEVELOPMENT_MANAGER', 'ACCOUNT_MANAGER'],
  'Technical Support / Help Desk': ['SUPPORT_MANAGER', 'SUPPORT_ENGINEER', 'HELP_DESK_AGENT', 'SYSTEM_ADMIN'],
  Admin: ['ADMIN'],
}

export default function Signup() {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    username: '', email: '', password: '', confirmPassword: '',
    role: '', department: '', managerId: '', adminId: '', adminReferenceId: '', managerReferenceId: '',
  })
  const [showPassword, setShowPassword] = useState(false)
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [success, setSuccess] = useState(false)
  const [loading, setLoading] = useState(false)

  function validate() {
    const next = {}
    if (!form.username.trim()) next.username = 'Username is required.'
    else if (form.username.trim().length < 3) next.username = 'Use at least 3 characters.'

    if (!form.email.trim()) next.email = 'Email is required.'
    else if (!EMAIL_RE.test(form.email.trim())) next.email = 'Enter a valid email address.'

    if (!form.password) next.password = 'Password is required.'
    else if (form.password.length < 8) next.password = 'Use at least 8 characters.'

    if (form.confirmPassword !== form.password) next.confirmPassword = 'Passwords do not match.'

    if (!form.role) next.role = 'Select a role.'
    if (!form.department) next.department = 'Select a department.'
    const isAdmin = form.role === 'ADMIN'
    const isManager = form.role.endsWith('_MANAGER')
    if (!isAdmin && !isManager && !form.managerId.trim()) next.managerId = 'Manager user ID is required.'
    if (isManager && !form.adminId.trim()) next.adminId = 'Admin user ID is required.'
    if (isManager && !form.managerReferenceId.trim()) next.managerReferenceId = 'Choose your Manager ID.'
    if (isAdmin && !form.adminReferenceId.trim()) next.adminReferenceId = 'Choose your Admin ID.'
    if (form.managerId.trim() && !/^[1-9]\d*$/.test(form.managerId.trim()))
      next.managerId = 'Enter a valid manager user ID (a positive number).'

    setErrors(next)
    return Object.keys(next).length === 0
  }

  function handleChange(e) {
    const { name, value } = e.target
    setForm((f) => ({ ...f, [name]: value, ...(name === 'department' ? { role: '' } : {}) }))
    if (errors[name]) setErrors((er) => ({ ...er, [name]: undefined }))
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setServerError('')
    if (!validate()) return

    setLoading(true)
    try {
      await AuthService.signup({
        username: form.username.trim(),
        email: form.email.trim(),
        password: form.password,
        role: form.role,
        department: form.department,
        managerId: form.managerId,
        adminId: form.adminId,
        adminReferenceId: form.adminReferenceId,
        managerReferenceId: form.managerReferenceId,
      })
      setSuccess(true)
      setTimeout(() => navigate('/login'), 1200)
    } catch (err) {
      const message = err.response?.data?.message || 'Could not create your account. Please try again.'
      setServerError(message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout
      eyebrow="Get started"
      title="Create your account"
      subtitle="Set up access to submit and track approval requests."
    >
      {serverError && (
        <div className="banner banner--error" role="alert">
          {serverError}
        </div>
      )}
      {success && (
        <div className="banner banner--success" role="status">
          Account created. Redirecting you to sign in…
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate>
        <div className="field">
          <label className="field-label" htmlFor="username">Username</label>
          <input
            id="username"
            name="username"
            type="text"
            className="field-input"
            placeholder="jane.doe"
            autoComplete="username"
            value={form.username}
            onChange={handleChange}
            aria-invalid={!!errors.username}
          />
          {errors.username && <p className="field-error">{errors.username}</p>}
        </div>

        <div className="field">
          <label className="field-label" htmlFor="email">Email</label>
          <input
            id="email"
            name="email"
            type="email"
            className="field-input"
            placeholder="jane@company.com"
            autoComplete="email"
            value={form.email}
            onChange={handleChange}
            aria-invalid={!!errors.email}
          />
          {errors.email && <p className="field-error">{errors.email}</p>}
        </div>

        <div className="field">
          <label className="field-label" htmlFor="password">Password</label>
          <div className="password-row">
            <input
              id="password"
              name="password"
              type={showPassword ? 'text' : 'password'}
              className="field-input"
              placeholder="At least 8 characters"
              autoComplete="new-password"
              value={form.password}
              onChange={handleChange}
              aria-invalid={!!errors.password}
              style={{ paddingRight: 56 }}
            />
            <button
              type="button"
              className="password-toggle"
              onClick={() => setShowPassword((s) => !s)}
              aria-label={showPassword ? 'Hide password' : 'Show password'}
            >
              {showPassword ? 'HIDE' : 'SHOW'}
            </button>
          </div>
          {errors.password && <p className="field-error">{errors.password}</p>}
        </div>

        <div className="field">
          <label className="field-label" htmlFor="confirmPassword">Confirm password</label>
          <input
            id="confirmPassword"
            name="confirmPassword"
            type={showPassword ? 'text' : 'password'}
            className="field-input"
            placeholder="Re-enter your password"
            autoComplete="new-password"
            value={form.confirmPassword}
            onChange={handleChange}
            aria-invalid={!!errors.confirmPassword}
          />
          {errors.confirmPassword && <p className="field-error">{errors.confirmPassword}</p>}
        </div>

        <div className="field">
          <label className="field-label" htmlFor="department">
            Department
          </label>
          <select
            id="department"
            name="department"
            className="field-input"
            value={form.department}
            onChange={handleChange}
          >
            <option value="">Select a department</option>
            <option value="Research & Development">Research &amp; Development</option>
            <option value="Finance & Accounting">Finance &amp; Accounting</option>
            <option value="Marketing">Marketing</option>
            <option value="Sales & Business Development">Sales &amp; Business Development</option>
            <option value="Technical Support / Help Desk">Technical Support / Help Desk</option>
            <option value="Admin">Admin</option>
          </select>
          {errors.department && <p className="field-error">{errors.department}</p>}
        </div>

        <div className="field">
          <label className="field-label" htmlFor="role">Role</label>
          <select id="role" name="role" className="field-input" value={form.role} onChange={handleChange} disabled={!form.department}>
            <option value="">{form.department ? 'Select a role' : 'Select a department first'}</option>
            {(DEPARTMENT_ROLES[form.department] || []).map((role) => <option key={role} value={role}>{role.replaceAll('_', ' ')}</option>)}
          </select>
          {errors.role && <p className="field-error">{errors.role}</p>}
        </div>

        {form.role && form.role !== 'ADMIN' && !form.role.endsWith('_MANAGER') && <div className="field">
          <label className="field-label" htmlFor="managerId">
            Manager ID
          </label>
          <input
            id="managerId"
            name="managerId"
            type="number"
            min="1"
            className="field-input"
            placeholder="e.g. 1"
            value={form.managerId}
            onChange={handleChange}
            aria-invalid={!!errors.managerId}
          />
          {errors.managerId ? (
            <p className="field-error">{errors.managerId}</p>
          ) : (
            <p className="field-hint">
              Enter the Manager ID created by a manager in your selected department.
            </p>
          )}
        </div>}

        {form.role.endsWith('_MANAGER') && <div className="field">
          <label className="field-label" htmlFor="adminId">Admin ID</label>
          <input id="adminId" name="adminId" type="number" min="1" className="field-input" placeholder="e.g. 1" value={form.adminId} onChange={handleChange} aria-invalid={!!errors.adminId} />
          {errors.adminId ? <p className="field-error">{errors.adminId}</p> : <p className="field-hint">All of your requests will be routed to this Admin.</p>}
        </div>}

        {form.role.endsWith('_MANAGER') && <div className="field">
          <label className="field-label" htmlFor="managerReferenceId">Choose your Manager ID</label>
          <input id="managerReferenceId" name="managerReferenceId" type="number" min="1" className="field-input" placeholder="e.g. 2001" value={form.managerReferenceId} onChange={handleChange} aria-invalid={!!errors.managerReferenceId} />
          {errors.managerReferenceId ? <p className="field-error">{errors.managerReferenceId}</p> : <p className="field-hint">Staff in your department use this ID to connect to you.</p>}
        </div>}

        {form.role === 'ADMIN' && <div className="field">
          <label className="field-label" htmlFor="adminReferenceId">Choose your Admin ID</label>
          <input id="adminReferenceId" name="adminReferenceId" type="number" min="1" className="field-input" placeholder="e.g. 1001" value={form.adminReferenceId} onChange={handleChange} aria-invalid={!!errors.adminReferenceId} />
          {errors.adminReferenceId ? <p className="field-error">{errors.adminReferenceId}</p> : <p className="field-hint">Managers use this ID when linking their account to you.</p>}
        </div>}

        <button type="submit" className="submit-btn" disabled={loading}>
          {loading ? 'Creating account…' : 'Create account'}
        </button>
      </form>

      <p className="form-switch">
        Already have an account? <Link to="/login">Sign in</Link>
      </p>
    </AuthLayout>
  )
}
