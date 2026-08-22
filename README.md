# Phase 2 — Workflow Engine Core

Builds on the Phase 1 auth module already in this repo. This module adds the
generic, reusable workflow engine, request submission, and the
manager/finance approval flow — the demo scenario from the spec (leave
request + budget request) now runs end to end.

## 1. What is being implemented

- Expanded roles: `EMPLOYEE`, `MANAGER`, `FINANCE`, `HR`, `IT_ADMIN`, `ADMIN`
  (replacing the placeholder `USER`/`ADMIN` from Phase 1), plus `department`
  and `managerId` on `User` so the engine knows who a request routes to.
- The workflow domain model: `WorkflowDefinition`, `WorkflowStep`,
  `WorkflowInstance`, `WorkflowTask`, `Request`, `Approval`,
  `WorkflowHistory`.
- `WorkflowEngine` — one service that starts an instance, resolves who a
  step routes to, and advances/ends the workflow on a decision. It has
  **zero knowledge of "leave" or "budget"** — those exist purely as rows in
  `workflow_definitions` / `workflow_steps`.
- `WorkflowSeeder` — inserts the two definitions from the spec
  (`LEAVE_REQUEST`: Employee → Manager; `BUDGET_REQUEST`: Employee → Manager
  → Finance) on startup, idempotently.
- REST layer: create/list/view requests, list/approve/reject tasks.
- Frontend: New Request form, My Requests table, Request Detail page with a
  workflow timeline, and a Pending Approvals inbox.

## 2. Why it's required

The spec is explicit that this must **not** become a pile of
`if (requestType.equals("LEAVE")) { ... }` branches. `WorkflowEngine` is the
answer to that: adding a third or fourth request type (IT support, WFH,
expense reimbursement) means inserting a new `WorkflowDefinition` +
`WorkflowStep` rows — no new Java code, no new branches. This is also the
piece every later phase depends on: AI classification (Phase 5) just needs
to produce a `requestType` + `metadata` map that gets handed to the same
`RequestService.createRequest()`, and the admin workflow builder (Phase 6)
is just a UI that writes the same definition/step rows `WorkflowSeeder`
writes today.

## 3. File / folder structure

```
backend/src/main/java/com/example/authapp/
├── entity/
│   ├── ERole.java                  (expanded: EMPLOYEE/MANAGER/FINANCE/HR/IT_ADMIN/ADMIN)
│   ├── User.java                   (+ department, managerId)
│   ├── WorkflowStatus.java         (new)
│   ├── StepType.java               (new)
│   ├── TaskStatus.java             (new)
│   ├── ApprovalDecision.java       (new)
│   ├── Priority.java               (new)
│   ├── WorkflowDefinition.java     (new)
│   ├── WorkflowStep.java           (new)
│   ├── WorkflowInstance.java       (new)
│   ├── WorkflowTask.java           (new)
│   ├── Request.java                (new — JSONB metadata)
│   ├── Approval.java               (new)
│   └── WorkflowHistory.java        (new)
├── repository/
│   ├── UserRepository.java         (+ findFirstByRole)
│   ├── WorkflowDefinitionRepository.java
│   ├── WorkflowInstanceRepository.java
│   ├── WorkflowTaskRepository.java
│   ├── RequestRepository.java
│   ├── ApprovalRepository.java
│   └── WorkflowHistoryRepository.java
├── dto/
│   ├── CreateRequestDTO.java
│   ├── RequestSummaryDTO.java
│   ├── RequestDetailDTO.java
│   ├── TaskSummaryDTO.java
│   └── TaskDecisionDTO.java
├── workflow/engine/
│   └── WorkflowEngine.java         (the engine itself)
├── service/
│   ├── RequestService.java
│   └── TaskService.java
├── controller/
│   ├── RequestController.java
│   └── TaskController.java
├── exception/
│   ├── WorkflowException.java      (new — 422)
│   └── GlobalExceptionHandler.java (+ WorkflowException, AccessDeniedException handlers)
└── config/
    ├── WebSecurityConfig.java      (role rules updated for new ERole set)
    └── WorkflowSeeder.java         (new — seeds the two demo workflows)

frontend/src/
├── utils/status.js                 (new — status badges, request-type field defs)
├── components/AppShell.jsx/.css    (new — sidebar layout used by all authed pages)
├── services/RequestService.js      (new)
├── services/TaskService.js         (new)
└── pages/
    ├── Dashboard.jsx/.css          (rewritten — real stats + quick links)
    ├── NewRequest.jsx/.css         (new)
    ├── MyRequests.jsx/.css         (new)
    ├── RequestDetail.jsx/.css      (new)
    └── Approvals.jsx/.css          (new)
```

## 4. Database changes

New tables (auto-created by `spring.jpa.hibernate.ddl-auto=update` on
startup — no manual migration needed for this stage of the project):

```
workflow_definitions   (id, name, description, workflow_type unique, active, created_at)
workflow_steps         (id, workflow_definition_id, step_order, name, step_type,
                         assigned_role, required, configuration)
workflow_instances     (id, workflow_definition_id, created_by, current_step,
                         status, created_at, updated_at, completed_at)
workflow_tasks         (id, workflow_instance_id, step_id, assigned_to, status,
                         comment, due_date, created_at, completed_at)
requests               (id, workflow_instance_id unique, request_type, title,
                         description, priority, metadata jsonb, created_at)
approvals               (id, workflow_instance_id, approver_id, decision, comment, created_at)
workflow_history        (id, workflow_instance_id, action, performed_by,
                          old_status, new_status, comment, created_at)
```

`users` gains two columns: `department` (varchar) and `manager_id` (bigint,
references another user's id — set at signup or via a future admin
endpoint). `requests.metadata` is `jsonb`, mapped via Hibernate 6's native
`@JdbcTypeCode(SqlTypes.JSON)` — no extra dependency needed.

**If you're upgrading an existing Phase 1 database**, the old `ROLE_USER`
enum value no longer exists. Either drop and recreate `authapp_db` (fine
for a dev/demo database — see `backend/setup.sql`), or manually update any
existing `user_roles` rows from `ROLE_USER` to `ROLE_EMPLOYEE`.

## 5. API endpoints

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/requests` | Any authenticated user | Body: `CreateRequestDTO`. Starts a workflow instance. |
| GET | `/api/requests` | Any authenticated user | Own requests; all requests if caller is `ADMIN`. |
| GET | `/api/requests/{id}` | Owner, assigned approver, or `ADMIN` | Full detail incl. timeline. 403 otherwise. |
| GET | `/api/tasks/my` | Any authenticated user | Pending tasks assigned to the caller. |
| POST | `/api/tasks/{id}/approve` | Assignee or `ADMIN` | Body (optional): `{ "comment": "..." }` |
| POST | `/api/tasks/{id}/reject` | Assignee or `ADMIN` | Same body shape. |
| POST | `/api/tasks/{id}/request-information` | Assignee or `ADMIN` | Sets status to `PENDING_INFORMATION`. |

**Create a leave request:**
```json
POST /api/requests
{
  "requestType": "LEAVE_REQUEST",
  "title": "Leave for family function",
  "priority": "MEDIUM",
  "metadata": { "startDate": "2026-09-10", "endDate": "2026-09-12", "reason": "Family function" }
}
```

**Create a budget request:**
```json
POST /api/requests
{
  "requestType": "BUDGET_REQUEST",
  "title": "Developer conference",
  "priority": "HIGH",
  "metadata": { "amount": "50000", "purpose": "Developer Conference" }
}
```

Errors surface as clean JSON via `GlobalExceptionHandler`: `422` for
workflow problems (unknown request type, no manager set, no Finance user
exists yet), `403` for access-denied, `400` for validation failures.

## 6. How to test it

1. **Seed at least one Manager and one Finance user**, since the engine
   needs someone to route to:
   ```bash
   # Manager
   curl -X POST http://localhost:8080/api/auth/signup -H "Content-Type: application/json" \
     -d '{"username":"priya.manager","email":"priya@co.com","password":"password123","role":["manager"]}'

   # Finance
   curl -X POST http://localhost:8080/api/auth/signup -H "Content-Type: application/json" \
     -d '{"username":"finance.team","email":"finance@co.com","password":"password123","role":["finance"]}'
   ```
2. **Sign up an employee with `managerId` set to the manager's id** (log in
   as the manager first to get their `id` from the login response):
   ```bash
   curl -X POST http://localhost:8080/api/auth/signup -H "Content-Type: application/json" \
     -d '{"username":"karan","email":"karan@co.com","password":"password123","managerId":1}'
   ```
3. **Log in as `karan`**, submit a leave request via `POST /api/requests`
   (example above) — it should land in `PENDING_MANAGER_APPROVAL`.
4. **Log in as `priya.manager`**, `GET /api/tasks/my` — the task should be
   there. `POST /api/tasks/{id}/approve` — status moves to `APPROVED` (leave
   has only one step) or `PENDING_FINANCE_APPROVAL` (budget has two).
5. **Check the trail**: `GET /api/requests/{id}` as `karan` shows the full
   `history` array and `steps` timeline.
6. Try submitting a request with an employee who has no `managerId` set —
   you should get a `422` with a clear message instead of a stack trace.

## 7. How the frontend connects to it

- `RequestService.js` / `TaskService.js` wrap the endpoints above using the
  same `api.js` axios instance from Phase 1 (JWT already attached
  automatically).
- `NewRequest.jsx` renders different fields per request type (see
  `utils/status.js` → `REQUEST_TYPES`) and posts to `/api/requests`.
- `MyRequests.jsx` lists `GET /api/requests` in a table with status badges.
- `RequestDetail.jsx` fetches `GET /api/requests/{id}`; if the response's
  `myPendingTaskId` is set, it shows Approve/Reject/Request-info buttons
  right there — the same page works for the requester and the approver.
- `Approvals.jsx` is the "Pending Approvals" inbox from `GET /api/tasks/my`,
  with one-click approve/reject.
- `Dashboard.jsx` now pulls both endpoints to show real counts instead of
  the Phase 1 placeholder.
- All of these render inside the new `AppShell` component (sidebar nav +
  logout), which every authenticated page after login now uses instead of
  the bare Phase 1 dashboard shell.

## What's intentionally out of scope for this module

- **AI classification** (Phase 5) — `metadata` is filled in by hand on the
  New Request form for now; swapping in an `AIWorkflowService` later only
  touches `NewRequest.jsx` and, on the backend, whatever populates
  `CreateRequestDTO` before it reaches `RequestService` — `WorkflowEngine`
  doesn't change.
- **Admin workflow builder UI** (Phase 6) — `WorkflowSeeder` stands in for
  it; the schema is already shaped for a builder to write directly to it.
- **Notifications** (Phase 3/6) — decisions are recorded and visible on the
  detail page, but no email/in-app notification fires yet.
- **Conditional routing** (e.g. "route to a VP if amount > ₹1,00,000") —
  `WorkflowStep.configuration` is a free-text column reserved for this;
  `WorkflowEngine` doesn't evaluate conditions yet.
