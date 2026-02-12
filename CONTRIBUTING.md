# Contributing

## Workflow
- Branch naming:
  - `feat/<short-description>`
  - `fix/<short-description>`
  - `chore/<short-description>`
- Keep one issue per PR.
- Link the issue in your PR description using `Closes #<issue>`.

## Local development
1. Install dependencies:
   - `npm install`
2. Start dev server:
   - `npm run dev`

## Production build
1. Run:
   - `npm run build`
2. Confirm the build completes with no errors.

## Verify GitHub Pages base path
1. Check `vite.config.js` for the configured `base` path.
2. Ensure it matches the repository Pages URL path.
3. Test asset loading and navigation on the deployed Pages site.

## Coding guidelines
- Keep changes within current scope (V1/V2 priorities only).
- Avoid adding new dependencies unless absolutely necessary.
- Keep user-facing text local and easy to update.
