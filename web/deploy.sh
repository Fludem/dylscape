#!/usr/bin/env bash
# Deploys the Onyx website to the VPS.
#
#   web/deploy.sh            build, test, upload, restart, verify
#   web/deploy.sh --dry-run  build and test only; touch nothing remote
#
# This never restarts the game server, so it is safe to run while people are playing. Nor does it
# touch /var/www/rsps: jav_config.ws and worldlist.ws live there and every client depends on them,
# which is why the site has its own root at /var/www/onyx.

set -euo pipefail

HOST="${ONYX_HOST:-root@2.28.73.211}"
DOMAIN="${ONYX_DOMAIN:-rsps.onyxleeds.co.uk}"
SITE_ROOT=/var/www/onyx
API_ROOT=/opt/onyx-web

cd "$(dirname "$0")/.."
DRY_RUN=0
[[ "${1:-}" == "--dry-run" ]] && DRY_RUN=1

say() { printf '\n\033[1m==> %s\033[0m\n' "$*"; }

say "Generating guide data"
python3 web/build_data.py

say "Running tests"
( cd web/api && python3 -m unittest discover -p 'test_*.py' )
( cd web && python3 -m unittest test_build_data )

# Catches a typo in a page that the tests cannot see.
say "Checking pages parse"
python3 - <<'PY'
import html.parser, pathlib, sys
class Check(html.parser.HTMLParser):
    def error(self, message): raise AssertionError(message)
missing = []
for page in sorted(pathlib.Path("web/site").rglob("*.html")):
    text = page.read_text()
    Check().feed(text)
    for asset in ("/assets/onyx.css", "/assets/app.js"):
        if asset not in text:
            missing.append(f"{page}: does not reference {asset}")
if missing:
    sys.exit("\n".join(missing))
print(f"checked {len(list(pathlib.Path('web/site').rglob('*.html')))} pages")
PY

if [[ $DRY_RUN -eq 1 ]]; then
  say "Dry run: stopping before anything remote"
  exit 0
fi

say "Uploading site to $HOST:$SITE_ROOT"
ssh "$HOST" "mkdir -p $SITE_ROOT $API_ROOT"
# --delete is safe here and only here: this root holds nothing but the generated site.
rsync -az --delete web/site/ "$HOST:$SITE_ROOT/"

say "Uploading API to $HOST:$API_ROOT"
rsync -az --delete --exclude '__pycache__' --exclude 'test_*.py' \
  web/api/ "$HOST:$API_ROOT/"

say "Installing service and web configuration"
rsync -az web/deploy/onyx-web.service "$HOST:/etc/systemd/system/onyx-web.service"
rsync -az web/deploy/Caddyfile "$HOST:/etc/caddy/Caddyfile"

ssh "$HOST" bash -s <<'REMOTE'
set -euo pipefail
chown -R caddy:caddy /var/www/onyx
chown -R root:root /opt/onyx-web
touch /var/log/onyx-web.log && chown rsmod:rsmod /var/log/onyx-web.log
# Refuse to reload a Caddyfile that does not parse; a bad one takes the client's config files down
# with the website.
caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile >/dev/null
systemctl daemon-reload
systemctl enable --now onyx-web
systemctl restart onyx-web
systemctl reload caddy
REMOTE

say "Verifying"
sleep 2
fail=0
check() {
  local label="$1" url="$2" want="$3"
  local got
  got=$(curl -s -o /dev/null -w '%{http_code}' --max-time 15 "$url")
  if [[ "$got" == "$want" ]]; then
    printf '  ok    %-46s %s\n' "$label" "$got"
  else
    printf '  FAIL  %-46s %s (wanted %s)\n' "$label" "$got" "$want"
    fail=1
  fi
}
# The client's config files come first: if the deploy broke those, nothing else matters.
check "jav_config.ws (client login)" "https://$DOMAIN/jav_config.ws" 200
check "worldlist.ws  (client login)" "https://$DOMAIN/worldlist.ws" 200
check "home"                          "https://$DOMAIN/" 200
check "hiscores"                      "https://$DOMAIN/hiscores.html" 200
check "guides"                        "https://$DOMAIN/guides/" 200
check "drop data"                     "https://$DOMAIN/data/drops-index.json" 200
check "api status"                    "https://$DOMAIN/api/status" 200
check "api hiscores"                  "https://$DOMAIN/api/hiscores?skill=overall" 200

ssh "$HOST" 'systemctl is-active rsmod caddy onyx-web' | tr '\n' ' '; echo

if [[ $fail -ne 0 ]]; then
  say "Deploy finished with failures above"
  exit 1
fi
say "Deployed: https://$DOMAIN/"
