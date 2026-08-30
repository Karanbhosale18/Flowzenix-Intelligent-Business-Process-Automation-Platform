import WorkflowStep from './WorkflowStep.jsx'

export default function WorkflowCanvas({ steps, selectedId, onSelect, onMove, onDelete, onAdd }) {
  return (
    <section className="card workflow-canvas">
      <div className="workflow-canvas-header">
        <div><p className="section-title">Workflow canvas</p><p className="field-hint">Steps run from top to bottom.</p></div>
        <button type="button" className="btn btn-primary" onClick={onAdd}>Add step</button>
      </div>
      {steps.length === 0 ? (
        <div className="workflow-empty"><span>＋</span><p>Your workflow has no steps yet.</p><button type="button" className="btn" onClick={onAdd}>Add the first step</button></div>
      ) : (
        <div className="workflow-step-list">
          {steps.map((step, index) => (
            <WorkflowStep key={step.id || `new-${index}`} step={step} index={index} total={steps.length} selected={step.id === selectedId} onSelect={onSelect} onMove={onMove} onDelete={onDelete} />
          ))}
        </div>
      )}
    </section>
  )
}
