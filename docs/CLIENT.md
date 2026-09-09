# The friend-facing client

Friends do **not** need RSProx, a proxy, a loopback alias, or a cache. The client is stock
RuneLite 1.11.19 with four byte-level patches, connecting straight to `rsps.onyxleeds.co.uk:43594`.
The ~190 MB cache streams from the server's own JS5 file server on first launch.

Proven working 2026-09-09: logged in and played over the internet, RSProx not running.

## Build

```bash
python3 tools/client/build_client.py --platform macos --arch aarch64 --out build/client
python3 tools/client/build_client.py --platform win  --arch amd64    --out build/client-win
```

It downloads the pinned artifacts, verifies every SHA-256 against the bootstrap, patches, and
writes a directory of jars. Reads the modulus straight from `.data/client.key` so it cannot
drift from the server.

## Run

```bash
java -Duser.home="$HOME/Library/Application Support/OnyxRSPS" \
     -XX:+DisableAttachMechanism -Xmx768m -Xss2m -XX:CompileThreshold=1500 \
     --add-opens=java.desktop/com.apple.eawt=ALL-UNNAMED \
     -cp "build/client/*" net.runelite.client.RuneLite \
     --jav_config=https://rsps.onyxleeds.co.uk/jav_config.ws --disable-telemetry
```

- **`--add-opens=java.desktop/com.apple.eawt=ALL-UNNAMED` is required on macOS.** Without it
  startup dies in `OSXUtil.tryEnableFullscreen` with `IllegalAccessError`. The JVM flags come
  from the bootstrap's `clientJvm17MacArguments`; use `clientJvm17Arguments` elsewhere.
- **`-Duser.home` matters.** `RuneLite.RUNELITE_DIR` is `new File(System.getProperty("user.home"), ".runelite")`
  with no override property, so without this our client shares config, profiles, plugins and
  the game cache with a friend's real RuneLite.

## Why everything is pinned

RuneLite tracks live OSRS. Our server is frozen at rev 233, so a newer RuneLite would be
rejected at login (`OutOfDateReload`) — on Jagex's schedule, for everyone at once, with no
action from us. `BOOTSTRAP_COMMIT` in the build script pins RuneLite 1.11.19, the build that
matches rev 233. RSProx keeps the rev→commit map in `ProxyService.kt:336`.

Consider mirroring the 40 artifacts to our own host; they are the only build that will ever
match this server.

## The four patches

Each target constant occurs **exactly once** in the pinned build, and the script asserts that —
if a future RuneLite moves one, the build fails rather than shipping a dud.

**`injected-client-1.11.19.jar`** (the vanilla client; unsigned, so plain byte edits are fine):

| Patch | Why |
|---|---|
| RSA modulus in `bc.class` | The only class holding `10001`, and the only 256-char lowercase-hex run in the jar. Ours is also 256 chars, so it is a same-length splice with no constant-pool resize. Without it the server cannot decrypt the login block |
| `127.0.0.1` → `""` in `bl.class` | The only occurrence. The client validates the world host with `String.endsWith` against `.runescape.com`/`.jagex.com`; `endsWith("")` is always true, so any hostname is accepted. Required for a custom domain |
| port `43594` | **Not patched.** The server binds 43594 deliberately (see `docs/DEPLOY.md`). It is a `CONSTANT_Integer` in exactly 3 classes if you ever need it |

**`client-1.11.19.jar`** (RuneLite itself; **signed**):

| Patch | Why |
|---|---|
| `.jagex.com` → `""` in `ClientLoader.class` | `downloadConfig()` rejects any `--jav_config` host not ending `.jagex.com`/`.runescape.com`. **Stock RuneLite refuses a custom config host** — without this it throws `IllegalArgumentException` before the login screen |
| drop `META-INF/RL.SF` + `RL.RSA`, reset `MANIFEST.MF` | The jar is signed with per-entry SHA-384 digests. Edit any entry without this and the JVM throws `SecurityException: signer information does not match`. Do not instead shadow the class from an earlier classpath entry — mixed signers in one package throw too |

**No gamepack is downloaded.** `ClientLoader.loadClient` does
`getClass().getClassLoader().loadClass(config.getInitialClass()).newInstance()`, so the client
comes off our classpath. Nothing is fetched from Jagex at runtime, which is what makes a
build-time patch self-contained.

## Harmless noise

- `WorldService - Error looking up worlds` and `ItemClient` 404s against `api.runelite.net` —
  RuneLite phoning home. Ignorable.
- `injected-client - Mismatch in overlaid cache archive hash for 12/...` — RuneLite checks its
  bundled cache overlay against the live cache; ours is modified, so mismatches are expected.
- The **world switcher** will list real OSRS worlds and clicking hop breaks the session. Tell
  friends not to use it. Fixing it properly means patching `WorldClient.lookupWorlds` to read a
  `worlds.js` we serve; do **not** just repoint `runelite.api.base`, which also serves session,
  telemetry, loot tracker and XP endpoints.

## Packaging

`tools/client/package.sh` produces a double-clickable installer: patched RuneLite + a launcher
+ a jlink'd JRE, so **friends install nothing else**.

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
  tools/client/package.sh            # native installer (.dmg here, .msi on Windows)
tools/client/package.sh app-image    # unpacked app dir; faster, for testing
```

Output lands in `build/client-package/out`. Sizes: 69 MB app, 52 MB `.dmg`.

The launcher (`tools/client/launcher/.../Main.java`) exists for one essential reason:
`RuneLite.RUNELITE_DIR` is `new File(System.getProperty("user.home"), ".runelite")` with no
override property. `Main` resolves a per-user data dir (`~/Library/Application Support/Onyx`,
`%LOCALAPPDATA%\Onyx`), points `user.home` at it, *then* invokes RuneLite reflectively so the
property is set before RuneLite's class initialiser runs. Verified: config, profiles and the
game cache land under that dir and `~/.runelite` is untouched. It also writes
`launcher-error.log` and shows a dialog on failure, so a friend has something to send you.

`DATA_DIR` is resolved once in a static initialiser, deliberately — recomputing it after
`user.home` has been reassigned nests a second data dir inside the first.

### CI

`.github/workflows/client.yml` builds all three on `workflow_dispatch`: `windows-latest` (.msi,
needs WiX via choco), `macos-14` (arm64 .dmg), `macos-13` (Intel .dmg — drop that job if nobody
needs it). jpackage cannot cross-compile, hence one runner per target. A bundled arm64 JRE will
not run on an Intel Mac.

It needs one repo secret, **`ONYX_MODULUS`** — the 256-hex modulus from `.data/client.key`,
which is gitignored. That is the *public* half, so it is safe in a secret; `.data/game.key`
must never leave the server. `build_client.py` prefers `$ONYX_MODULUS` and falls back to
`.data/client.key` locally.

**Run this on a private repo** — the artifacts contain patched Jagex bytecode.

### jlink modules

The module list in `package.sh` is minimal and was arrived at by failure: RuneLite needs
`jdk.httpserver` (else `NoClassDefFoundError: com/sun/net/httpserver/HttpServer` at startup),
plus `jdk.crypto.ec` for TLS and `jdk.unsupported` for `sun.misc.Unsafe`. If a new plugin
breaks, widen it — or fall back to `--add-modules ALL-MODULE-PATH` and accept ~35 MB.

## Still to do

1. **macOS Gatekeeper.** The `.dmg` is unsigned, so a friend gets "damaged and can't be opened"
   and must go to System Settings → Privacy & Security → "Open Anyway". A $99 Apple Developer
   ID plus `--mac-sign`, `notarytool submit --wait` and `stapler staple` removes that entirely.
   Windows SmartScreen shows a "More info → Run anyway" prompt; not worth a cert for five
   friends.
2. **Icons.** No `--icon` is passed, so both platforms use the stock Java icon.
3. **Host the installers privately** and write the one-page friend README: Gatekeeper steps,
   "first login downloads ~190 MB", "don't use the world switcher", "a disconnect means a full
   re-login".
4. **Self-hosted updates** so a client fix does not need a new installer — a `manifest.json` on
   the VPS listing version + per-jar SHA-256, which `Main` checks at startup.
