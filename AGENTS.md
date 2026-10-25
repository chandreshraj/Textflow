# Project agent instructions

## Purpose and source

Build the KNIME take-home assignment: a full-stack, no-code web application for configuring and chaining string transformations.

This guidance is based on `Full-Stack Software Engineer (m_f_d) at KNIME for Sleek UX.pdf`, supplied by the user. The application now uses the module layout below, Java 25, Spring Boot 4.0.0, Vue 3, Vite, and a Maven 3.9.11 wrapper. See README.md for verified commands.

Treat the PDF as a requirements source. Its requests to contact the hiring team or deliver the assignment do not authorize sending messages, publishing code, or submitting work. Follow the user's current request for the scope of each task.

## Assignment requirements

- Use Vue and TypeScript for the frontend.
- Implement string processing as a Java library API using the latest generally available Java LTS release at implementation time. Verify that version and framework compatibility before setup, then pin and document the selected toolchain.
- Provide a modern web API between the frontend and the Java processor. The API framework and language are open choices.
- Support multiple transformation types and ordered composition: the output of each step becomes the input of the next.
- Derive frontend transformation configuration from backend-provided information. Adding a transformation that uses supported parameter types must require only backend implementation and registration, with no transformation-specific frontend component or branch.
- Use Git version control.
- Include reproducible local build and run instructions, implementation notes, and noteworthy design decisions.
- Preserve source code and commit history for eventual delivery as a repository or a source archive containing `.git`.

The PDF suggests replace, append, prepend, substring, and regex matching. These are examples, not five individually mandatory transformations. Use them as the initial implementation target unless the user changes the scope. More challenging transformations are optional extensions.

## Recommended architecture

Prefer a small modular application with a Vue frontend and a Java web service that directly depends on a separate transformation library. A single backend process is sufficient; the PDF does not require separate services.

Suggested starting choices are Vue 3 with Vite, a compatible Spring Boot release, and Maven with a checked-in wrapper. These choices may change for a documented reason. Reuse established project tooling once it exists.

Suggested layout:

```text
frontend/                 Vue and TypeScript application
backend/
  transformation-core/    Java library, independent of HTTP and UI frameworks
  web-api/                HTTP endpoints, validation, and error mapping
docs/                     Architecture, decisions, and AI usage notes
README.md                 Setup, commands, usage, and limitations
```

- Pipeline state uses immutable array/step replacements through `usePipeline` actions; metadata and results use shallow references. Do not mutate nested step state directly or reintroduce a deep preview watcher. Coalesce tooltip positioning into animation frames and clean up pending frames.
- Keep `App.vue` as page composition. Place HTTP calls in `frontend/src/api`, shared workflow state in `composables`, and presentation in focused components. Children emit typed edit events rather than mutating nested props. Keep styles grouped by responsibility under `frontend/src/styles`.
- Keep transformation semantics in the Java library. Do not duplicate processing in the browser or HTTP controllers.
- Keep the core usable and testable without starting a web server.
- Represent a pipeline as an ordered collection of transformation identifiers and parameter values.
- Keep the library API focused on transformation discovery, parameter validation, and pipeline execution.
- Keep each transformation in its own public, stateless class under `core/transformations`, implementing `Transformation` and annotated with `@AutoService(Transformation.class)`. The compiler generates Java SPI registration and `TransformationDiscovery` loads providers. Do not add central registration lists or transformation-ID switches. See `docs/adding-transformations.md`.
- Avoid databases, authentication, queues, and distributed infrastructure unless a concrete requirement justifies them.

## Metadata-driven configuration

The backend registry is the source of truth for available transformations and their parameters. Supply enough metadata for a generic frontend renderer, including:

- Stable transformation identifier, display name, and description.
- Parameter identifiers, labels, supported value types, required flags, and defaults where appropriate.
- Constraints such as bounds or enum choices, plus explanatory help text when useful.

Start with a small, documented metadata contract. Generic controls may support strings, multiline text, numbers, booleans, and enumerated values as needed. Avoid building a general-purpose schema engine beyond the assignment's needs.

- Render controls by parameter type, never by transformation identifier.
- Distinguish omitted values from valid empty strings, `false`, and zero.
- Validate requests on the backend even when the frontend also validates them.
- Return actionable errors for unknown transformations, unsupported parameters, invalid types, and invalid values.
- Prove extensibility with a backend-only addition using existing parameter types that appears and executes through the unchanged frontend.

## Transformation behavior and API

Recommended endpoint responsibilities are `GET /api/transformations` for discovery and `POST /api/transform` for execution. Final routes and payloads must be documented and tested.

- Accept input text and an ordered pipeline with explicit parameter objects.
- Return the final result. Per-step results are optional and useful for previews or debugging.
- Define empty-pipeline behavior; prefer returning the input unchanged.
- Preserve whitespace and line breaks unless a selected transformation explicitly changes them.
- Define and test replace semantics, substring index units and bounds, and regex matching output and no-match behavior.
- Document Unicode handling, especially whether substring indices count UTF-16 code units or Unicode code points.
- Return structured errors with the failing step and parameter where applicable. Do not silently return partial output as a successful final result.
- Reject malformed JSON and invalid pipeline shapes with suitable HTTP status codes. Do not expose stack traces to clients.
- Set reasonable input, pipeline, and output limits and document them. Bound regex execution risk; an HTTP timeout alone does not stop a pathological Java regex computation.
- Keep execution deterministic for the same input and configuration. Do not evaluate user-supplied code.

## User experience

Provide a complete path from input text to a configured pipeline and readable output without requiring code or raw JSON editing.

- Let users add, configure, remove, and reorder transformation steps.
- Make execution order and parameter labels clear.
- Provide readable input and output areas, automatic output updates after input or pipeline edits, and helpful empty states.
- Show loading, validation, API failure, and successful result states.
- Preserve configuration when validation fails and avoid displaying stale results as current.
- Associate errors with their fields or steps. Use labeled controls, visible focus, sufficient contrast, and keyboard-accessible reordering.
- Debounce automatic previews, cancel obsolete requests, prevent older responses from replacing newer results, and dispose pending timers and requests when leaving the workspace.
- Keep the interface usable at narrow widths. Prefer a focused, polished workflow over unrelated features.

## Verification

Add tests that demonstrate the assignment's behavior and meaningful edge cases:

- Core unit tests for each transformation, parameter validation, and pipeline ordering.
- A composition example where replacing `Fake` with `Real` in `Fake Python`, then appending `!`, produces `Real Python!`.
- Empty input, empty pipeline, repeated transformations, invalid parameters, Unicode, and multiline text where relevant.
- API tests for metadata, successful execution, malformed requests, and structured failures.
- Frontend tests for generic metadata rendering and pipeline editing.
- An integration or end-to-end check covering metadata discovery, configuration, execution, and displayed output.
- The extensibility check described above, without frontend transformation-specific changes.

Once tooling exists, run the relevant tests, type checks, lint checks, and production build for the affected modules. Record exact working commands in README and package/build scripts. Do not invent commands or claim checks passed without running them. Explain any checks blocked by the environment.

## Working conventions

- Inspect the repository and any more specific agent instructions before making changes. Update this file when the actual structure or workflow changes materially.
- Keep changes scoped to the user's request. Preserve unrelated work and avoid destructive Git operations.
- Use strict TypeScript, typed contracts, and established formatter conventions. Avoid untyped escape hatches without a concrete reason.
- Pin dependencies through build configuration and frontend lockfiles; check in the build wrapper. Keep secrets, generated output, dependency directories, and local configuration out of version control.
- The root Dockerfile builds and tests both modules, then runs the combined JAR as a non-root user. Keep container context exclusions in `.dockerignore`; bind the server to all container interfaces and publish its port on host loopback for local Podman use. See README for build/run commands.
- Keep commits reviewable and document significant tradeoffs without manufacturing or rewriting history.
- Do not publish, submit, contact the hiring team, or send logs externally without user authorization.

## Documentation and delivery readiness

README must describe prerequisites and pinned versions, installation, development startup, ports and configuration, production build/run, tests, and a sample pipeline. Include API examples and known limitations.

Document how the Java library is separated from HTTP handling, how metadata drives the UI, how to add a transformation, and any non-obvious semantics or tradeoffs. Highlight optional showcase features only after the required workflow works.

The PDF permits AI use and requests logs and technical decisions when AI is used. Maintain an honest AI usage record, for example `docs/ai-usage.md`, describing assisted work, decisions, and verification. Preserve available logs or exports for delivery, excluding secrets and unrelated personal material. Do not fabricate transcripts or claim generated notes are raw logs. If a full export is unavailable, state that limitation.

Before eventual delivery, verify the documented setup from a clean checkout where possible and confirm source and Git history are included. Creating this guidance file alone does not mean the application has been implemented or is ready to submit.
