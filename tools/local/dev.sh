#!/usr/bin/env bash
#
# Start / stop / restart the whole local stack, per the runbook in README.md.
#
#   ./tools/local/dev.sh              # restart everything (default)
#   ./tools/local/dev.sh start
#   ./tools/local/dev.sh stop
#   ./tools/local/dev.sh status
#   ./tools/local/dev.sh restart --build   # rebuild the server first
#
# The server runs from `installDist`, NOT `./gradlew run`, so no Gradle daemon
# is held while playing -- see the "Memory" section of README.md. That means a
# code change needs `--build` before it takes effect.

set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LOCAL="$ROOT/tools/local"
LOGS="$LOCAL/logs"
RSPROX="${RSPROX_DIR:-$HOME/Coding/Python/rsprox}"

# Homebrew's keg path is not a valid JAVA_HOME; the real one is under libexec.
JDK21="${JDK21:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"

# RSProx addresses world 1 + target 1 as 127.0.1.3. macOS only routes 127.0.0.1.
LO0_ALIAS="127.0.1.3"

# Process match patterns. Specific enough not to catch anything unrelated.
# The server matches on its main class, so `stop` also catches a server someone
# started with `./gradlew run` rather than from installDist.
PAT_SERVER="org.rsmod.server.app.GameServerKt"
PAT_CONFIG="http.server 8080"
PAT_RSPROX="net.rsprox.gui.ProxyToolGuiKt"
PAT_CLIENT="net.runelite.client.RuneLite"

mkdir -p "$LOGS"

# The port RSMod binds lives in .data/server.toml, so follow it rather than
# hardcoding 43595 -- otherwise this script silently waits on the wrong port.
PORT="$(sed -n 's/^[[:space:]]*port[[:space:]]*=[[:space:]]*\([0-9]\{1,\}\).*/\1/p' \
        "$ROOT/.data/server.toml" 2>/dev/null | head -1)"
PORT="${PORT:-43594}"

say()  { printf '\033[1;36m==>\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m warn:\033[0m %s\n' "$*"; }
die()  { printf '\033[1;31merror:\033[0m %s\n' "$*" >&2; exit 1; }

port_up() { lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1; }
proc_up() { pgrep -f "$1" >/dev/null 2>&1; }

# Wait until `cond` succeeds, up to `secs`. Polls twice a second.
wait_for() {
  local label="$1" secs="$2"; shift 2
  local tries=$(( secs * 2 ))
  for ((i = 0; i < tries; i++)); do
    if "$@"; then return 0; fi
    sleep 0.5
  done
  warn "timed out after ${secs}s waiting for $label"
  return 1
}

# TERM, then KILL anything that ignored it.
stop_pattern() {
  local label="$1" pattern="$2"
  proc_up "$pattern" || return 0
  say "stopping $label"
  pkill -f "$pattern" 2>/dev/null
  for ((i = 0; i < 20; i++)); do
    proc_up "$pattern" || return 0
    sleep 0.5
  done
  warn "$label ignored SIGTERM; sending SIGKILL"
  pkill -9 -f "$pattern" 2>/dev/null
  sleep 1
}

ensure_lo0() {
  if ifconfig lo0 2>/dev/null | grep -q "$LO0_ALIAS"; then
    return 0
  fi
  say "adding loopback alias $LO0_ALIAS (needs sudo, does not survive reboot)"
  sudo ifconfig lo0 alias "$LO0_ALIAS" || die "could not add $LO0_ALIAS; client cannot reach the proxy without it"
}

build_server() {
  [ -x "$JDK21/bin/java" ] || die "JDK 21 not found at $JDK21 (override with JDK21=...)"
  say "building server (installDist)"
  ( cd "$ROOT" && JAVA_HOME="$JDK21" ./gradlew :server:app:installDist --console=plain ) \
    || die "server build failed"
  # Don't leave a daemon holding ~300 MB while we play.
  ( cd "$ROOT" && JAVA_HOME="$JDK21" ./gradlew --stop >/dev/null 2>&1 )
}

start_config() {
  if port_up 8080; then
    say "config server already up on 8080"
    return 0
  fi
  say "starting static config server on 127.0.0.1:8080"
  ( cd "$LOCAL" && nohup python3 -m http.server 8080 --bind 127.0.0.1 \
      >"$LOGS/config.log" 2>&1 & )
  wait_for "config server" 15 port_up 8080
}

start_server() {
  local bin="$ROOT/server/app/build/install/app/bin/app"
  [ -x "$bin" ] || { warn "no installDist build found; building one"; build_server; }
  [ -x "$JDK21/bin/java" ] || die "JDK 21 not found at $JDK21 (override with JDK21=...)"

  if port_up "$PORT"; then
    die "port $PORT is already in use -- run '$0 stop' first, or check for a stray test JVM: lsof -nP -iTCP:$PORT -sTCP:LISTEN"
  fi

  say "starting game server (port $PORT)"
  # -Xmx2g: the shipped script asks for -Xms1g and the JVM default max is a
  # quarter of RAM, which lets it balloon. It settles around 1.4-1.9 GB.
  ( cd "$ROOT" && JAVA_HOME="$JDK21" JAVA_OPTS="-Xms256m -Xmx2g" \
      nohup "$bin" >"$LOGS/server.log" 2>&1 & )

  if ! wait_for "server port $PORT" 180 port_up "$PORT"; then
    warn "last 20 lines of $LOGS/server.log:"
    tail -20 "$LOGS/server.log"
    return 1
  fi
  grep -E "Revision:|Bound to ports:" "$LOGS/server.log" | tail -2
}

start_rsprox() {
  [ -d "$RSPROX" ] || die "RSProx checkout not found at $RSPROX (override with RSPROX_DIR=...)"
  [ -x "$JDK21/bin/java" ] || die "JDK 21 not found at $JDK21 (override with JDK21=...)"

  say "starting RSProx GUI"
  # RSProx builds on a JDK 11 toolchain, but its Gradle 8.7 launcher cannot run
  # on the default JDK 26 -- it dies with a bare "* What went wrong: 26.x".
  ( cd "$RSPROX" && JAVA_HOME="$JDK21" \
      nohup ./gradlew proxy --console=plain >"$LOGS/rsprox.log" 2>&1 & )

  if ! wait_for "RSProx window" 240 proc_up "$PAT_RSPROX"; then
    warn "last 20 lines of $LOGS/rsprox.log:"
    tail -20 "$LOGS/rsprox.log"
    return 1
  fi

  # RSProx relaunches the last client on its own if one was running before.
  if wait_for "RuneLite client" 90 proc_up "$PAT_CLIENT"; then
    say "RuneLite launched"
  else
    say "RSProx is up -- press Launch (target 'RS Mod (local)', client 'RuneLite')"
  fi
}

do_stop() {
  stop_pattern "RuneLite client" "$PAT_CLIENT"
  stop_pattern "RSProx GUI"      "$PAT_RSPROX"
  stop_pattern "game server"     "$PAT_SERVER"
  stop_pattern "config server"   "$PAT_CONFIG"
  if port_up "$PORT"; then
    warn "port $PORT is still held by something this script did not start:"
    lsof -nP -iTCP:"$PORT" -sTCP:LISTEN | tail -n +2
  fi
  say "stopped"
}

do_start() {
  ensure_lo0
  start_config
  start_server || return 1
  start_rsprox || return 1
  echo
  say "ready -- log in with any username/password (dev realm ignores both)"
  echo "    logs: $LOGS/{server,rsprox,config}.log"
}

do_status() {
  printf '%-16s %s\n' "config :8080"  "$(port_up 8080          && echo up || echo down)"
  printf '%-16s %s\n' "server :$PORT" "$(port_up "$PORT"       && echo up || echo down)"
  printf '%-16s %s\n' "rsprox :43701" "$(port_up 43701         && echo up || echo down)"
  printf '%-16s %s\n' "runelite"      "$(proc_up "$PAT_CLIENT" && echo up || echo down)"
  printf '%-16s %s\n' "lo0 alias"     "$(ifconfig lo0 2>/dev/null | grep -q "$LO0_ALIAS" && echo up || echo missing)"
}

CMD="restart"
BUILD=0
for arg in "$@"; do
  case "$arg" in
    start|stop|restart|status|build) CMD="$arg" ;;
    --build|-b) BUILD=1 ;;
    -h|--help) sed -n '2,14p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) die "unknown argument: $arg" ;;
  esac
done

case "$CMD" in
  status)  do_status ;;
  build)   build_server ;;
  stop)    do_stop ;;
  start)   [ "$BUILD" -eq 1 ] && build_server; do_start ;;
  restart) do_stop; [ "$BUILD" -eq 1 ] && build_server; do_start ;;
esac
