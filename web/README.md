# The Onyx website

`https://rsps.onyxleeds.co.uk/` — landing page, hiscores and guides, served by the Caddy instance
that was already terminating TLS for the client's config files.

The game server is not involved. It gains no dependency, is not restarted by a deploy, and does
not know the site exists.

```
web/
  site/            static pages; no build step, no framework, no package manager
  api/             hiscores.py (HTTP) + onyxdb.py (queries) + tests. Stdlib only
  build_data.py    generates site/data/ from the repo's own content data
  deploy/          Caddyfile and onyx-web.service, as deployed
  deploy.sh        build, test, upload, restart, verify
```

## Deploy

```bash
web/deploy.sh --dry-run   # build + tests only, nothing remote
web/deploy.sh             # the real thing
```

It regenerates the guide data, runs the tests, uploads, reloads Caddy and the API service, then
curls the live site — `jav_config.ws` first, because a broken deploy that takes out the client's
config file is the only outcome here that actually matters.

## Run it locally

```bash
python3 web/build_data.py
ONYX_DB=.data/saves/game.db ONYX_REALM=1 ONYX_PORT=8099 python3 web/api/hiscores.py
```

Then serve `web/site/` on another port with `/api/*` proxied to 8099, or point the pages at the
API directly by setting `window.ONYX_API` before `app.js` loads. Realm 1 is `dev` — the local
`.data/saves/game.db`; realm 2 is `main`, which is what the VPS runs.

```bash
cd web/api && python3 -m unittest discover -p 'test_*.py'
```

## How it reads player data

Everything comes from `characters`, `accounts` and `stats` in the game's SQLite save file, opened
read-only. Five things about that schema will produce plausible, wrong numbers if forgotten, and
all five are handled in `api/onyxdb.py`:

| | |
|---|---|
| `stats.fine_xp` is XP × 10 | `PlayerStatMap.XP_FINE_PRECISION` |
| Stat ids 23 and 24 are not skills | `sailing`/`unreleased` are written on every save; exclude them from totals |
| `realm_id` 1 is dev, 2 is main | The VPS is realm 2 |
| `accounts.display_name` is nullable | Fall back to `login_username` |
| Combat level truncates | `prayer/2`, `ranged/2`, `magic/2` are Kotlin `Int` divisions — true division puts the site half a level above the client |

### Two traps worth spelling out

**Stats are only written on logout.** `AccountManager.save` has exactly one caller,
`AccountRegistry.queueLogout`. There is no periodic autosave, so the hiscores show each player's
last-logout state and an online player's numbers are stale until they log off. The site says so
rather than hiding it. Making them live would mean adding a periodic save to the game server —
note that the existing save path also stamps `last_logout = CURRENT_TIMESTAMP`, so it needs more
care than adding a tick.

**The schema mixes time zones and stores no offsets.** `last_logout`, `created_at` and
`stats.updated_at` come from SQLite's `CURRENT_TIMESTAMP`, which is UTC. But `characters.last_login`
is bound from `LocalDateTime.now()` in `CharacterAccountApplier`, which is the *game host's local
time*. Reading both as UTC puts "last login" in the future on any machine that is not on UTC. The
API resolves both and emits ISO-8601 with explicit offsets, which is only correct because the web
service runs on the same machine as the game server. The VPS is on `Etc/UTC`, so the two agree
there anyway; a development machine on BST is where this shows up.

## The service

`onyx-web.service` runs `hiscores.py` as **`User=rsmod`**, bound to `127.0.0.1:8081`. The user
matters: the database is in WAL mode, and even a read-only SQLite connection needs write access to
the `-shm` sidecar, which `rsmod` owns. Caddy reverse-proxies `/api/*` to it. No firewall change is
needed — `ufw` still allows only 22, 80, 443 and 43594.

```bash
ssh root@2.28.73.211 'systemctl status onyx-web; tail -20 /var/log/onyx-web.log'
```

## The one rule

**Never rsync `--delete` into `/var/www/rsps`.** That directory holds `jav_config.ws` and
`worldlist.ws`; deleting them stops every handed-out client from logging in. The website has its
own root, `/var/www/onyx`, precisely so that a site deploy cannot reach them. `deploy.sh` only ever
uses `--delete` against `/var/www/onyx` and `/opt/onyx-web`.

## Guide data is generated, not written

`build_data.py` reads the repo's own content files, so a guide page cannot drift from the game:

- **Drop tables** — the TOML shards in `content/custom/drop-tables` (1,027 monsters). Rates are
  rendered as `1/N`; the raw weights use denominators from 512 to 10,000,000 and are unreadable.
- **Teleports** — parsed out of `TeleportTable.kt`. It is a regex over Kotlin source, so it exits
  non-zero if it ever matches nothing rather than publishing an empty page.
- **Features and skills** — the directory listing under `content/`.

Item names come from symbol names (`wolf_bones` → "Wolf bones"). The real display names live in the
cache and need a booted server to decode, so a handful of awkward cases are corrected by the
`OBJ_NAME_FIXES` and `RUNE_PREFIXES` tables in that script.

## Not done

- **The client download.** `play.html` has a placeholder. `tools/client/build_client.py` produces a
  directory of jars, not an installer, and `docs/CLIENT.md` is clear that patched Jagex bytecode
  should not be published — so nothing is hosted until the installer exists.
- No account registration on the site; accounts auto-create at the login screen.
- No vote, store or donation pages.
