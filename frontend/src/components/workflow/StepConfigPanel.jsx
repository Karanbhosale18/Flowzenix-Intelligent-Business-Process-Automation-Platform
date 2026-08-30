const ROLES = ['ROLE_MANAGER', 'ROLE_FINANCE', 'ROLE_HR', 'ROLE_IT_ADMIN', 'ROLE_ADMIN', 'ROLE_EMPLOYEE']
const TYPES = ['APPROVAL', 'ACTION', 'NOTIFICATION']

export default function StepConfigPanel({ step, onChange, onSave, onCancel, onDelete, saving }) {
  if (!step) return <aside className="card step-config-empty"><p className="section-title">Configure a step</p><p className="field-hint">Select a step on the canvas to edit its routing and behaviour.</p></aside>
  const set = (key, value) => onChange({ ...step, [key]: value })
  return (
    <aside className="card step-config-panel">
      <div className="step-config-heading"><div><p className="section-title">Configure step</p><p className="field-hint">Define who handles this stage.</p></div><button type="button" className="btn" onClick={onCancel}>Close</button></div>
      <div className="field"><label className="field-label" htmlFor="step-name">Step name</label><input id="step-name" className="field-input" value={step.name || ''} onChange={(e) => set('name', e.target.value)} placeholder="e.g. IT triage" /></div>
      <div className="field"><label className="field-label" htmlFor="step-type">Step type</label><select id="step-type" className="field-input" value={step.stepType || 'APPROVAL'} onChange={(e) => set('stepType', e.target.value)}>{TYPES.map((type) => <option key={type}>{type}</option>)}</select></div>
      <div className="field"><label className="field-label" htmlFor="assigned-role">Assigned role</label><select id="assigned-role" className="field-input" value={step.assignedRole || ''} onChange={(e) => set('assignedRole', e.target.value)}><option value="">Choose a role…</option>{ROLES.map((role) => <option key={role} value={role}>{role.replace('ROLE_', '').replaceAll('_', ' ')}</option>)}</select></div>
      <label className="workflow-check"><input type="checkbox" checked={step.required !== false} onChange={(e) => set('required', e.target.checked)} /> Required step</label>
      <div className="field"><label className="field-label" htmlFor="step-config">Configuration <span className="field-optional">(optional JSON)</span></label><textarea id="step-config" className="field-input workflow-config" rows={4} value={step.configuration || ''} onChange={(e) => set('configuration', e.target.value)} placeholder={'{ "queue": "it-support" }'} /></div>
      <div className="step-config-actions"><button type="button" className="btn btn-primary" disabled={saving} onClick={onSave}>{saving ? 'Saving…' : 'Save step'}</button><button type="button" className="btn btn-danger" onClick={() => onDelete(step)}>Delete</button></div>
    </aside>
  )
}
