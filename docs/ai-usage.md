# AI usage record

Codex assisted with the initial implementation on 2026-10-23, following the user-provided assignment PDF and project AGENTS.md. The user requested implementation after reviewing the generated project guidance.

Assisted work: architecture, Java library and HTTP API, metadata contract, Vue interface and styling, tests, build configuration, and documentation. Decisions included a single modular Java service, RE2/J matching, code-point substring indices, generic Vue parameter rendering, bounded request/output sizes, and a production JAR embedding the frontend.

This document is a summary, not a raw transcript. The complete chat/tool transcript is not exported into this repository. Export the Codex conversation if raw logs are desired for submission, reviewing it for secrets and unrelated personal information first. No transcript has been fabricated and no submission has been sent externally.

Verification results are recorded in the implementing agent's final response and in generated test reports under ignored build directories. Re-run the README commands to reproduce them.

Verified on 2026-10-23: documented clean npm install/build script; production Vue build with strict type checks; 4 Vue unit/component tests; 6 Java core tests; 4 real HTTP integration tests; 1 Playwright workflow covering desktop and mobile. An initial browser run completed the workflow but timed out while closing the browser; the subsequent run with a larger test timeout passed in 5.7 seconds. Desktop and mobile screenshots were visually inspected. Git was initialized locally; no remote was configured.
