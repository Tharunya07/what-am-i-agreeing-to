# V2 Roadmap

## V2.1 - Input Quality and Parsing Stability
- Improve robustness for pasted policy/legal text with mixed formatting.
  - Acceptance criteria: Handles long-form text without truncation or UI freeze.
- Add clearer parse-state feedback for success/failure states.
  - Acceptance criteria: User sees explicit state (processing, success, error) for each run.
- Expand sample coverage for common agreement types.
  - Acceptance criteria: At least three representative sample inputs validate expected parsing output.

## V2.2 - Explainability and Output UX
- Improve summary readability with stronger structure and plain-language sections.
  - Acceptance criteria: Output consistently separates key obligations, risks, and unknowns.
- Add confidence or uncertainty cues where extraction is ambiguous.
  - Acceptance criteria: Ambiguous statements are marked and not presented as definitive facts.
- Support easy copy/export of generated output.
  - Acceptance criteria: User can copy all output in one action without formatting loss.

## V2.3 - Reliability, QA, and Deployment Confidence
- Add lightweight regression checks for representative inputs.
  - Acceptance criteria: CI validates baseline outputs for selected sample agreements.
- Strengthen client-side error handling and user-safe fallbacks.
  - Acceptance criteria: Failures show actionable messaging with no broken UI state.
- Finalize release readiness checklist for Pages deployment.
  - Acceptance criteria: Build, deploy, and smoke test steps are documented and repeatable.
