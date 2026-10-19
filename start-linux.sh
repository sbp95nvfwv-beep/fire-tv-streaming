#!/usr/bin/env bash
# Startet die private Mediathek auf einem Linux-PC und öffnet die Upload-Seite.
set -euo pipefail
cd "$(dirname "$(readlink -f "$0")")"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker fehlt. Installiere Docker Engine und das Docker-Compose-Plugin, dann starte dieses Skript erneut."
  exit 1
fi

if [[ ! -f .env ]]; then
  cp .env.example .env
  token=$(openssl rand -hex 32 2>/dev/null || date +%s | sha256sum | cut -d' ' -f1)
  sed -i "s|replace-with-a-long-random-secret|$token|" .env
  echo "Ein Admin-Token wurde in .env erzeugt. Bewahre ihn sicher auf."
fi

docker compose up --build -d
url="http://localhost:8080"
echo "Mediathek läuft unter $url"
command -v xdg-open >/dev/null 2>&1 && xdg-open "$url" >/dev/null 2>&1 || true
