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
6. The UI renders output and expandable step results. A revision counter discards responses for an edited pipeline. Changes trigger a preview after a 250 ms debounce; there is no Run button. An AbortController cancels obsolete requests, and revision checks also reject late results or errors even if cancellation is ignored. Pending timers and active requests are cancelled on component disposal.

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


## Frontend modules

`App.vue` composes the page and connects events; it contains no network calls or pipeline editing logic.

```text
frontend/src/
  api/transformations.ts        Typed HTTP client and API error mapping
  composables/usePipeline.ts   Input, ordered steps, editing actions
  composables/useWorkspace.ts  Discovery and workspace coordination
  composables/useLiveExecution.ts  Debounce, cancellation, execution and revision checks
  presets/example.ts           Optional example configuration
  components/
    StudioHeader.vue           Application header
    TransformationCatalog.vue  Searchable backend-provided library
    TextInput.vue              Input editor
    PipelineEditor.vue         Step list, edit/clear actions and live status
    PipelineStep.vue           One step, metadata fields, ordering controls
    ParameterField.vue         Generic control by parameter type
    OutputPanel.vue            Current/previous preview state and guarded copying
    ExecutionTrace.vue         Expandable intermediate results
  styles/                      Base, shell, catalog, pipeline, editors, trace, responsive CSS
```

Pipeline edits flow upward as typed component events and are applied by `usePipeline`; children do not mutate nested props. Search, expanded results, and copy confirmation remain local presentation state. `useWorkspace` coordinates discovery and editing. `useLiveExecution` watches input, pipeline, and discovery readiness; retains the last successful preview with an explicit stale flag on edits; debounces HTTP requests; and ignores responses for older revisions. Loading covers both the debounce period and the request. Stale output and step results stay mounted to avoid layout movement; copy is disabled until the displayed result is current. A failed update preserves the previous result with a needs-attention state. Discovery must succeed before execution starts. Component and API errors retain their existing field and step locations.

On the backend, `ProcessorConfiguration` owns discovery and dependency wiring; the controller receives a processor through constructor injection. `TransformRequest` owns the HTTP input-shape check. `ParameterValidator` applies descriptor defaults and validates values independently of the pipeline executor. The compatibility `Builtins.all()` entry point delegates to discovery, so it contains no duplicate implementations or registration list.


## High-priority UI improvements

At desktop widths (1100px and above), the editing column and sticky preview appear side by side. Below that breakpoint, the preview follows the editor and a persistent View output button scrolls to and focuses its landmark. No transformation drawer, diff mode, inline step previews, or undo feature was added. Labels/descriptions use larger, darker text; step controls provide 36px desktop and 44px phone hit areas. Preview status distinguishes updating, updated and needs-attention, and previous output cannot be copied while stale.


## Visual theme

The application theme is adapted from https://www.knime.com/ (reviewed 2026-10-23): yellow `#fdd800`, orange `#f89639`, dark text, white/neutral surfaces, Figtree headings and Inter body text, pill buttons, and a decorative angular line motif. `frontend/src/styles/theme.css` owns shared semantic tokens, which the existing stylesheet modules consume. A darker orange variant is used for heading text to preserve contrast. Textflow keeps its name and symbol; no claim of official KNIME ownership is added. Existing preview states, touch targets, responsive layout, and pipeline behavior are retained.
