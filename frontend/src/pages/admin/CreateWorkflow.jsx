import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import AppShell from '../../components/AppShell.jsx'
import WorkflowService from '../../services/WorkflowService.js'
import WorkflowCanvas from '../../components/workflow/WorkflowCanvas.jsx'
import StepConfigPanel from '../../components/workflow/StepConfigPanel.jsx'
import AuthService from '../../services/AuthService.js'
import './Workflows.css'

const blankStep = () => ({ name: 'New step', stepType: 'APPROVAL', assignedRole: 'ROLE_IT_ADMIN', required: true, configuration: '' })

export default function CreateWorkflow() {
  const navigate = useNavigate()
  const { id } = useParams()
  const editing = Boolean(id)
  const [form, setForm] = useState({ name: '', workflowType: '', description: '' })
  const [steps, setSteps] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [workflowId, setWorkflowId] = useState(id ? Number(id) : null)
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [loading, setLoading] = useState(editing)
  const user = AuthService.getCurrentUser()

  useEffect(() => {
    if (!editing) return
    WorkflowService.get(id).then((workflow) => {
      setForm({ name: workflow.name || '', workflowType: workflow.workflowType || '', description: workflow.description || '' })
      setSteps(workflow.steps || [])
    }).catch((err) => setError(err.response?.data?.message || 'Could not load workflow.')).finally(() => setLoading(false))
  }, [id, editing])

  const selected = useMemo(() => steps.find((step) => step.id === selectedId), [steps, selectedId])
  if (!(user?.roles || []).includes('ROLE_ADMIN')) return <AppShell title="Workflow builder"><div className="card"><p>You need an administrator account to manage workflows.</p></div></AppShell>

  function validateDefinition() {
    const errors = []
    if (!form.name.trim()) errors.push('Workflow name is required.')
    if (!/^[A-Za-z][A-Za-z0-9_]*$/.test(form.workflowType.trim())) errors.push('Workflow type must start with a letter and contain only letters, numbers, and underscores.')
    if (!steps.length) errors.push('Add at least one step before activation.')
    steps.forEach((step, index) => { if (!step.name?.trim()) errors.push(`Step ${index + 1} needs a name.`); if (!step.assignedRole) errors.push(`Step ${index + 1} needs an assigned role.`) })
    return errors
  }

  async function ensureDefinition() {
    const payload = { ...form, name: form.name.trim(), workflowType: form.workflowType.trim().toUpperCase() }
    if (workflowId) { await WorkflowService.update(workflowId, payload); return workflowId }
    const created = await WorkflowService.create({ ...payload, active: false })
    setWorkflowId(created.id)
    return created.id
  }

  async function addStep() {
    setError('')
    try {
      const idToUse = await ensureDefinition()
      const step = await WorkflowService.addStep(idToUse, blankStep())
      setSteps((items) => [...items, step])
      setSelectedId(step.id)
    } catch (err) { setError(err.response?.data?.message || 'Save the workflow details before adding a step.') }
  }

  async function saveStep() {
    if (!selected) return
    setError('')
    try {
      const step = await WorkflowService.updateStep(workflowId, selected.id, { ...selected, stepOrder: steps.indexOf(selected) + 1 })
      setSteps((items) => items.map((item) => item.id === step.id ? step : item))
    } catch (err) { setError(err.response?.data?.message || 'Could not save this step.') }
  }

  async function persistSelectedStep() {
    if (!selected || !workflowId) return
    const step = await WorkflowService.updateStep(workflowId, selected.id, { ...selected, stepOrder: steps.indexOf(selected) + 1 })
    setSteps((items) => items.map((item) => item.id === step.id ? step : item))
  }

  async function moveStep(index, direction) {
    const nextIndex = index + direction
    if (nextIndex < 0 || nextIndex >= steps.length || !workflowId) return
    const previous = steps
    const next = [...steps]; [next[index], next[nextIndex]] = [next[nextIndex], next[index]]
    setSteps(next)
    try { await WorkflowService.reorder(workflowId, next.map((step) => step.id)) } catch (err) { setSteps(previous); setError(err.response?.data?.message || 'Could not reorder steps.') }
  }

  async function deleteStep(step) {
    if (!window.confirm(`Remove “${step.name || 'this step'}”?`)) return
    setError('')
    try {
      if (step.id && workflowId) await WorkflowService.deleteStep(workflowId, step.id)
      const next = steps.filter((item) => item.id !== step.id)
      setSteps(next); setSelectedId(null)
    } catch (err) { setError(err.response?.data?.message || 'Could not remove this step.') }
  }

  async function saveAndActivate() {
    const validation = validateDefinition()
    if (validation.length) { setError(validation.join(' ')); return }
    setSaving(true); setError('')
    try { const idToUse = await ensureDefinition(); await persistSelectedStep(); await WorkflowService.activate(idToUse); navigate('/admin/workflows') }
    catch (err) { setError(err.response?.data?.message || 'Could not activate workflow.') }
    finally { setSaving(false) }
  }

  async function saveDraft() {
    setSaving(true); setError('')
    try { await ensureDefinition(); await persistSelectedStep(); navigate('/admin/workflows') }
    catch (err) { setError(err.response?.data?.message || 'Could not save workflow.') }
    finally { setSaving(false) }
  }

  return (
    <AppShell title={editing ? 'Edit workflow' : 'Create workflow'} actions={<button type="button" className="btn" onClick={() => navigate('/admin/workflows')}>Cancel</button>}>
      {error && <div className="banner banner--error" role="alert">{error}</div>}
      {loading ? <div className="card"><p className="empty-state">Loading workflow…</p></div> : <>
        <section className="card workflow-details-form">
          <div className="workflow-form-fields"><div className="field"><label className="field-label" htmlFor="workflow-name">Workflow name</label><input id="workflow-name" className="field-input" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="IT support request" /></div><div className="field"><label className="field-label" htmlFor="workflow-type">Workflow type</label><input id="workflow-type" className="field-input" value={form.workflowType} onChange={(e) => setForm({ ...form, workflowType: e.target.value.toUpperCase() })} placeholder="IT_SUPPORT_REQUEST" /><p className="field-hint">This unique key appears on the request form.</p></div></div>
          <div className="field"><label className="field-label" htmlFor="workflow-description">Description <span className="field-optional">(optional)</span></label><textarea id="workflow-description" className="field-input" rows={2} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></div>
        </section>
        <div className="workflow-builder-grid"><WorkflowCanvas steps={steps} selectedId={selectedId} onSelect={(step) => setSelectedId(step.id)} onMove={moveStep} onDelete={deleteStep} onAdd={addStep} /><StepConfigPanel step={selected} saving={false} onChange={(step) => setSteps((items) => items.map((item) => item.id === step.id ? step : item))} onSave={saveStep} onCancel={() => setSelectedId(null)} onDelete={deleteStep} /></div>
        <div className="workflow-footer"><p className="field-hint">Activation checks that the workflow has ordered steps and valid assignees.</p><div><button type="button" className="btn" disabled={saving} onClick={saveDraft}>Save draft</button><button type="button" className="btn btn-primary" disabled={saving} onClick={saveAndActivate}>{saving ? 'Saving…' : 'Save & activate'}</button></div></div>
      </>}
    </AppShell>
  )
}
