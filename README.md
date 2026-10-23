# Textflow

A no-code string transformation studio for the KNIME full-stack assignment. Compose transformations, configure them through backend metadata, and inspect each step's result.

## Requirements

- Java 25 LTS (verified with Temurin 25.0.4.1).
- Node.js 24 (verified with 24.10.0) and npm (verified with 11.18.0).
- Internet access for the first dependency installation. Maven 3.9.11 is downloaded by the checked-in wrapper; no global Maven installation is needed.

## Build and run

From the repository root:

```sh
./scripts/build.sh
java -jar backend/web-api/target/web-api-1.0.0.jar
```

Open http://127.0.0.1:8080. The executable JAR serves both the Vue production build and Java API. `PORT=8081 java -jar ...` changes the port. The server binds to loopback by default. Stop it with Ctrl-C.

The bundled example replaces `Fake` with `Real` in `Fake Python`, then appends `!`, producing `Real Python!`. Add steps from the library, edit their fields, move them with the arrow buttons, and run the pipeline. Edits clear the previous result; configuration survives errors. Output and individual step results preserve line breaks.

## Development

Build the Java modules once, then run the backend:

```sh
cd backend
./mvnw package
java -jar web-api/target/web-api-1.0.0.jar
```

In a second terminal:

```sh
cd frontend
npm ci
npm run dev
```

Open the Vite URL, normally http://127.0.0.1:5173. Vite proxies `/api` to port 8080. Frontend changes hot reload. Rebuild and restart the Java JAR after backend changes. Rebuild the frontend before packaging to refresh the bundled UI.

## Verification

```sh
cd frontend
npm test
npm run build             # strict TypeScript checks + production build
cd ../backend
./mvnw verify             # core unit tests + live HTTP integration tests
```

For the browser test, build the application first, stop any stale server on port 8080, then:

```sh
cd frontend
npx playwright install chromium
npm run test:e2e
```

Playwright starts the packaged Java server if none is running. It exercises the real API, composition, editing, reordering, validation, and mobile overflow. It also writes desktop/mobile screenshots to `docs/`. No separate lint configuration is installed; TypeScript and the builds enforce compilation checks.

## API

`GET /api/transformations` returns descriptors containing `id`, `name`, `description`, and `parameters`. Each parameter has an ID, label, generic type, required flag, default, optional numeric bounds, enum options, and help text.

```sh
curl http://127.0.0.1:8080/api/transformations
curl http://127.0.0.1:8080/api/transform \
  -H 'Content-Type: application/json' \
  -d '{"input":"Fake Python","pipeline":[{"transformation":"replace","parameters":{"old":"Fake","new":"Real"}},{"transformation":"append","parameters":{"text":"!"}}]}'
```

Execution returns `{"output":"Real Python!","steps":[...]}`. Each step result includes its zero-based `index`, transformation ID, and output. An empty pipeline returns the input unchanged.

Validation errors return HTTP 400 with `message`, nullable zero-based `step`, and nullable `parameter`. Requests over 1 MiB return 413. Input and each intermediate output are capped at 100,000 UTF-16 code units; pipelines at 30 steps; regex patterns at 500 characters. No partial execution is reported as successful. Unknown parameters are rejected; omitted parameters use declared defaults; explicit empty strings, zero, and false remain distinct from missing values.

## Transformations and semantics

| Operation | Behavior |
| --- | --- |
| Replace | Literal, case-sensitive, non-overlapping replacement of every occurrence. Find cannot be empty; replacement may be empty. |
| Append / prepend | Add literal text, including whitespace and line breaks. |
| Substring | Zero-based Unicode code-point indices; start inclusive, end exclusive; invalid bounds are errors. |
| Regex match | RE2 syntax, all full matches joined with `\n`; no match produces empty text. Zero-width matches follow RE2/J matcher iteration. Lookaround and backreferences are unsupported. |
| Change case | Upper/lowercase using `Locale.ROOT`; may change text length. |
| Trim whitespace | Java `String.strip()` semantics (`Character.isWhitespace`), not every Unicode separator such as non-breaking space. |

## Architecture and extensions

See [architecture](docs/architecture.md) and [AI usage record](docs/ai-usage.md). The Java library has no web-framework dependencies. The server handles HTTP and calls the processor. The Vue application renders generic parameter controls; there is no transformation-ID switch for configuration or execution. Only the optional example loader names specific operations.

To add an operation, add a public class implementing `Transformation` and annotate it with `@AutoService(Transformation.class)`. Metadata and processing logic live together in that class. The compiler generates Java SPI registration; `TransformationDiscovery` loads providers in stable ID order. No central list, controller, or frontend edits are needed. Rebuild, restart the server, and reload the browser. See [adding a transformation](docs/adding-transformations.md) for a copyable example. `ProcessorTest.discoversAnnotatedClassWithoutRegistryChanges` verifies discovery and execution of a test-only provider; the UI test renders and executes previously unknown metadata.

## Scope and limitations

This is a local assignment application, with no authentication, persistence, or multi-user storage. Refreshing resets the workspace. The processor is synchronous and returns intermediate text for all steps. RE2/J avoids catastrophic backtracking but deliberately supports a subset of Java regex syntax. UI character counts and length limits use JavaScript/Java UTF-16 units, while substring indices use Unicode code points, not grapheme clusters. Fonts load from Google Fonts when available, with local fallbacks. A hostile multi-user deployment would need deployment-specific rate limiting and concurrency controls.

Source control is initialized locally. Keep the genuine `.git` history with any source archive; do not include `node_modules`, build output, or credentials. No remote publication or assignment submission is performed by the build.
