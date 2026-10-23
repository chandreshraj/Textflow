# Architecture and decisions

## Boundaries

`transformation-core` exports descriptor records, the `Transformation` extension interface, a registry-backed processor, typed step/result records, and structured validation errors. It depends only on RE2/J. `web-api` depends on core and Spring Boot 4.0.0, exposes discovery/execution endpoints, caps JSON bytes before deserialization, and maps validation failures to HTTP. The production JAR embeds `frontend/dist`; development uses Vite's API proxy to keep requests same-origin.

Java 25 satisfies the latest LTS requirement. A single Java backend avoids a cross-language bridge and a second service while preserving a reusable library boundary. Maven's reactor compiles and tests both modules; the wrapper pins Maven 3.9.11. Vue 3 composition API and strict TypeScript keep the frontend small. Dependencies and the npm lockfile pin the selected build.

## Data flow

1. The browser fetches transformation descriptors.
2. A generic field component renders controls based on parameter types, including numbers, booleans, enum choices, and text.
3. The user constructs an ordered list of IDs and parameter maps.
4. The processor validates each step, applies defaults only for omitted values, and executes library implementations sequentially.
5. The API returns final and intermediate outputs, or a located validation error.
6. The UI renders output and expandable step results. A revision counter discards responses for an edited pipeline. Run remains disabled during an in-flight request.

No transformation code runs in the browser. New operations using supported parameter types require only implementation/registration in Java; operations with a genuinely new parameter type would require extending the generic renderer and contract.

## Tradeoffs

- Explicit registration is easy to review for seven operations; service loading would be useful for third-party plugins.
- RE2/J provides bounded matching behavior instead of relying on cancellation of Java's backtracking regex engine.
- Code-point substring semantics avoid splitting surrogate pairs; grapheme-cluster indexing is outside scope.
- One MiB request, 100k text, and 30-step caps keep a local interactive workload bounded. Replace checks expansion before allocation. Returning step output costs up to roughly 3 million UTF-16 units per execution, excluding serialization overhead.
- No database is necessary for ephemeral text processing. No user text is deliberately logged or sent to external services.
- The sample pipeline is a convenience preset, independent of the metadata renderer.

## Verification approach

Core tests check composition, all built-ins, defaults, empty values, Unicode, error locations, bounds, and a registered extension. HTTP integration tests start a real ephemeral server and check discovery, execution, bad JSON, and validation. Vue component tests check unknown metadata, pipeline editing, value types, and offline states. Playwright checks the assembled application at desktop/mobile widths.
