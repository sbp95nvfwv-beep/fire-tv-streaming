#!/usr/bin/env bash
# Startet die private Mediathek auf einem Linux-PC und öffnet die Upload-Seite.
set -euo pipefail
cd "$(dirname "$(readlink -f "$0")")"

if ! command -v python3 >/dev/null 2>&1; then
  echo "Python 3 fehlt. Installiere Python 3 und starte dieses Skript erneut."
  exit 1
fi

if [[ ! -f .env ]]; then
  cp .env.example .env
  token=$(openssl rand -hex 32 2>/dev/null || python3 -c 'import secrets; print(secrets.token_hex(32))')
  sed -i "s|replace-with-a-long-random-secret|$token|" .env
  echo "Ein Admin-Token wurde in .env erzeugt. Bewahre ihn sicher auf."
fi

if ! pgrep -f "server/linux_server.py" >/dev/null 2>&1; then
  nohup python3 server/linux_server.py > server.log 2>&1 &
  sleep 1
fi
url="http://localhost:8080"
echo "Mediathek läuft unter $url"
command -v xdg-open >/dev/null 2>&1 && xdg-open "$url" >/dev/null 2>&1 || true
