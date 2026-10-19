#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [[ ! -f .env ]]; then
  cp .env.example .env
  echo "Bitte setze ADMIN_TOKEN in .env und starte das Skript erneut."
  exit 1
fi
docker compose up --build -d
if command -v open >/dev/null; then open http://localhost:8080; elif command -v xdg-open >/dev/null; then xdg-open http://localhost:8080; fi
