import { useEffect, useState } from 'react'
import AppShell from '../components/AppShell.jsx'
import ProfileService from '../services/ProfileService.js'
import '../components/AuthForm.css'

const DEPARTMENTS = ['Research & Development', 'Finance & Accounting', 'Marketing', 'Sales & Business Development', 'Technical Support / Help Desk', 'Admin']

export default function Profile() {
  const [form, setForm] = useState({ password: '', department: '', managerId: '' })
  const [profile, setProfile] = useState(null)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    ProfileService.get().then((data) => {
      setProfile(data)
      setForm({ password: '', department: data.department || '', managerId: data.assignedManagerReferenceId || '' })
    }).catch(() => setError('Could not load your account details.'))
  }, [])

  function change(e) { setForm((current) => ({ ...current, [e.target.name]: e.target.value })); setMessage('') }

  async function submit(e) {
    e.preventDefault(); setError(''); setMessage('')
    if (!form.department) return setError('Choose a department.')
    if (form.password && form.password.length < 8) return setError('Your new password must be at least 8 characters.')
    setSaving(true)
    try {
      const updated = await ProfileService.update({ password: form.password || undefined, department: form.department, managerId: form.managerId ? Number(form.managerId) : null })
      setProfile(updated); setForm((current) => ({ ...current, password: '' })); setMessage('Your account details have been updated.')
    } catch (err) {
      setError(err.response?.status === 400 ? 'Check the department and manager user ID, then try again.' : 'Could not update your account details.')
    } finally { setSaving(false) }
  }

  return <AppShell title="My profile"><form className="card" onSubmit={submit} noValidate style={{ maxWidth: 620 }}>
    {error && <div className="banner banner--error">{error}</div>}
    {message && <div className="banner banner--success">{message}</div>}
    <p className="field-hint">Signed in as <strong>{profile?.username || '…'}</strong> (user ID: {profile?.id || '…'}).</p>
    {profile?.department === 'Admin' && <div className="banner banner--success">Your Admin ID: <strong>{profile.adminReferenceId || 'Not set'}</strong></div>}
    {profile?.managerReferenceId && <div className="banner banner--success">Your Manager ID: <strong>{profile.managerReferenceId}</strong></div>}
    <div className="field"><label className="field-label" htmlFor="password">New password <span className="field-optional">(optional)</span></label><input id="password" name="password" type="password" className="field-input" placeholder="Leave blank to keep your password" value={form.password} onChange={change} autoComplete="new-password" /></div>
    <div className="field"><label className="field-label" htmlFor="department">Department</label><select id="department" name="department" className="field-input" value={form.department} onChange={change}><option value="">Select a department</option>{DEPARTMENTS.map((department) => <option value={department} key={department}>{department}</option>)}</select></div>
    <div className="field"><label className="field-label" htmlFor="managerId">Manager ID <span className="field-optional">(optional)</span></label><input id="managerId" name="managerId" type="number" min="1" className="field-input" value={form.managerId} onChange={change} /><p className="field-hint">Current manager: {profile?.managerName || 'Not assigned'} {profile?.assignedManagerReferenceId ? `(Manager ID: ${profile.assignedManagerReferenceId})` : ''}</p></div>
    <button className="submit-btn" disabled={saving}>{saving ? 'Saving…' : 'Save changes'}</button>
  </form></AppShell>
}
