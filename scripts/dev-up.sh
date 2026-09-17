#!/usr/bin/env bash
# Start local Postgres (Docker) and wait until it is healthy.
set -euo pipefail
cd "$(dirname "$0")/.."

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required for local Postgres. Install Docker Desktop (Windows) or Docker Engine." >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose v2 is required (docker compose)." >&2
  exit 1
fi

docker compose up -d

echo "Waiting for Postgres to be healthy..."
for _ in $(seq 1 45); do
  if docker compose exec -T postgres pg_isready -U app -d app >/dev/null 2>&1; then
    echo "Postgres is ready on localhost:5432"
    echo
    echo "DATABASE_URL hint (copy into .env if you have not already):"
    echo '  DATABASE_URL="postgresql://app:app@localhost:5432/app"'
    echo
    echo "Next (once the product has Prisma):"
    echo "  ./scripts/db-migrate.sh"
    echo "  ./scripts/db-seed.sh"
    echo "  npm run dev"
    exit 0
  fi
  sleep 1
done

echo "Postgres did not become healthy in time. Check: docker compose logs postgres" >&2
exit 1
