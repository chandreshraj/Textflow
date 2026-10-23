# Architecture and decisions

## Boundaries

`transformation-core` exports descriptor records, the `Transformation` extension interface, a registry-backed processor, typed step/result records, and structured validation errors. Its runtime processing dependency is RE2/J. AutoService annotations and a compile-time annotation processor generate standard Java SPI provider resources. No runtime scanning framework is needed. `web-api` depends on core and Spring Boot 4.0.0, exposes discovery/execution endpoints, caps JSON bytes before deserialization, and maps validation failures to HTTP. The production JAR embeds `frontend/dist`; development uses Vite's API proxy to keep requests same-origin.

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

- Each operation is a separate public class under `core/transformations`, implementing `Transformation` and annotated with `@AutoService`. The compiler generates `META-INF/services/com.knime.textflow.core.Transformation`. `TransformationDiscovery` uses Java `ServiceLoader`, sorts providers by ID, and passes them to the processor, which rejects duplicate IDs. No hardcoded list is maintained. Providers require a public no-argument constructor and should be stateless because the processor is shared by concurrent requests. Additional provider JARs can participate when present on the runtime classpath.
- RE2/J provides bounded matching behavior instead of relying on cancellation of Java's backtracking regex engine.
- Code-point substring semantics avoid splitting surrogate pairs; grapheme-cluster indexing is outside scope.
- One MiB request, 100k text, and 30-step caps keep a local interactive workload bounded. Replace checks expansion before allocation. Returning step output costs up to roughly 3 million UTF-16 units per execution, excluding serialization overhead.
- No database is necessary for ephemeral text processing. No user text is deliberately logged or sent to external services.
- The sample pipeline is a convenience preset, independent of the metadata renderer.

## Verification approach

Core tests check composition, all built-ins, defaults, empty values, Unicode, error locations, bounds, and a discovered test-only extension, plus explicit processor injection for standalone use. HTTP integration tests start a real ephemeral server and check discovery, execution, bad JSON, and validation. Vue component tests check unknown metadata, pipeline editing, value types, and offline states. Playwright checks the assembled application at desktop/mobile widths.
