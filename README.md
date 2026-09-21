<p align="center"><img src="docs/jimi.png" alt="JIMI" width="120"></p>

# JIMI

Assistant d'agenda conversationnel : on écrit « Ajoute une réunion demain à
14 h » ou « Qu'est-ce que j'ai aujourd'hui ? », un LLM extrait l'intention et
l'événement structuré, et l'API agit sur l'agenda — ou demande ce qui manque.

JIMI agit sur l'agenda **réel** de l'utilisateur (Google, Microsoft, CalDAV :
iCloud, Fastmail, Nextcloud…). Le LLM n'exécute jamais une action destructive :
une modification ou une suppression attend une confirmation explicite, puis
s'applique sans repasser par le LLM.

- Web : [jimi.julsql.fr](https://jimi.julsql.fr)
- Android : [Play Store](https://play.google.com/store/apps/details?id=fr.tsp.jimithechatbot)
- API : [jimi-api.julsql.fr](https://jimi-api.julsql.fr/swagger-ui/index.html)

## Organisation

| Chemin      | Contenu                                                                   |
| ----------- | ------------------------------------------------------------------------- |
| `apps/api/` | API Spring Boot 3 / Java 17 — LLM, fournisseurs d'agenda, MariaDB         |
| `apps/app/` | Client Expo / React Native — une base de code pour iOS, Android et le web |
| `docs/`     | Ressources partagées                                                      |

Chaque app garde son README détaillé : contrat d'API, configuration,
fournisseurs d'agenda et procédure de release Android.

## Pourquoi un monorepo

Le client et l'API partagent un contrat (`POST /chat`, `/chat/confirm`,
`/agenda`, `/connect/*`) qui évolue en même temps des deux côtés — le passage
aux agendas réels a touché les deux dépôts à chaque étape. Un seul dépôt permet
une PR par changement de contrat, une CI et une politique de sécurité communes.

## Démarrage

```sh
make setup      # deps npm, hooks git, .env copiés depuis les .env.example
make db         # MariaDB locale (Docker)
make api        # API sur :8080 — renseigner MISTRAL_API_KEY dans apps/api/.env
make app        # client web
make test       # tests de l'API + typecheck du client
```

`make help` liste toutes les cibles.

## CI/CD

| Workflow         | Déclencheur             | Rôle                                                        |
| ---------------- | ----------------------- | ----------------------------------------------------------- |
| `api`            | `apps/api/**`           | `mvnw test`, puis image `ghcr.io/julsql/jimi/api`           |
| `app`            | `apps/app/**`           | typecheck + build web, puis image `ghcr.io/julsql/jimi/app` |
| `security`       | chaque PR, hebdomadaire | gitleaks, CodeQL, `npm audit`, Trivy sur les deux images    |
| `release-please` | push sur `main`         | versions, changelogs et tags par app (`api-v*`, `app-v*`)   |

Un push sur `main` publie l'image, puis notifie Keel qui redéploie sur k3s
(manifestes dans le dépôt serveur, `k3s/jimi/`). Les builds natifs (Play Store,
App Store) restent manuels : voir [`apps/app/README.md`](apps/app/README.md).

## Contribuer

Voir [CONTRIBUTING.md](CONTRIBUTING.md). Les commits suivent
[Conventional Commits](https://www.conventionalcommits.org), avec l'app en scope.

## Licence

MIT — voir le `LICENSE` de chaque app.
