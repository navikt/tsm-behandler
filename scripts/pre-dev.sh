#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

docker compose up -d

echo "Venter på postgres på localhost:7971..."
for _ in $(seq 1 30); do
  if docker compose exec -T postgres pg_isready -U user -d tsm-behandler >/dev/null 2>&1; then
    echo "postgres er klar"
    exit 0
  fi
  sleep 1
done

echo "postgres ble ikke klar i tide" >&2
exit 1
