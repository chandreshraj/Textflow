# UI performance

Optimizations preserve the existing theme, preview timing, error behavior, and keyboard/touch interactions. No additional runtime dependencies were added.

## Measurements

The baseline and regression tests use a 30-step pipeline. Measurements of reactive notifications and DOM reads are deterministic synthetic microbenchmarks, not frame-rate or device-latency measurements.

| Check | Before | After |
| --- | --- | --- |
| Synchronous pipeline notifications per reorder | 2 | 1 |
| Tooltip layout reads for 20 scroll events in one frame, without a clipping ancestor | 60 | At most 2 |
| Step components updated for one parameter edit in a 30-step pipeline | 1 | 1 (preserved) |
| Unchanged catalog updates during that parameter edit | 0 | 0 (preserved) |
| Preview response transfer bytes (80 KB repeated text, 30 trim steps, gzip accepted) | 2,481,483 | 3,546 |

The initial local preview transport baseline was 2,481,483 uncompressed response bytes for 80,000 repeated `a` characters and 30 trim steps, even when the client requested gzip. With compression enabled the same JSON transfers in 3,546 bytes: 99.86% less data. The benchmark script reproduces this case. Repetitive sample text compresses especially well; actual savings vary with text and pipeline.

## Implementation

- `usePipeline` publishes immutable step-array replacements and keeps unchanged step objects stable. Each action publishes at most one change; unchanged parameter values publish none. The preview watcher no longer deeply traverses all steps on each edit.
- Immutable discovery metadata and execution results use shallow references, avoiding unnecessary proxy wrapping.
- Pipeline/trace descriptors are indexed by ID, and search text is normalized once when metadata changes.
- Open tooltips use passive scroll listeners, schedule at most one layout update per animation frame, and reuse the anchor measurement. Pending frames are cancelled on close/unmount.
- Spring Boot negotiates gzip for large JSON previews and static assets, reducing network transfer while retaining the existing JSON shape. Responses below 1 KiB are left uncompressed. Compression reduces transfer size, not the size of JSON the browser must parse.
- Font stylesheets are discovered in the HTML head with preconnections; they no longer sit behind an external CSS import. Font display still uses swap and local fallback fonts.

The frontend bundle was already small and tree-shaken. This change targets reactive work, layout work, and transfer size rather than introducing extra splitting or UI virtualization for the bounded 30-step editor.

## Reproduce

```sh
npm --prefix frontend test -- src/test/performance.test.ts
npm --prefix frontend run build
cd backend && ./mvnw verify
```

After starting the built app, from the repository root:

```sh
python3 scripts/benchmark-preview.py
```

`ApiTest.compressesLargePreviewResponses` verifies gzip negotiation, decompression, and a substantial reduction for this sample. Existing browser workflows exercise live results, stale-response protection, stable previews, scrolling, tooltips, and desktop/mobile layouts.
