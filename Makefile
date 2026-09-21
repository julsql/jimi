# Point d'entree unique du monorepo. Chaque cible reste utilisable seule,
# pour que la CI et le poste de dev lancent exactement la meme chose.

.PHONY: help setup require-setup test test-api test-app build-web db api app

help:
	@grep -E '^[a-z-]+:.*?## .*$$' $(MAKEFILE_LIST) \
	  | awk 'BEGIN{FS=":.*?## "}{printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

setup:  ## Prepare l'environnement de dev local (deps npm, hooks git, .env)
	@test -d apps/app/node_modules || (cd apps/app && npm ci --silent --no-audit --no-fund)
	@test -f apps/api/.env || cp apps/api/.env.example apps/api/.env
	@test -f apps/app/.env || cp apps/app/.env.example apps/app/.env
	@npx --yes lefthook@1 install >/dev/null 2>&1 || true
	@echo "Environnement pret. Renseigner MISTRAL_API_KEY dans apps/api/.env."

require-setup:
	@test -d apps/app/node_modules || { echo "Deps npm absentes. Lancer: make setup" >&2; exit 1; }

test-api:  ## Tests unitaires de l'API (Maven)
	@cd apps/api && ./mvnw -B -q test

test-app: require-setup  ## Typecheck du client
	@cd apps/app && npm run typecheck --silent

build-web: require-setup  ## Bundle web statique du client (apps/app/dist)
	@cd apps/app && npm run build:web

test: test-api test-app  ## Toute la suite

db:  ## MariaDB locale (Docker)
	@docker compose -f apps/api/docker-compose.db.yml up -d

api:  ## Lance l'API en local (charge apps/api/.env)
	@cd apps/api && ./run.sh

app: require-setup  ## Lance le client web en local
	@cd apps/app && npm run web
