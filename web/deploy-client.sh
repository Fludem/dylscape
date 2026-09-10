#!/usr/bin/env bash
# Builds the zipped clients and publishes them at https://rsps.onyxleeds.co.uk/downloads/.
#
#   web/deploy-client.sh              rebuild both zips, upload, verify
#   web/deploy-client.sh --no-build   upload whatever is already in build/client-zip
#
# Separate from deploy.sh because the zips are ~140 MB and only change when the client does; a
# site deploy should not re-upload them. Never restarts the game server. Serving them needs the
# /downloads route in web/deploy/Caddyfile, which deploy.sh installs.

set -euo pipefail

HOST="${ONYX_HOST:-root@2.28.73.211}"
DOMAIN="${ONYX_DOMAIN:-rsps.onyxleeds.co.uk}"
DL_ROOT=/var/www/downloads
OUT=build/client-zip
JAVAC="${JAVAC:-/opt/homebrew/opt/openjdk@21/bin/javac}"

cd "$(dirname "$0")/.."
say() { printf '\n\033[1m==> %s\033[0m\n' "$*"; }

if [[ "${1:-}" != "--no-build" ]]; then
  say "Building client zips"
  python3 tools/client/make_zips.py --javac "$JAVAC"
fi

shopt -s nullglob
zips=("$OUT"/Onyx-*.zip)
[[ ${#zips[@]} -gt 0 && -f "$OUT/client.json" ]] || { echo "no zips in $OUT; build first"; exit 1; }

say "Uploading to $HOST:$DL_ROOT"
ssh "$HOST" "mkdir -p $DL_ROOT"
# Zips first and the index last, so the page never advertises a size or checksum for a file that
# is still uploading. rsync writes to a temp name and renames, so a download mid-upload gets the
# old zip rather than half of the new one.
rsync -az "${zips[@]}" "$HOST:$DL_ROOT/"
rsync -az "$OUT/client.json" "$HOST:$DL_ROOT/"
ssh "$HOST" "chown -R caddy:caddy $DL_ROOT"

say "Verifying"
fail=0
while read -r name sha; do
  remote=$(ssh -n "$HOST" "sha256sum $DL_ROOT/$name" | cut -d' ' -f1)
  code=$(curl -s -o /dev/null -I -w '%{http_code}' --max-time 15 "https://$DOMAIN/downloads/$name")
  if [[ "$remote" == "$sha" && "$code" == 200 ]]; then
    printf '  ok    %-26s %s, checksum matches\n' "$name" "$code"
  else
    printf '  FAIL  %-26s http %s, remote sha256 %s\n' "$name" "$code" "${remote:0:16}"
    fail=1
  fi
done < <(python3 -c 'import json,sys
for f in json.load(open(sys.argv[1]))["files"]: print(f["name"], f["sha256"])' "$OUT/client.json")

for path in downloads/client.json jav_config.ws; do
  code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 15 "https://$DOMAIN/$path")
  [[ "$code" == 200 ]] && printf '  ok    %-26s %s\n' "$path" "$code" \
                       || { printf '  FAIL  %-26s %s\n' "$path" "$code"; fail=1; }
done

if [[ $fail -ne 0 ]]; then
  say "Client deploy finished with failures above"
  exit 1
fi
say "Published: https://$DOMAIN/play.html"
