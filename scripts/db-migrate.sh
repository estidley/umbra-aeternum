#!/usr/bin/env bash
# Apply Prisma migrations to DATABASE_URL (Postgres) and regenerate the client.
# Stub: product repo must have Prisma. This template does not ship an app.
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ ! -f package.json ]]; then
  echo "No package.json yet — this playbook is process + stubs. Add the app, then re-run." >&2
  exit 1
fi

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

npx prisma generate
npx prisma migrate deploy
