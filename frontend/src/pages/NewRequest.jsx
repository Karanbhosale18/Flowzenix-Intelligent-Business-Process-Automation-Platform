import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AppShell from '../components/AppShell.jsx'
import RequestService from '../services/RequestService.js'
import { REQUEST_TYPES, fieldsForType } from '../utils/status.js'
import WorkflowService from '../services/WorkflowService.js'
import ProfileService from '../services/ProfileService.js'
import '../components/AuthForm.css'
import './NewRequest.css'

export default function NewRequest() {
  const navigate = useNavigate()
  const [requestType, setRequestType] = useState('')
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [priority, setPriority] = useState('MEDIUM')
  const [metadata, setMetadata] = useState({})
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [loading, setLoading] = useState(false)
  const [workflowTypes, setWorkflowTypes] = useState(REQUEST_TYPES)
  const [profile, setProfile] = useState(null)

  useEffect(() => {
    WorkflowService.listActive().then((definitions) => {
      if (!definitions?.length) return
      setWorkflowTypes(definitions.map((definition) => ({
        value: definition.workflowType,
        label: definition.name,
        // Existing request-specific fields remain available; newly-created
        // definitions intentionally start with a generic title/description form.
        fields: fieldsForType(definition.workflowType),
      })))
    }).catch(() => { /* the static catalogue keeps the form usable during upgrades */ })
  }, [])

  useEffect(() => { ProfileService.get().then(setProfile).catch(() => setProfile({})) }, [])

  const fields = workflowTypes.find((type) => type.value === requestType)?.fields || []

  function handleTypeChange(value) {
    setRequestType(value)
    setMetadata({})
    setErrors({})
  }

  function handleFieldChange(key, value) {
    setMetadata((m) => ({ ...m, [key]: value }))
    if (errors[key]) setErrors((e) => ({ ...e, [key]: undefined }))
  }

  function validate() {
    const next = {}
    if (!requestType) next.requestType = 'Choose a request type.'
    if (!title.trim()) next.title = 'Give the request a short title.'
    fields.forEach((f) => {
      if (f.required && !metadata[f.key]) next[f.key] = `${f.label} is required.`
    })
    setErrors(next)
    return Object.keys(next).length === 0
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setServerError('')
    if (!validate()) return

    setLoading(true)
    try {
      const created = await RequestService.create({
        requestType,
        title: title.trim(),
        description: description.trim(),
        priority,
        metadata,
      })
      navigate(`/requests/${created.requestId}`)
    } catch (err) {
      setServerError(err.response?.data?.message || 'Could not submit the request. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AppShell title="New request">
      <div className="new-request-layout">
        <form className="card new-request-form" onSubmit={handleSubmit} noValidate>
          {serverError && <div className="banner banner--error" role="alert">{serverError}</div>}

          <div className="field">
            <label className="field-label" htmlFor="requestType">Request type</label>
            <select
              id="requestType"
              className="field-input"
              value={requestType}
              onChange={(e) => handleTypeChange(e.target.value)}
              aria-invalid={!!errors.requestType}
            >
              <option value="">Select a type…</option>
              {workflowTypes.map((t) => (
                <option key={t.value} value={t.value}>{t.label}</option>
              ))}
            </select>
            {errors.requestType && <p className="field-error">{errors.requestType}</p>}
          </div>

          <div className="field">
            <label className="field-label" htmlFor="title">Title</label>
            <input
              id="title"
              className="field-input"
              placeholder="e.g. Leave for family function"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              aria-invalid={!!errors.title}
            />
            {errors.title && <p className="field-error">{errors.title}</p>}
          </div>

          {fields.map((f) => (
            <div className="field" key={f.key}>
              <label className="field-label" htmlFor={f.key}>{f.label}</label>
              <input
                id={f.key}
                type={f.type}
                className="field-input"
                value={metadata[f.key] || ''}
                onChange={(e) => handleFieldChange(f.key, e.target.value)}
                aria-invalid={!!errors[f.key]}
              />
              {errors[f.key] && <p className="field-error">{errors[f.key]}</p>}
            </div>
          ))}

          <div className="field">
            <label className="field-label" htmlFor="description">Additional details (optional)</label>
            <textarea
              id="description"
              className="field-input new-request-textarea"
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          <div className="field">
            <label className="field-label" htmlFor="priority">Priority</label>
            <select
              id="priority"
              className="field-input"
              value={priority}
              onChange={(e) => setPriority(e.target.value)}
            >
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
            </select>
          </div>

          <button type="submit" className="submit-btn" disabled={loading}>
            {loading ? 'Submitting…' : 'Submit request'}
          </button>
        </form>

        <aside className="card new-request-preview">
          <p className="preview-eyebrow">Your manager</p>
          <p className="preview-manager">
            {profile === null ? 'Loading manager…' : profile.managerName ? `${profile.managerName} (ID: ${profile.managerId})` : 'No manager assigned — update your profile before submitting a request.'}
          </p>
          <p className="preview-eyebrow">What happens next</p>
          {requestType ? (
            <ol className="preview-steps">
              <li>Your request is created and a workflow instance starts.</li>
              {requestType === 'LEAVE_REQUEST' && (
                <>
                  <li>It's routed to your reporting manager for approval.</li>
                  <li>You're notified once they approve or reject it.</li>
                </>
              )}
              {requestType === 'BUDGET_REQUEST' && (
                <>
                  <li>Your manager reviews it first.</li>
                  <li>If approved, it moves to Finance for final approval.</li>
                  <li>You're notified of the final decision.</li>
                </>
              )}
            </ol>
          ) : (
            <p className="preview-empty">Pick a request type to see how it will be routed.</p>
          )}
        </aside>
      </div>
    </AppShell>
  )
}
