# Textflow throughput and latency — local Podman measurement

Measured on 2026-10-24. This is an exploratory benchmark, not a production capacity or availability guarantee.

## Environment and method

- One Spring Boot application serving both the bundled Vue UI and transformation API; no database or downstream service calls.
- ARM64 Podman VM: 5 configured CPUs, 2 GiB memory, shared with another container. No separate CPU or memory limit on Textflow.
- Client runs on the macOS host through localhost port forwarding, HTTP without TLS, persistent connections, gzip accepted, request logging enabled.
- Synthetic deterministic ASCII text. Pipelines repeat normalize-whitespace, literal replace, upper case, lower case, URL encode, and URL decode, truncated to 3, 10 or 30 steps. These results do not cover every transformation or input distribution.
- Two passes at 1, 8 and 32 concurrent requests. Each case has 1 second of warmup and 4 seconds of measurement, plus time to drain in-flight requests.
- Closed-loop client: a worker sends another request after its previous response completes. This does not establish a maximum sustainable external arrival rate or a target SLA.
- Latency is client request start until the complete compressed HTTP response is received. It excludes browser decompression, JSON parsing and rendering.
- 170,580 measured requests, 0 failures. Preflight responses were decompressed and checked for expected step counts and transformed output. Load responses were checked for HTTP 200; they were not individually parsed.

## Warm second pass

| Input bytes | Steps | Concurrent requests | Requests/s | p50 ms | p95 ms | p99 ms | Requests sampled |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1,024 | 3 | 1 | 1,290.1 | 0.66 | 1.28 | 2.78 | 5,161 |
| 1,024 | 3 | 8 | 6,963.3 | 0.98 | 2.19 | 3.67 | 27,858 |
| 1,024 | 3 | 32 | 12,030.2 | 2.24 | 5.56 | 8.25 | 48,138 |
| 10,240 | 10 | 1 | 210.6 | 4.32 | 6.84 | 9.31 | 843 |
| 10,240 | 10 | 8 | 946.4 | 7.76 | 14.32 | 18.29 | 3,792 |
| 10,240 | 10 | 32 | 1,176.0 | 24.34 | 55.81 | 77.67 | 4,731 |
| 80,000 | 30 | 1 | 6.3 | 157.78 | 166.39 | 173.26 | 26 |
| 80,000 | 30 | 8 | 28.2 | 267.13 | 401.73 | 449.90 | 118 |
| 80,000 | 30 | 32 | 28.7 | 1072.63 | 1539.59 | 1629.57 | 132 |

The large serial case has only 26 samples in this pass; its tail percentiles are rough estimates. Longer tests with production-like traffic and an explicit latency objective are needed for capacity planning.

## Response sizes

| Input bytes / steps | JSON response bytes | Compressed body bytes |
| --- | ---: | ---: |
| 1,024 / 3 | 4,282 | 1,476 |
| 10,240 / 10 | 115,467 | 24,691 |
| 80,000 / 30 | 2,570,518 | 1,434,533 |

The large case returns every intermediate output as well as the final output. Its roughly 2.57 MB JSON response still transfers about 1.43 MB after gzip for this data. At 8 and 32 concurrent requests throughput stays around 28–29 requests/s while latency rises. That is an observed plateau for this workload, not a universal limit of the processor.

## Browser edit-to-result sample

The initial two-step Replace + Append example was measured with short text and the service otherwise idle, across 20 input edits in headless Chromium. The interval includes the 250 ms preview debounce, API request, response parsing, DOM update and two animation frames.

- p50: 297.8 ms
- p95: 299.1 ms
- Range: 280.9–314.7 ms

These browser figures apply to the short default example, not the 80 KB pipeline or a remote network.

## Saved evidence

- [Raw API results](benchmarks/textflow-load-results.json)
- [Raw browser results](benchmarks/textflow-ui-latency-results.json)

These are the original saved results from October 24, 2026, preserved without modification. They were not rerun for this documentation update. The tested source revision and container image digest were not recorded, so these results should not be treated as verification of the current build.
