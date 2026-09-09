#!/usr/bin/env bash
#
# Build a double-clickable client: patched RuneLite + launcher + bundled JRE.
#
#   tools/client/package.sh                 # native installer for this OS
#   tools/client/package.sh app-image       # unpacked app dir (faster; for testing)
#
# jpackage cannot cross-compile: run this on the OS you are targeting.
# See docs/CLIENT.md.
set -euo pipefail

APP="Onyx"
VERSION="${APP_VERSION:-1.0.0}"
TYPE="${1:-}"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BUILD="$ROOT/build/client-package"
BUNDLE="$BUILD/bundle"     # every jar that lands on the classpath
RUNTIME="$BUILD/jre"
OUT="$BUILD/out"

JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 21 2>/dev/null || echo "")}"
[ -n "$JAVA_HOME" ] || { echo "set JAVA_HOME to a JDK 21+ (needs jlink and jpackage)"; exit 1; }
JAVAC="$JAVA_HOME/bin/javac"; JAR="$JAVA_HOME/bin/jar"
JLINK="$JAVA_HOME/bin/jlink"; JPACKAGE="$JAVA_HOME/bin/jpackage"

case "$(uname -s)" in
  Darwin) PLATFORM=macos; [ "$(uname -m)" = arm64 ] && ARCH=aarch64 || ARCH=amd64
          # RuneLite's own clientJvm17MacArguments; the add-opens is mandatory or startup
          # dies in OSXUtil.tryEnableFullscreen with IllegalAccessError.
          JVM_ARGS=(--java-options "-XX:+DisableAttachMechanism" --java-options "-Xmx768m" \
                    --java-options "-Xss2m" --java-options "-XX:CompileThreshold=1500" \
                    --java-options "--add-opens=java.desktop/com.apple.eawt=ALL-UNNAMED" \
                    --java-options "-Dapple.awt.application.name=$APP")
          DEFAULT_TYPE=dmg
          EXTRA=(--mac-package-identifier "uk.co.onyxleeds.rsps") ;;
  *)      PLATFORM=win; ARCH=amd64
          JVM_ARGS=(--java-options "-XX:+DisableAttachMechanism" --java-options "-Xmx768m" \
                    --java-options "-Xss2m" --java-options "-XX:CompileThreshold=1500")
          DEFAULT_TYPE=msi
          EXTRA=(--win-shortcut --win-menu --win-per-user-install --win-dir-chooser) ;;
esac
TYPE="${TYPE:-$DEFAULT_TYPE}"

echo "==> patching RuneLite for $PLATFORM/$ARCH"
rm -rf "$BUNDLE" "$RUNTIME" "$OUT"
python3 "$ROOT/tools/client/build_client.py" --platform "$PLATFORM" --arch "$ARCH" --out "$BUNDLE"

echo "==> compiling launcher"
CLIENT_JAR="$(ls "$BUNDLE"/client-*.jar | head -1)"
mkdir -p "$BUILD/classes"
"$JAVAC" -d "$BUILD/classes" -cp "$CLIENT_JAR" \
    "$ROOT/tools/client/launcher/src/net/onyx/launcher/Main.java"
"$JAR" --create --file "$BUNDLE/onyx-launcher.jar" \
    --main-class net.onyx.launcher.Main -C "$BUILD/classes" .

echo "==> building runtime image"
"$JLINK" --add-modules java.base,java.datatransfer,java.desktop,java.logging,java.management,\
java.naming,java.net.http,java.prefs,java.scripting,java.sql,java.xml,jdk.crypto.ec,\
jdk.crypto.cryptoki,jdk.unsupported,jdk.zipfs,jdk.management,jdk.httpserver,\
java.instrument \
    --strip-debug --no-header-files --no-man-pages --compress=zip-6 --output "$RUNTIME"

echo "==> jpackage --type $TYPE"
mkdir -p "$OUT"
"$JPACKAGE" --type "$TYPE" --name "$APP" --app-version "$VERSION" \
    --input "$BUNDLE" --main-jar onyx-launcher.jar --main-class net.onyx.launcher.Main \
    --runtime-image "$RUNTIME" --dest "$OUT" \
    --vendor "Onyx" --description "Onyx RSPS client" \
    "${JVM_ARGS[@]}" "${EXTRA[@]}"

echo
echo "built:"; ls -1 "$OUT"
