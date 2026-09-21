#!/usr/bin/env bash
# Loads .env then starts the Spring Boot app.
# Usage: ./run.sh [extra mvnw args]
set -euo pipefail

if [[ -f .env ]]; then
    set -a
    # shellcheck disable=SC1091
    source .env
    set +a
else
    echo "Warning: .env not found. Copy .env.example to .env and fill in your keys." >&2
fi

exec ./mvnw spring-boot:run "$@"
