# Contribuer à JIMI

## Les règles qui priment

- **Le LLM ne détruit rien.** En mode agenda, une modification ou une
  suppression renvoie `AWAITING_CONFIRMATION` ; l'agenda n'est touché qu'après
  `POST /chat/confirm`, sur les identifiants enregistrés, sans reconsulter le
  LLM. Toute évolution de `ChatService` doit préserver cette garantie.
- **Le mode legacy reste le défaut.** Les apps déjà installées envoient
  `calendarMode=false` et ne gèrent pas la confirmation : ne pas casser ce
  contrat tant qu'elles circulent.
- **Aucun contenu d'agenda n'est persisté** côté API, et les jetons OAuth sont
  chiffrés (AES-256-GCM, `TOKEN_ENCRYPTION_KEY`).

## Changer le contrat d'API

Client et API vivent dans le même dépôt pour qu'un changement de contrat tienne
dans **une seule PR** : l'endpoint, le client (`apps/app/api/`), et les README
des deux apps.

## Commandes

```sh
make setup      # environnement local
make test       # tests API + typecheck client
make test-api   # ./mvnw test
make test-app   # tsc --noEmit
make build-web  # bundle web statique
```

## Commits

[Conventional Commits](https://www.conventionalcommits.org), en anglais. Le
scope est l'app touchée : `fix(api):`, `feat(app):`, `ci:`, `docs:`. Le hook
`commit-msg` (lefthook) le vérifie.

## Releases

`release-please` ouvre **une seule PR de release** pour tout le dépôt : des PR
séparées touchent toutes `.release-please-manifest.json` et se mettent en
conflit, et release-please ne rebase pas ses propres PR.

Les **tags restent par composant** (`api-v1.1.0`, `app-v3.2.0`). Le web et l'API
se déploient déjà à chaque push sur `main` ; les tags servent de repère pour les
builds natifs envoyés à la main aux stores.

Pour l'app, release-please met à jour `package.json`, `app.json`,
`android/app/build.gradle` et `Info.plist`. Les numéros de build (`versionCode`,
`CFBundleVersion`) restent à incrémenter à la main avant chaque envoi.

**Ne jamais rebaser une PR de release à la main.** Si elle diverge, la fermer et
relancer le workflow `release-please`.

## Sécurité

- Les secrets de signature (keystore, `.p12`, clefs Apple/Google) n'entrent pas
  dans le dépôt : `.gitignore` et le hook `pre-commit` les bloquent. Ils vivent
  dans `keys/`, ignoré.
- Perdre le keystore d'upload Play Store, c'est perdre la possibilité de publier
  des mises à jour : il doit être sauvegardé hors de la machine.
- Une vulnérabilité de dépendance se corrige, même en développement.
