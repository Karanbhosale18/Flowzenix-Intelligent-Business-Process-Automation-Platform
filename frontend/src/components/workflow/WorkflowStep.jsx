export default function WorkflowStep({ step, index, total, selected, onSelect, onMove, onDelete }) {
  const role = (step.assignedRole || '').replace('ROLE_', '').replaceAll('_', ' ')
  return (
    <div className={`workflow-step${selected ? ' workflow-step--selected' : ''}`}>
      <button type="button" className="workflow-step-main" onClick={() => onSelect(step)}>
        <span className="workflow-step-order">{index + 1}</span>
        <span className="workflow-step-copy">
          <strong>{step.name || 'Untitled step'}</strong>
          <small>{step.stepType || 'STEP'} · {role || 'Unassigned'} · {step.required === false ? 'Optional' : 'Required'}</small>
        </span>
      </button>
      <div className="workflow-step-actions">
        <button type="button" aria-label="Move step up" disabled={index === 0} onClick={() => onMove(index, -1)}>↑</button>
        <button type="button" aria-label="Move step down" disabled={index === total - 1} onClick={() => onMove(index, 1)}>↓</button>
        <button type="button" aria-label="Delete step" className="workflow-step-delete" onClick={() => onDelete(step)}>×</button>
      </div>
    </div>
  )
}
