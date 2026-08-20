# Task 3 Report: Publish wizard requirements

## Scope

Implemented Task 3 exclusively in `frontend/src/views/dashboard/ProjectNew.vue`. No backend, API helper, documentation, `ProjectList.vue`, or project-hall UI files were changed.

## Changes

- Added the required nested form state:
  ```js
  requirements: { size: '', colorMode: 'RGB', formats: [], publishPermission: '' }
  ```
- Added an Element Plus parameter editor before the demand description, styled to match the stall publishing dialog:
  - size input
  - RGB/CMYK color-mode select
  - multi-select delivery formats
  - publish-permission radio group
- Prevented advancing past the demand-details step unless title and all four requirement values are present.
- Added all requirements to the preview and sent them unchanged in the create-project request as:
  ```js
  requirements: { size, colorMode, formats, publishPermission }
  ```
- Preserved ordered multi-image uploads by rebuilding `form.images` from Element Plus's displayed file list after changes and upload completion. The first submitted image URL remains the cover.

## Verification

- `npm.cmd run build` from `frontend/` — passed (Vite production build completed successfully).
- `git diff --check` — passed.

The build emits pre-existing Rollup warnings for two `@vueuse/core` PURE annotations and the application chunk-size threshold; neither stops the build.

## Commit

Committed as `feat: collect project requirements in publish wizard`.
