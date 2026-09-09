# Deploying to the public server

The live server for friends. Everything here has been done once already and works — this is
the runbook for repeating it, not a proposal.

| | |
|---|---|
| Host | `rsps.onyxleeds.co.uk` (`2.28.73.211`), Ubuntu 26.04, 4 vCPU, 7.6 GB RAM |
| Access | `ssh root@2.28.73.211` (key auth) |
| Game + JS5 | TCP **43594** |
| Config files | `https://rsps.onyxleeds.co.uk/{jav_config.ws,worldlist.ws}` via Caddy |
| Install root | `/opt/rsmod` (user `rsmod`, `.data` relative to it) |
| Service | `systemctl {status,restart,stop} rsmod`, log at `/opt/rsmod/server.log` |

## The two rules

1. **Never regenerate `.data/game.key`.** Every client ever handed out has the matching
   modulus baked in; a new keypair bricks all of them at once with no error a player can
   act on. It is not in git (`.data` is gitignored). **Keep an off-machine backup.**
   `GameNetworkRsaGenerator` skips generation when the file exists, but `cleanInstall`
   deletes it and a fresh checkout mints a new one.
2. **Never run the public server on the `dev` realm.** `dev` has `dev_mode=1`, which means
   `AccountLoadResponseHook` both skips password checks and hands every login **owner**
   (`::master`, `::tele`, `::invadd`). See "Realm" below.

Also worth backing up: `/opt/rsmod/.data/saves/game.db` — every account, character, stat and
inventory, in one file, with no other copy.

## First-time setup

```bash
ssh root@2.28.73.211
apt-get update && apt-get install -y openjdk-21-jre-headless rsync sqlite3 ufw caddy
useradd -r -m -d /opt/rsmod -s /usr/sbin/nologin rsmod
mkdir -p /opt/rsmod/app /opt/rsmod/.data /var/www/rsps && chown -R rsmod:rsmod /opt/rsmod
```

### Upload

The cache is **not** uploaded — the server downloads vanilla from openrs2 on first boot
(~17 s) and repacks `enriched`/`game`/`js5` itself. Only these go up:

```bash
./gradlew :server:app:installDist
rsync -az --delete server/app/build/install/app/     root@2.28.73.211:/opt/rsmod/app/
rsync -az .data/game.key .data/client.key            root@2.28.73.211:/opt/rsmod/.data/
rsync -az .data/symbols/                             root@2.28.73.211:/opt/rsmod/.data/symbols/
# the installer copies this out of the source tree, which is not deployed:
rsync -az server/logging/src/main/resources/logback.xml \
          server/logging/src/main/resources/logback.novice.xml \
          root@2.28.73.211:/opt/rsmod/server/logging/src/main/resources/
ssh root@2.28.73.211 chown -R rsmod:rsmod /opt/rsmod
```

> **The logback step is not optional.** `GameServerLogbackCopy` resolves
> `./server/logging/src/main/resources/` relative to the CWD and hard-fails with
> `FileNotFoundException: Source logback file logback.novice.xml not found` if it is absent.
> It skips silently once `logback.xml` exists there.

### `.data/server.toml`

```toml
realm = 'main'
world = 1
first_launch = false
port = 43594
```

43594 is the port the client hard-codes, so binding it means the client needs no port patch.
The laptop stays on 43595 so local integration tests never collide with the VPS.

### systemd

```ini
[Unit]
Description=RS Mod game server (rev 233)
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=rsmod
Group=rsmod
WorkingDirectory=/opt/rsmod
Environment=JAVA_OPTS=-Xms512m -Xmx4g
ExecStart=/opt/rsmod/app/bin/app
Restart=on-failure
RestartSec=15
SuccessExitStatus=143
StandardOutput=append:/opt/rsmod/server.log
StandardError=append:/opt/rsmod/server.log

[Install]
WantedBy=multi-user.target
```

**`WorkingDirectory` is load-bearing.** Every `.data` path is relative to the CWD and
`NetworkFactory` hard-codes `Paths.get(".data", "game.key")`. Started from the wrong
directory the server quietly builds a *new* keypair and every client breaks.

`-Xmx4g` because `Js5Store` holds the entire client cache (~190 MB) resident on top of the
game cache and map data.

### Realm

The first boot creates `game.db` and comes up **fail-closed** — `main` ships with
`require_registration=1`, so nobody can log in. Then:

```sql
UPDATE realms SET
    require_registration      = 0,   -- auto-create accounts on first login
    ignore_passwords          = 0,   -- and actually verify them (Argon2)
    dev_mode                  = 0,   -- no free owner, no password bypass
    auto_assign_display_names = 1,
    login_message             = 'Welcome to Onyx.'
WHERE name = 'main';
```

Restart afterwards. `ConnectionHandler.passLogin` then takes the `loadOrCreate` branch, so a
friend just picks a username and password at the login screen — there is no registration site
and none is needed.

XP rate is **not** a realm decision: `realms.player_xp_rate_in_hundreds` is only a default,
and the first-login chooser writes `characters.xp_rate_in_hundreds` per character.

Promote an admin once their account row exists (takes effect on their next login):

```sql
UPDATE accounts SET modlevel = 'owner' WHERE login_username = 'fludems';
```

Valid values come from `.data/symbols/modlevel.sym`: `player`, `moderator`, `admin`, `owner`.

### Caddy and firewall

`/etc/caddy/Caddyfile` — TLS is provisioned automatically:

```
rsps.onyxleeds.co.uk {
	root * /var/www/rsps
	file_server
}
```

Serve `jav_config.ws` + `worldlist.ws` from `/var/www/rsps`, generated from the `tools/local`
originals with three host strings changed:

- `jav_config.ws`: `codebase=https://rsps.onyxleeds.co.uk/`,
  `param=17=https://rsps.onyxleeds.co.uk/worldlist.ws`, `cachedir=onyxrsps`
  (`param=25=233` and `param=12=1` unchanged)
- `worldlist.ws`: regenerate with `make_worldlist.py` and `HOST = "rsps.onyxleeds.co.uk"`

The worldlist `host` **must** equal the `codebase` host.

```bash
ufw allow 22/tcp && ufw allow 80/tcp && ufw allow 443/tcp && ufw allow 43594/tcp && ufw --force enable
```

## Redeploying after a code change

```bash
./gradlew :server:app:installDist
rsync -az --delete server/app/build/install/app/ root@2.28.73.211:/opt/rsmod/app/
ssh root@2.28.73.211 'chown -R rsmod:rsmod /opt/rsmod/app && systemctl restart rsmod'
```

Build from a clean checkout of a known commit — `installDist` bundles whatever is in the
working tree, including another session's uncommitted work.

If the change dirties the cache the server repacks on boot (slow, and it restarts itself).
That also changes the JS5 CRCs, so **every friend re-downloads the changed groups** on their
next login: `ConnectionHandler` compares their 23 archive CRCs against our master index and
answers `OutOfDateReload`. Self-healing, but batch content pushes rather than trickling them.

## Verifying

```bash
# does the game port accept rev 233 from outside?
python3 -c "
import socket,struct
s=socket.create_connection(('rsps.onyxleeds.co.uk',43594),10)
s.sendall(bytes([15])+struct.pack('>i',233)+b'\x00'*16)
print('JS5:', s.recv(8).hex(), '(00 = accepted)')"

curl -sI https://rsps.onyxleeds.co.uk/jav_config.ws | head -1
ssh root@2.28.73.211 'systemctl is-active rsmod caddy; tail -5 /opt/rsmod/server.log'
```

After a login, confirm the realm is actually locked down:

```sql
select login_username, display_name, substr(password_hash,1,10), modlevel from accounts;
```

A real `$argon2i$...` hash and `modlevel = player` for a new account means passwords are being
checked and the dev-realm owner grant is off.

## Known rough edges

- **Reconnect is unimplemented.** `ConnectionHandler.onReconnect` always answers `ConnectFail`,
  so any network blip is a full re-login.
- **First login downloads ~190 MB** over the game port at rsprot's default JS5 throttle —
  several minutes. Raise it via `Js5Configuration` in `NetworkFactory` if it becomes a
  complaint.
- **Private messages are a no-op** (`MessagePrivate` → `NoopMessageHandler`); friends chat is
  server-driven via `content/other/chatchannel/GlobalFriendChat.kt`.

## The client

Friends do **not** need RSProx, and they do not need a cache — the server serves it over JS5.
See `docs/CLIENT.md` for building the patched RuneLite bundle.
