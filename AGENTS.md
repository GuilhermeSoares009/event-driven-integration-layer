# AGENTS.md — Project Instructions (Spec-Kit aligned)

## Objective
- Deliver small, reviewable, safe changes.
- Prefer clarity over shortcuts and consistency over rewrites.
- If ambiguous, present two options and follow the simplest one.

## Working Style
- Work in atomic PRs and small diffs.
- Do not refactor outside scope without asking.
- Edit only what is necessary.
- If you need context, ask for specific file paths.
- Always signal current milestone when a milestone is finished, including the milestone number.

## Planning
- Before editing many files, present a short plan (max 5 steps) and wait for confirmation.
- If the plan exceeds 5 steps, split it.

## Spec-Driven Workflow (Spec Kit)
- Spec Kit assets live under `speckit/`.
- Use templates from `speckit/templates` when creating new features.
- Store feature docs in `specs/<id-feature>/` following the Spec Kit structure:

```text
specs/<id-feature>/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
└── tasks.md
```

- Keep specs and plans updated as the source of truth for code changes.
- Naming convention: features use `specs/###-short-slug/` and branch `###-short-slug` (example: `specs/001-webhook-inbox/`).

## Project Context
- Domain: Event-driven integration layer (webhooks -> inbox -> queue -> outbox)
- Stack: Laravel (PHP), Redis queues, relational DB
- Reliability: idempotency, dedupe, retries, DLQ, replay
- Observability: structured logs, basic metrics

## Commands (adjust to repo)
- Install deps: `composer install`
- Dev server: `php artisan serve`
- Queue worker: `php artisan queue:work`
- Tests: `php artisan test` (or `composer test` if configured)
- Lint: `composer lint` (if configured)

> Rule: if touching frontend, run at least lint + build. If touching critical rules, run relevant tests.

## Code Style
- Follow existing repository conventions (formatting, naming, structure).
- Prefer small functions, explicit names, and avoid magic.
- Avoid adding dependencies; if required, explain why and impact.

## Git and Commits
- Small commits and PRs.
- Commit messages: imperative + context (example: "Fix inbox dedupe on replay").
- No force-push unless requested.

## Design / UI / UX (when applicable)
- Prioritize clarity, accessibility (WCAG AA), and responsive behavior.
- Use semantic HTML and clear states (loading/empty/error).
- Mobile-first: nothing breaks at 360px.

## Quality and Security
- Never expose secrets. If detected, stop and report.
- Avoid destructive commands without warning.
- Prefer changes with easy rollback.

## Code Review Skills
- Generic CR skill: `skills/cr-general.md`
- Integration-layer CR skill: `skills/cr-integration-layer.md`

Use these checklists when reviewing changes or when asked to do CR.

## Stop and Ask
- Design conflicts or broad refactors.
- Unclear test/execution commands.

## Principles
- Execute > explain.
- Simplicity beats premature sophistication.
- Exploration and execution do not happen at the same time.
