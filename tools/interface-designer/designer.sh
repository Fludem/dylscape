#!/usr/bin/env bash
# The interface designer. See README.md.
#
#   designer.sh                       export the cache art if it is missing, then serve on :8098
#   designer.sh export                re-export the cache art (after the cache changes)
#   designer.sh import <BuilderClass> [--force]
#                                     write designs/<interface>.interface.json from a Kotlin builder
#                                     (EXTRA_CP=<module>/build/classes/kotlin/main if the builder
#                                     is newer than the last installDist)
#
# Like tools/interface-mockup, the Java halves compile against the installDist jars and read
# .data/cache/game directly: no Gradle, no server boot.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
LIB="$REPO/server/app/build/install/app/lib"
CLASSES="$HERE/build/classes"

compile() {
    if [[ ! -d "$LIB" ]]; then
        echo "No installDist at $LIB." >&2
        echo "Run ./gradlew installDist (or tools/local/dev.sh restart --build) first." >&2
        exit 1
    fi
    mkdir -p "$CLASSES"
    javac -nowarn -cp "$LIB/*" -d "$CLASSES" \
        "$REPO/tools/interface-mockup/SpriteDump.java" "$HERE"/export/*.java
}

export_assets() {
    compile
    rm -f "$HERE/assets/sprites.json" "$HERE/assets/fonts.json"
    # openrs2 throws from its channel close at exit, after the export is complete, so success is
    # judged by the index files existing and stderr only surfaces when they do not.
    java -cp "$LIB/*:$CLASSES" DesignerExport \
        "$REPO/.data/cache/game" "$REPO/.data/symbols/font.sym" "$HERE/assets" \
        2> "$HERE/build/export.log" || true
    [[ -f "$HERE/assets/sprites.json" && -f "$HERE/assets/fonts.json" ]] || {
        cat "$HERE/build/export.log" >&2
        echo "Export failed: assets/sprites.json or fonts.json is missing." >&2
        exit 1
    }
}

case "${1:-serve}" in
    serve)
        [[ -f "$HERE/assets/sprites.json" ]] || export_assets
        shift $(( $# > 0 ? 1 : 0 ))
        exec python3 "$HERE/serve.py" "$@"
        ;;
    export)
        export_assets
        ;;
    import)
        [[ $# -ge 2 ]] || { echo "usage: designer.sh import <BuilderClass> [--force]" >&2; exit 1; }
        compile
        tmp="$(mktemp)"
        trap 'rm -f "$tmp"' EXIT
        # A builder newer than the last installDist: EXTRA_CP=<module>/build/classes/kotlin/main.
        java -cp "${EXTRA_CP:+$EXTRA_CP:}$LIB/*:$CLASSES" DesignImport "$2" \
            "$REPO/.data/symbols/.local/component.sym" > "$tmp"
        python3 "$HERE/serve.py" --import "$tmp" "${@:3}"
        ;;
    *)
        echo "usage: designer.sh [serve [--port N] | export | import <BuilderClass> [--force]]" >&2
        exit 1
        ;;
esac
