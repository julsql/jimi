#!/usr/bin/env bash
#
# Backup jimi — dump de la base MariaDB (donnees a conserver).
# La DB n'est pas exposee sur l'hote en prod -> dump via `docker exec`.
#
# Usage (sur le VPS, typiquement via cron) :
#   BACKUP_DIR=/backup/current ./scripts/backup.sh
#
# Variables d'environnement :
#   BACKUP_DIR    repertoire de sortie          (defaut: /backup/current)
#   RETENTION     nb de dumps a conserver        (defaut: 7)
#   DB_CONTAINER  nom du conteneur MariaDB       (defaut: jimi-db)
#
# Identifiants lus dans l'environnement du conteneur
# (MARIADB_ROOT_PASSWORD / MARIADB_DATABASE) : aucun secret dans ce script.
# MYSQL_PWD est utilise pour eviter le mot de passe en ligne de commande.
#
set -euo pipefail
export PATH="/usr/local/bin:/usr/bin:/bin:${PATH:-}"

APP="jimi"
DB_CONTAINER="${DB_CONTAINER:-jimi-db}"
BACKUP_DIR="${BACKUP_DIR:-/backup/current}"
RETENTION="${RETENTION:-7}"
STAMP="$(date +%Y%m%d_%H%M%S)"

mkdir -p "$BACKUP_DIR"

echo "[$APP] Dump de la base ($DB_CONTAINER)..."
TMP="$BACKUP_DIR/.${APP}_db_${STAMP}.sql.gz.tmp"
if docker exec "$DB_CONTAINER" sh -c \
     'MYSQL_PWD="$MARIADB_ROOT_PASSWORD" exec mariadb-dump --single-transaction --routines --events -u root "$MARIADB_DATABASE"' \
     | gzip > "$TMP"; then
  mv "$TMP" "$BACKUP_DIR/${APP}_db_${STAMP}.sql.gz"
else
  rm -f "$TMP"
  echo "[$APP] ERREUR : echec du dump de la base" >&2
  exit 1
fi

find "$BACKUP_DIR" -maxdepth 1 -type f -name "${APP}_db_*.sql.gz" -printf '%T@ %p\n' \
  | sort -rn | tail -n +$((RETENTION + 1)) | cut -d' ' -f2- | xargs -r rm -f

echo "[$APP] Termine -> $BACKUP_DIR"
