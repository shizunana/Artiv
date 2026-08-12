# Task 2 Report: Owner-only applicant card data and controlled actions

## Delivered

- `GET /api/v1/projects/{projectId}/applications` now requires the project employer and returns `ApplicationDetail` records containing the application, painter profile, and no more than four recent non-deleted works.
- `POST /api/v1/projects/{projectId}/conversations/{painterId}` lets only the employer start an idempotent project conversation for a pending (`0`) or selected (`1`) applicant.
- Project-scoped private messages require an existing employer-started project conversation. Both participants may reply after it exists; applicants cannot open one themselves.
- Selecting an application no longer automatically opens a project conversation.
- `POST /api/v1/projects/{projectId}/order-intents/{painterId}` authorizes the employer and active applicant but has no side effects and returns `订单功能开发中`. It does not import or call `OrderService`.

## TDD evidence

- Red: `mvn.cmd '-Dtest=ApplicationServiceTest,MessageServiceTest' test` failed because `listApplicantDetails` and `startProjectConversation` did not exist.
- Green: the same focused suite passed with 10 tests after implementation.
- Full verification: `mvn.cmd test` passed with 13 tests, 0 failures, and `git diff --check` passed.

## Scope

- Backend-only changes; frontend and project documentation were not modified.
- No project order, `order_info`, application status, or project state is changed by the order-intent endpoint.

## Round 1 remediation

- Removed the public `ApplicationService.listByProject` bypass; applicant data is retrieved only after the owner check in `listApplicantDetails`.
- Removed `createProjectConversation` from the public service contract. The retained private compatibility helper is unreachable from callers, and selection continues to create no conversation.
- Added a nullable `starter_key` with `uk_project_conversation_starter` in both the fresh schema and an upgrade migration. Only a project starter marker receives that identity, leaving normal replies unconstrained. Initiation now inserts once and, on a duplicate-key race, fetches and returns the single persisted starter marker.
- Added tests for selected, non-owner, and withdrawn initiation outcomes; duplicate-key idempotency; no public applicant-list or legacy starter API; and non-owner order intent with no application, project, or message mutation.

## Round 2 remediation

- Corrected the fresh schema: `starter_key` and `uk_project_conversation_starter` now belong exclusively to `message`; the accidental order table index was removed.
- The upgrade migration adds `starter_key`, backfills every active `APPLICATION_CONVERSATION_STARTED` marker deterministically as `project_id:conversation_key:APPLICATION_CONVERSATION_STARTED`, then adds the unique key.
- Starter lookup recognizes both backfilled keyed rows and legacy null-key starter rows, preventing duplicate initiation and retaining replies during an upgrade rollout.
- Added authorized pending and selected order-intent tests for the exact `订单功能开发中` response and zero message/project/application mutation, plus withdrawn rejection and legacy-starter initiation/reply coverage.
