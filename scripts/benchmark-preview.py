#!/usr/bin/env python3
"""Measure transport bytes for a local maximum-step preview; start the app first."""
import gzip
import json
import sys
import urllib.request

base_url = sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8080"
payload = json.dumps({
    "input": "a" * 80000,
    "pipeline": [{"transformation": "trim", "parameters": {}} for _ in range(30)],
}).encode()
request = urllib.request.Request(
    base_url.rstrip("/") + "/api/transform", data=payload,
    headers={"Content-Type": "application/json", "Accept-Encoding": "gzip"},
)
with urllib.request.urlopen(request, timeout=30) as response:
    body = response.read()
    encoding = response.headers.get("Content-Encoding", "none")
decoded = gzip.decompress(body) if encoding == "gzip" else body
result = json.loads(decoded)
assert result["output"] == "a" * 80000
assert len(result["steps"]) == 30
print(json.dumps({
    "contentEncoding": encoding,
    "transferBytes": len(body),
    "jsonBytes": len(decoded),
    "reductionPercent": round(100 * (1 - len(body) / len(decoded)), 2),
}, indent=2))
