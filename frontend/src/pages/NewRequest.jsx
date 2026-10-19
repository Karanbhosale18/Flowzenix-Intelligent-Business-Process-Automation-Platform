import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AppShell from '../components/AppShell.jsx'
import RequestService from '../services/RequestService.js'
import WorkflowService from '../services/WorkflowService.js'
import ProfileService from '../services/ProfileService.js'
import AiAssistantService from '../services/AiAssistantService.js'
import InvoiceService from '../services/InvoiceService.js'
import { REQUEST_TYPES, fieldsForType } from '../utils/status.js'
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

  // AI Assistant state
  const [showAi, setShowAi] = useState(false)
  const [aiPrompt, setAiPrompt] = useState('')
  const [aiLoading, setAiLoading] = useState(false)
  const [aiResult, setAiResult] = useState(null)
  const [aiError, setAiError] = useState('')

  // Invoice Intelligence state
  const [showInvoice, setShowInvoice] = useState(false)
  const [invoiceFile, setInvoiceFile] = useState(null)
  const [invoiceLoading, setInvoiceLoading] = useState(false)
  const [invoiceData, setInvoiceData] = useState(null)
  const [invoiceError, setInvoiceError] = useState('')

  useEffect(() => {
    WorkflowService.listActive().then((definitions) => {
      if (!definitions?.length) return
      setWorkflowTypes(definitions.map((definition) => ({
        value: definition.workflowType,
        label: definition.name,
        fields: fieldsForType(definition.workflowType),
      })))
    }).catch(() => {})
  }, [])

  useEffect(() => {
    ProfileService.get().then(setProfile).catch(() => setProfile({}))
  }, [])

  const fields = workflowTypes.find((type) => type.value === requestType)?.fields || []

  function handleTypeChange(value) {
    setRequestType(value)
    setMetadata({})
    setErrors({})
    if (value === 'BUDGET_REQUEST') {
      setShowInvoice(true)
    }
  }

  function handleFieldChange(key, value) {
    setMetadata((m) => ({ ...m, [key]: value }))
    if (errors[key]) setErrors((e) => ({ ...e, [key]: undefined }))
  }

  async function handleAnalyzeAi(e) {
    e.preventDefault()
    if (!aiPrompt.trim()) return
    setAiLoading(true)
    setAiError('')
    setAiResult(null)
    try {
      const res = await AiAssistantService.parseRequest(aiPrompt.trim())
      setAiResult(res.data)
    } catch (err) {
      setAiError(err.response?.data?.message || 'Failed to analyze request with AI assistant.')
    } finally {
      setAiLoading(false)
    }
  }

  function handleApplyAi() {
    if (!aiResult) return
    if (aiResult.requestType) {
      const matched = workflowTypes.find(t => t.value === aiResult.requestType || t.value.includes(aiResult.requestType))
      if (matched) setRequestType(matched.value)
    }
    if (aiResult.title) setTitle(aiResult.title)
    if (aiResult.description) setDescription(aiResult.description)
    if (aiResult.priority) setPriority(aiResult.priority)

    if (aiResult.extractedData) {
      setMetadata((prev) => ({
        ...prev,
        ...aiResult.extractedData
      }))
    }
    setAiResult(null)
    setShowAi(false)
  }

  async function handleAnalyzeInvoice(e) {
    e.preventDefault()
    if (!invoiceFile) return
    setInvoiceLoading(true)
    setInvoiceError('')
    setInvoiceData(null)
    try {
      const res = await InvoiceService.extract(invoiceFile)
      setInvoiceData(res.data)
    } catch (err) {
      setInvoiceError(err.response?.data?.message || 'Failed to extract invoice data.')
    } finally {
      setInvoiceLoading(false)
    }
  }

  function handleApplyInvoice() {
    if (!invoiceData) return
    setRequestType('BUDGET_REQUEST')
    if (invoiceData.vendorName) {
      setTitle(`Invoice Payment: ${invoiceData.vendorName} (${invoiceData.invoiceNumber || 'No #' })`)
    }
    if (invoiceData.totalAmount) {
      setMetadata((prev) => ({
        ...prev,
        amount: invoiceData.totalAmount,
        purpose: `Invoice #${invoiceData.invoiceNumber || 'N/A'} from ${invoiceData.vendorName || 'Vendor'}`
      }))
    }
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

      // If an invoice file was analyzed, upload and link it to the newly created request
      if (invoiceFile && invoiceData) {
        try {
          const formData = new FormData()
          formData.append('file', invoiceFile)
          formData.append('requestId', created.requestId)
          if (invoiceData.vendorName) formData.append('vendorName', invoiceData.vendorName)
          if (invoiceData.invoiceNumber) formData.append('invoiceNumber', invoiceData.invoiceNumber)
          if (invoiceData.invoiceDate) formData.append('invoiceDate', invoiceData.invoiceDate)
          if (invoiceData.subtotal) formData.append('subtotal', invoiceData.subtotal)
          if (invoiceData.taxAmount) formData.append('taxAmount', invoiceData.taxAmount)
          if (invoiceData.totalAmount) formData.append('totalAmount', invoiceData.totalAmount)
          await InvoiceService.upload(formData)
        } catch (invoiceUploadErr) {
          console.warn('Invoice attachment notice:', invoiceUploadErr)
        }
      }

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
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>

          {/* AI Request Assistant Collapsible Card */}
          <div className="feature-panel">
            <div className="feature-panel-header">
              <h3 className="feature-panel-title">
                <span>✨</span> AI Request Assistant
              </h3>
              <button
                type="button"
                className="btn btn-sm"
                onClick={() => setShowAi(!showAi)}
              >
                {showAi ? 'Hide Assistant' : 'Describe in Plain Language'}
              </button>
            </div>

            {showAi && (
              <div>
                <p className="field-hint" style={{ marginTop: 0 }}>
                  Enter a plain English description of what you need approved. The assistant will detect the workflow type and extract fields for you to review and confirm.
                </p>
                <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
                  <input
                    type="text"
                    className="field-input"
                    placeholder="e.g. I need approval for a department software purchase costing 25,000."
                    value={aiPrompt}
                    onChange={(e) => setAiPrompt(e.target.value)}
                  />
                  <button
                    type="button"
                    className="btn btn-primary"
                    disabled={aiLoading || !aiPrompt.trim()}
                    onClick={handleAnalyzeAi}
                  >
                    {aiLoading ? 'Analyzing…' : 'Analyze'}
                  </button>
                </div>

                {aiError && <div className="error-tag">{aiError}</div>}

                {aiResult && (
                  <div className="ai-preview-box">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <strong>Extracted Request Preview</strong>
                      <span className="feature-badge">
                        {aiResult.fallback ? 'Local NLP Rules' : 'Spring AI'}
                      </span>
                    </div>

                    <div className="ai-field-row">
                      <span className="ai-field-label">Detected Type</span>
                      <span className="ai-field-val">{aiResult.requestType}</span>
                    </div>
                    <div className="ai-field-row">
                      <span className="ai-field-label">Suggested Title</span>
                      <span className="ai-field-val">{aiResult.title}</span>
                    </div>
                    <div className="ai-field-row">
                      <span className="ai-field-label">Priority</span>
                      <span className="ai-field-val">{aiResult.priority}</span>
                    </div>

                    {aiResult.explanation && (
                      <p style={{ margin: '4px 0', fontSize: 12, color: 'var(--text-muted)' }}>
                        {aiResult.explanation}
                      </p>
                    )}

                    {aiResult.missingFields && aiResult.missingFields.length > 0 && (
                      <div className="warning-tag">
                        ⚠️ Missing fields to specify before submitting: {aiResult.missingFields.join(', ')}
                      </div>
                    )}

                    <button
                      type="button"
                      className="btn btn-primary"
                      style={{ marginTop: 8 }}
                      onClick={handleApplyAi}
                    >
                      Apply to Request Form
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Invoice Intelligence Panel */}
          <div className="feature-panel">
            <div className="feature-panel-header">
              <h3 className="feature-panel-title">
                <span>📄</span> Invoice Intelligence (OCR / Extraction)
              </h3>
              <button
                type="button"
                className="btn btn-sm"
                onClick={() => setShowInvoice(!showInvoice)}
              >
                {showInvoice ? 'Close' : 'Attach Invoice Document'}
              </button>
            </div>

            {showInvoice && (
              <div>
                <p className="field-hint" style={{ marginTop: 0 }}>
                  Upload an invoice document (PDF, PNG, JPEG up to 10MB) to automatically extract vendor, invoice number, line items, and totals.
                </p>
                <div style={{ display: 'flex', gap: 8, marginTop: 8, alignItems: 'center' }}>
                  <input
                    type="file"
                    accept=".pdf,.png,.jpg,.jpeg"
                    className="field-input"
                    onChange={(e) => setInvoiceFile(e.target.files[0] || null)}
                  />
                  <button
                    type="button"
                    className="btn btn-primary"
                    disabled={invoiceLoading || !invoiceFile}
                    onClick={handleAnalyzeInvoice}
                  >
                    {invoiceLoading ? 'Extracting…' : 'Extract Data'}
                  </button>
                </div>

                {invoiceError && <div className="error-tag">{invoiceError}</div>}

                {invoiceData && (
                  <div className="ai-preview-box">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <strong>Extracted Invoice Details</strong>
                      <span className="feature-badge">Validated</span>
                    </div>

                    <div className="ai-field-row">
                      <span className="ai-field-label">Vendor</span>
                      <span className="ai-field-val">{invoiceData.vendorName || '—'}</span>
                    </div>
                    <div className="ai-field-row">
                      <span className="ai-field-label">Invoice Number</span>
                      <span className="ai-field-val">{invoiceData.invoiceNumber || '—'}</span>
                    </div>
                    <div className="ai-field-row">
                      <span className="ai-field-label">Invoice Date</span>
                      <span className="ai-field-val">{invoiceData.invoiceDate || '—'}</span>
                    </div>
                    <div className="ai-field-row">
                      <span className="ai-field-label">Subtotal</span>
                      <span className="ai-field-val">{invoiceData.subtotal != null ? `$${invoiceData.subtotal}` : '—'}</span>
                    </div>
                    <div className="ai-field-row">
                      <span className="ai-field-label">Tax</span>
                      <span className="ai-field-val">{invoiceData.taxAmount != null ? `$${invoiceData.taxAmount}` : '—'}</span>
                    </div>
                    <div className="ai-field-row">
                      <span className="ai-field-label">Total Amount</span>
                      <span className="ai-field-val">{invoiceData.totalAmount != null ? `$${invoiceData.totalAmount}` : '—'}</span>
                    </div>

                    {invoiceData.warnings && invoiceData.warnings.map((w, i) => (
                      <div key={i} className="warning-tag">⚠️ {w}</div>
                    ))}

                    <button
                      type="button"
                      className="btn btn-primary"
                      style={{ marginTop: 8 }}
                      onClick={handleApplyInvoice}
                    >
                      Fill Form with Extracted Values
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Standard Submission Form */}
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
                placeholder="e.g. Software Purchase Approval"
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
        </div>

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
