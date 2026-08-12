# Task 4 Report: Project detail and applicant conversations

## Scope

Completed Task 4 in the frontend only. The work preserves the existing project-detail, project API, and message API changes. No backend files, `ProjectList.vue`, or project documentation were modified.

## Project detail and owner actions

- Replaced the placeholder project detail with the reference-inspired detail layout.
- Owners can load applicant details, see each applicant's automatically supplied works, start a project conversation, and send a project order intent.
- The API helpers added for the existing owner flow are `getProjectEmployer`, `getApplications`, `createProjectOrderIntent`, and `startProjectConversation`.

## Artist application entry

- Added an `应征企划` entry visible only when the visitor is logged in, has `user.certStatus === 2`, is not the project owner, and the project is recruiting (`status === 0`).
- The Element Plus dialog requires an intro and a positive quote.
- Submission calls `POST /projects/{projectId}/applications` through the existing `applyProject` helper with exactly `{ intro, quote }`; it does not expose or send `workLink`.
- On success it shows a toast, closes, and resets the dialog. Applicant works continue to be supplied automatically to owners by the existing applicant-detail response.

## Visual checkpoints

1. **Shell:** Coordinator manually verified the gray detail-page background, 220px sidebar, desktop card shell, and mobile fallback.
2. **Body:** Coordinator manually verified the title/meta block, ordered image gallery, detail copy, and requirements grid.
3. **Card actions:** Coordinator manually verified the owner applicant cards, auto-displayed works, private-message action, and project-order action; owner/non-owner display was also manually verified before this continuation.

## Verification

- `npm.cmd run build` in `frontend/` completed successfully (Vite production build).
- `git diff --check` completed with no whitespace errors.
- Focused source review confirmed the application gate, both Element Plus validation rules, `applyProject(projectId, { intro, quote })`, and no `workLink` usage in `ProjectDetail.vue`.

The build reports the repository's existing Rollup PURE-annotation and chunk-size warnings; it exits successfully.
