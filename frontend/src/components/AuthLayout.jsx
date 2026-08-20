import './AuthLayout.css'

/**
 * Shared split-screen shell for the Login and Signup pages.
 * Left: brand panel with the "approval chain" signature graphic.
 * Right: the actual form, passed in as children.
 */
export default function AuthLayout({ eyebrow, title, subtitle, children }) {
  return (
    <div className="auth-shell">
      <aside className="brand-panel">
        <div className="brand-mark">
          <span className="brand-dot" />
          FlowGate
        </div>

        <div className="brand-copy">
          <p className="brand-eyebrow">Workflow automation, decided faster</p>
          <h1 className="brand-headline">
            Every request finds<br />the right approver.
          </h1>
          <p className="brand-sub">
            Invoices, leave, and purchase requests — routed, checked
            against policy, and signed off without the email trail.
          </p>
        </div>

        <ApprovalChain />

        <div className="brand-footnote">Secured with JWT · role-based access control</div>
      </aside>

      <main className="form-panel">
        <div className="form-card">
          <p className="form-eyebrow">{eyebrow}</p>
          <h2 className="form-title">{title}</h2>
          {subtitle && <p className="form-subtitle">{subtitle}</p>}
          {children}
        </div>
      </main>
    </div>
  )
}

/* Signature element: an animated Submit → Review → Approve chain,
   directly grounded in the product's own domain rather than a
   generic illustration. */
function ApprovalChain() {
  const stages = ['Submitted', 'Reviewing', 'Approved']
  return (
    <div className="chain" role="img" aria-label="Request flow: submitted, reviewing, approved">
      {stages.map((stage, i) => (
        <div className="chain-stage" key={stage}>
          <div className={`chain-node ${i === 2 ? 'chain-node--final' : ''}`}>
            {i === 2 ? (
              <svg width="14" height="14" viewBox="0 0 16 16" fill="none">
                <path d="M3 8.5L6.5 12L13 4.5" stroke="var(--bg)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            ) : (
              <span className="chain-node-dot" />
            )}
          </div>
          <span className="chain-label">{stage}</span>
          {i < stages.length - 1 && <div className="chain-line" style={{ animationDelay: `${i * 0.6}s` }} />}
        </div>
      ))}
    </div>
  )
}
