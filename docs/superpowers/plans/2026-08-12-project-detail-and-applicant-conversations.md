# Project Detail and Applicant Conversations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the project detail page, structured project requirements, owner-only applicant work cards, and owner-initiated project conversations without implementing real project orders.

**Architecture:** Add a JSON requirements column to the existing `project` aggregate and expose it through the existing project APIs. Return a purpose-built application detail DTO for owner-only applicant cards. Add a dedicated owner-initiated conversation endpoint that creates the existing normalized project conversation; the message service enforces who may initiate and who may reply.

**Tech Stack:** Vue 3, Element Plus, Vue Router, Pinia, Spring Boot 3, MyBatis-Plus, JUnit 5, Mockito, MySQL 8.

## Global Constraints

- Work only in `feature/project-detail`, forked from `feature/f9-messaging`; do not modify `main` or merge branches.
- Do not change the existing project-hall card layout; its existing `firstImage(project.images)` remains the only cover selection.
- Compare UI against `http://127.0.0.1:8767/site/projects_11631031.html` at three checkpoints: structural layout, information body, applicant cards.
- Exclude flow diagram, related character card, share, ratings, completion rate, feedback interval, project process, payment, actual project order creation, and order lifecycle.
- Requirements keys are exactly `size`, `colorMode`, `formats`, and `publishPermission`; subject style remains `project.subject`.
- Applicant project chat: only the employer may initiate for status `0` or `1`; after a conversation exists both employer and that applicant may send; a withdrawn application cannot create a new conversation.
- `POST /projects/{projectId}/order-intents/{painterId}` is a no-side-effect placeholder; it must not create `order_info` or mutate application/project state.
- A project may have only one selected applicant now; preserve application-based data so future multi-painter support only relaxes the single-selection rule.

---

### Task 1: Persist structured project requirements

**Files:** `backend/sql/migrations/20260812_project_detail.sql`, `backend/src/main/java/com/huiyue/dto/ProjectRequirements.java`, `Project.java`, `ProjectRequest.java`, `ProjectServiceImpl.java`, `backend/sql/init.sql`, `ProjectServiceTest.java`.

- [ ] Write a failing `ProjectServiceTest` asserting a request containing `ProjectRequirements("3000×4000 px", "RGB", List.of("PNG", "PSD"), "可自由发布")` is serialized before `projectMapper.insert`.
- [ ] Run `mvn.cmd -Dtest=ProjectServiceTest test` and verify it fails because requirements are not modeled.
- [ ] Add `requirements TEXT` migration/schema field, the DTO, request/entity fields, and Jackson JSON serialization in project creation.
- [ ] Run the focused test then `mvn.cmd test`.
- [ ] Commit as `feat: store project requirements`.

### Task 2: Owner-only applicant card data and controlled actions

**Files:** `ApplicationDetail.java`, `ApplicationController.java`, `ApplicationService.java`, `ApplicationServiceImpl.java`, `MessageService.java`, `MessageServiceImpl.java`, `MessageController.java`, `ApplicationServiceTest.java`, `MessageServiceTest.java`.

- [ ] Write failing tests proving a non-employer cannot list applicants and an employer can start a chat with a pending applicant.
- [ ] Run `mvn.cmd -Dtest=ApplicationServiceTest,MessageServiceTest test` and verify expected failures.
- [ ] Return application plus painter profile and four recent works only to the employer; reject all other callers.
- [ ] Add the owner-only conversation endpoint and asymmetric initiation rules; retain replies only after an existing conversation is found.
- [ ] Add the authorized no-side-effect order-intent placeholder endpoint returning `订单功能开发中` without importing `OrderService`.
- [ ] Run focused tests and `mvn.cmd test`.
- [ ] Commit as `feat: add owner initiated applicant conversations`.

### Task 3: Publish wizard requirements

**Files:** `frontend/src/views/dashboard/ProjectNew.vue`, optionally `frontend/src/api/project.js`.

- [ ] Add `requirements: { size: '', colorMode: 'RGB', formats: [], publishPermission: '' }` to the form and API payload.
- [ ] Add required controls before requirement description; validate all four values before moving forward.
- [ ] Reuse the Element Plus parameter style from stall publishing; retain ordered multi-upload and do not modify `ProjectList.vue`.
- [ ] Run `npm.cmd run build` and commit as `feat: collect project requirements in publish wizard`.

### Task 4: Project detail UI and applicant cards

**Files:** `frontend/src/views/projects/ProjectDetail.vue`, `frontend/src/api/project.js`, `frontend/src/api/message.js`.

- [ ] Load project and compute ownership from `userStore.userInfo.id === project.employerId`; load applicants only for owners.
- [ ] Checkpoint 1: build reference-inspired grey page, 220px owner/budget sidebar, white main card, 1080px desktop shell, mobile single-column fallback.
- [ ] Checkpoint 2: add title/meta, ordered responsive image gallery, two-column requirement grid, and description. Do not render excluded reference-page sections.
- [ ] Checkpoint 3: show owner-only applicant cards with profile, intro, quote, four work thumbnails, right-aligned `私信` and `发送企划订单` buttons. Route an opened chat to messages with `withUserId` and `projectId`.
- [ ] At each checkpoint inspect the reference and local page at equivalent viewport; correct in-scope differences before continuing.
- [ ] Run `npm.cmd run build`, then manually check owner versus non-owner visibility.
- [ ] Commit as `feat: build project detail applicant cards`.

### Task 5: Documentation and verification

**Files:** `docs/沟通与消息设计.md`, `docs/接口文档.md`, `docs/项目交接说明.md`.

- [ ] Describe owner-only chat creation, reply rules, order-intent placeholder, and future multi-painter extension.
- [ ] List every interface not manually tested, including multi-account live WebSocket, selected-applicant replacement, and future multi-painter order creation.
- [ ] Run `mvn.cmd test`, `npm.cmd run build`, and `git diff --check`.
- [ ] Commit as `docs: record project detail communication scope`.
