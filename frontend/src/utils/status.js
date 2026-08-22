// Maps backend WorkflowStatus values to a display label + badge style.
const STATUS_MAP = {
  DRAFT: { label: 'Draft', className: 'badge-neutral' },
  SUBMITTED: { label: 'Submitted', className: 'badge-pending' },
  AI_PROCESSING: { label: 'AI Processing', className: 'badge-pending' },
  PENDING_MANAGER_APPROVAL: { label: 'Pending Manager', className: 'badge-pending' },
  PENDING_FINANCE_APPROVAL: { label: 'Pending Finance', className: 'badge-pending' },
  PENDING_HR_APPROVAL: { label: 'Pending HR', className: 'badge-pending' },
  PENDING_IT_ADMIN_APPROVAL: { label: 'Pending IT Admin', className: 'badge-pending' },
  PENDING_INFORMATION: { label: 'Needs Information', className: 'badge-info' },
  APPROVED: { label: 'Approved', className: 'badge-approved' },
  REJECTED: { label: 'Rejected', className: 'badge-rejected' },
  CANCELLED: { label: 'Cancelled', className: 'badge-neutral' },
  COMPLETED: { label: 'Completed', className: 'badge-approved' },
}

export function statusInfo(status) {
  return STATUS_MAP[status] || { label: status, className: 'badge-neutral' }
}

export function formatDate(iso) {
  if (!iso) return '\u2014'
  const d = new Date(iso)
  return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' }) +
    ' \u00b7 ' + d.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })
}

// Field definitions per request type, used to render the right inputs on
// the New Request form and to pretty-print metadata on the detail page.
export const REQUEST_TYPES = [
  {
    value: 'LEAVE_REQUEST',
    label: 'Leave Request',
    fields: [
      { key: 'startDate', label: 'Start date', type: 'date', required: true },
      { key: 'endDate', label: 'End date', type: 'date', required: true },
      { key: 'reason', label: 'Reason', type: 'text', required: true },
    ],
  },
  {
    value: 'BUDGET_REQUEST',
    label: 'Budget Request',
    fields: [
      { key: 'amount', label: 'Amount (\u20b9)', type: 'number', required: true },
      { key: 'purpose', label: 'Purpose', type: 'text', required: true },
      { key: 'department', label: 'Department', type: 'text', required: false },
    ],
  },
]

export function fieldsForType(type) {
  return REQUEST_TYPES.find((t) => t.value === type)?.fields || []
}
