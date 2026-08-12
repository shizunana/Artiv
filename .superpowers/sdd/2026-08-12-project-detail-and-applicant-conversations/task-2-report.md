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
