#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
npm --prefix frontend ci --no-audit --no-fund
npm --prefix frontend run test
npm --prefix frontend run build
cd backend
./mvnw verify
