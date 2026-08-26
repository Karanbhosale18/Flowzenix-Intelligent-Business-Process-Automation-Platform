# Module 1 — Core Workflow Management: Completion Summary

This document maps the Module 1 checklist to what is implemented in the
repository, so a reviewer can trace each requirement to the code that
satisfies it. Phase 1 (authentication) is treated as **Task 1** — already
built — and the workflow engine, request/approval flow, and its tests are
**Module 1**.

## Scope decisions for this pass

Three scope choices were made explicitly before this work and shape what you
see below:

- **Cancel request = backend only.** The cancel capability is implemented
  end-to-end on the server (service authorization + engine transition +
  REST endpoint + tests). The corresponding frontend "Cancel" button and
  role-based menu gating were deliberately left out of this pass.
- **Tests = unit tests only.** The suite is pure JUnit 5 + Mockito with no
  Spring context and no database, so it runs with `mvn test` alone. HTTP-layer
  and security-filter coverage (which would need `@SpringBootTest` /
  Testcontainers because `requests.metadata` is JSONB) is out of scope.
- **User setup = enhanced signup UI.** Instead of a separate admin screen,
  the existing signup form now captures an optional role, department, and
  manager's user ID, which is enough to stand up the manager/finance/employee
  cast the demo workflows route between.

## 1.1 User & Role Management

| Requirement | Where it lives |
|---|---|
| Roles defined | `entity/ERole.java` — `EMPLOYEE`, `MANAGER`, `FINANCE`, `HR`, `IT_ADMIN`, `ADMIN` |
| Users carry roles + routing data | `entity/User.java` — `roles`, plus `department` and `managerId` |
| Role capture at onboarding | `frontend/src/pages/Signup.jsx` + `services/AuthService.js` (optional role / department / manager ID) |
| Role-based access enforced | `config/WebSecurityConfig.java` (URL rules) **and** service-layer ownership/admin checks (see 1.2 / 1.3) |

Access control is enforced **server-side**: the security config restricts
routes, and each service double-checks that the caller is the owner, the
assigned approver, or an `ADMIN` before returning or mutating anything. This
delivers the "users can access only what's relevant to their role" goal at
the API boundary regardless of what any client renders. (Frontend menu
gating by role was intentionally deferred per the scope note above.)

## 1.2 Request Management

| Requirement | Where it lives |
|---|---|
| Create a request | `service/RequestService.createRequest()` → `POST /api/requests` (`RequestController`); UI: `pages/NewRequest.jsx` |
| Select request type | `dto/CreateRequestDTO.requestType`; validated against `workflow_definitions` |
| Provide request details | `entity/Request.java` `metadata` (JSONB via `@JdbcTypeCode(SqlTypes.JSON)`) |
| Submit for approval | `WorkflowEngine.start()` kicks off the instance + first task |
| View own requests | `RequestService.listRequestsFor()` → `GET /api/requests`; UI: `pages/MyRequests.jsx` |
| View request detail | `RequestService.getRequestDetail()` → `GET /api/requests/{id}`; UI: `pages/RequestDetail.jsx` |
| Track status | `entity/WorkflowStatus.java` surfaced on every summary/detail DTO |
| **Cancel a request** *(new)* | `RequestService.cancelRequest()` → `POST /api/requests/{id}/cancel` → `WorkflowEngine.cancel()` |

Cancel authorization: only the request's owner or an `ADMIN` may cancel;
anyone else gets `403`. Cancelling a request that is already approved,
rejected, or cancelled returns `422` (it's terminal). A successful cancel
flips the instance to `CANCELLED` and closes its open task so it drops out of
the assignee's inbox.

## 1.3 Approval Management

| Requirement | Where it lives |
|---|---|
| View pending approvals | `service/TaskService.listMyTasks()` → `GET /api/tasks/my`; UI: `pages/Approvals.jsx` |
| Open a request to approve | `GET /api/requests/{id}` (same detail page serves requester and approver) |
| Approve | `TaskService.approve()` → `WorkflowEngine.decide(..., APPROVED, ...)` |
| Reject | `TaskService.reject()` → `decide(..., REJECTED, ...)` |
| Request more information | `TaskService.requestInformation()` → `decide(..., INFO_REQUESTED, ...)` |
| Add comments to a decision | `comment` recorded on the `WorkflowTask` and the `Approval` row |
| **Prevent duplicate approval** | `WorkflowEngine.decide()` throws `WorkflowException("already been resolved")` if the task is no longer `PENDING` |

## 1.4 Workflow Execution

The whole point of the module: `workflow/engine/WorkflowEngine.java` is the
single, **data-driven** place that mutates workflow state. It never branches
on request type — "leave" and "budget" exist only as rows seeded by
`config/WorkflowSeeder.java`.

| Requirement | Where it lives |
|---|---|
| Start workflow | `WorkflowEngine.start()` |
| Create workflow instance | `entity/WorkflowInstance.java`, persisted in `start()` |
| Create a task | `entity/WorkflowTask.java`, created per active step |
| Assign the task | `WorkflowEngine.resolveAssignee()` — `ROLE_MANAGER` → the creator's `managerId`; other roles → `UserRepository.findFirstByRole()` |
| Move to the next step | `WorkflowEngine.advanceToStep()` on an approval |
| Complete the workflow | `decide()` terminates as `APPROVED` when the last step is approved |
| Handle rejection | `decide()` with `REJECTED` ends the instance and spawns no next task |
| Handle invalid state | `WorkflowException` (mapped to `422`) for: no steps configured, employee has no `managerId`, no eligible approver, deciding a resolved task, cancelling a terminal instance |

## 1.5 Workflow History

| Requirement | Where it lives |
|---|---|
| Record workflow events | `entity/WorkflowHistory.java`; the engine writes an entry at submission, each assignment, each decision, and completion |
| Show a timeline | `getRequestDetail()` returns the ordered `history` + `steps`; UI renders it in `pages/RequestDetail.jsx` |

A full budget request therefore produces a six-entry trail: submitted →
assigned to Manager Review → Manager Review approved → assigned to Finance
Approval → Finance Approval approved → completed.

## 1.6 Module 1 Testing

Three Mockito test classes under `backend/src/test/java/com/example/authapp/`
cover every required scenario at the unit level:

| Checklist scenario | Test(s) |
|---|---|
| Leave workflow | `WorkflowEngineTest.startLeaveRequestAssignsManager`, `approveSingleStepCompletes` |
| Budget workflow | `WorkflowEngineTest.approveFirstOfTwoStepsAdvances`, `endToEndBudgetLifecycle` |
| Approval | `WorkflowEngineTest.approveSingleStepCompletes`, `TaskServiceTest.assigneeCanApprove` |
| Rejection | `WorkflowEngineTest.rejectEndsWorkflow`, `TaskServiceTest.assigneeCanReject` |
| Unauthorized access | `RequestServiceTest.strangerIsRefused` / `strangerCannotView`, `TaskServiceTest.nonAssigneeIsRefused` |
| Invalid request | `RequestServiceTest.unknownTypeThrowsAndStartsNothing`, `WorkflowEngineTest.startWithNoStepsThrows` / `startFailsWhenEmployeeHasNoManager` |
| End-to-end | `WorkflowEngineTest.endToEndBudgetLifecycle` (submit → manager → finance → `APPROVED`, six history entries) |

Also covered: duplicate-approval prevention (`decideOnResolvedTaskThrows`),
cancel behaviour (`cancelInFlightClosesTask`, `cancelTerminalThrows`), the
request-info park (`requestInformationParksWorkflow`), and the inbox/detail
mappings.

Run them:

```bash
cd backend
mvn test
```

## What changed in this pass (vs. the already-built Task 1 baseline)

- **Backend:** added `RequestService.cancelRequest()`, the
  `POST /api/requests/{id}/cancel` endpoint, and `WorkflowEngine.cancel()`.
- **Frontend:** enhanced `Signup.jsx` (+ `AuthService.js`, `AuthForm.css`)
  with optional role / department / manager-ID fields.
- **Tests:** added `WorkflowEngineTest`, `RequestServiceTest`,
  `TaskServiceTest`.
- **Docs:** updated `README.md` (cancel endpoint + test step, signup note,
  new "Automated tests" section, file tree) and added this summary.

## Verification notes

The unit tests were designed to need no database or Spring context, so
`mvn test` is sufficient — Postgres does **not** need to be running for them.
Compilation and the test run should be executed on a machine with **JDK 17**
and Maven (the sandbox this was authored in has neither, so the checks here
are review-based). For a manual end-to-end pass, follow section 6 of the
`README.md`.
