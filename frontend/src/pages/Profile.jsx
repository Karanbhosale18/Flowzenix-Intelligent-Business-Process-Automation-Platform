import { useEffect, useState } from 'react'
import AppShell from '../components/AppShell.jsx'
import ProfileService from '../services/ProfileService.js'
import '../components/AuthForm.css'

const DEPARTMENTS = ['Research & Development', 'Finance & Accounting', 'Marketing', 'Sales & Business Development', 'Technical Support / Help Desk', 'Admin']

export default function Profile() {
  const [password, setPassword] = useState('')
  const [profile, setProfile] = useState(null)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    ProfileService.get().then((data) => {
      setProfile(data)
    }).catch(() => setError('Could not load your account details.'))
  }, [])

  async function submit(e) {
    e.preventDefault()
    setError('')
    setMessage('')
    if (!password) return setError('Enter a new password if you want to change it.')
    if (password.length < 8) return setError('Your new password must be at least 8 characters.')
    setSaving(true)
    try {
      const updated = await ProfileService.update({ password })
      setProfile(updated)
      setPassword('')
      setMessage('Your password has been updated successfully.')
    } catch (err) {
      setError(err.response?.data?.message || 'Could not update your account details.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <AppShell title="My profile">
      <form className="card" onSubmit={submit} noValidate style={{ maxWidth: 620 }}>
        {error && <div className="banner banner--error">{error}</div>}
        {message && <div className="banner banner--success">{message}</div>}
        <p className="field-hint">
          Signed in as <strong>{profile?.username || '…'}</strong> ({profile?.email || '…'}) · User ID: {profile?.id || '…'}
        </p>
        {profile?.adminReferenceId && (
          <div className="banner banner--success">Your Admin ID: <strong>{profile.adminReferenceId}</strong></div>
        )}
        {profile?.managerReferenceId && (
          <div className="banner banner--success">Your Manager ID: <strong>{profile.managerReferenceId}</strong></div>
        )}

        <div className="field" style={{ marginTop: 16 }}>
          <label className="field-label">Department</label>
          <input className="field-input" value={profile?.department || 'Not assigned'} disabled readOnly />
          <p className="field-hint">Department assignments are managed by an administrator.</p>
        </div>

        <div className="field">
          <label className="field-label">Reporting Manager</label>
          <input
            className="field-input"
            value={profile?.managerName ? `${profile.managerName} ${profile.assignedManagerReferenceId ? `(Manager ID: ${profile.assignedManagerReferenceId})` : ''}` : 'Not assigned'}
            disabled
            readOnly
          />
          <p className="field-hint">Manager assignments are managed by an administrator.</p>
        </div>

        <div className="field">
          <label className="field-label" htmlFor="password">Change password</label>
          <input
            id="password"
            name="password"
            type="password"
            className="field-input"
            placeholder="Enter at least 8 characters"
            value={password}
            onChange={(e) => { setPassword(e.target.value); setMessage('') }}
            autoComplete="new-password"
          />
        </div>

        <button className="submit-btn" disabled={saving || !password}>
          {saving ? 'Updating…' : 'Update password'}
        </button>
      </form>
    </AppShell>
  )
}
