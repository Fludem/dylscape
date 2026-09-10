# interface-mockup

Standalone Java tools for designing server-authored interfaces (`ComponentBuilder`) without a
`packCache` and client restart per tweak. For drawing a new panel by hand, use
`tools/interface-designer/` instead: a visual editor whose saved designs are packed as drawn. Like `tools/npc-spawns/DumpNpcTypes.java`, they compile
against the installed server jars and read `.data/cache/game` directly - no Gradle, no boot.

| File | Does |
|---|---|
| `SpriteDump.java` | Renders cache sprites (js5 archive 8) to PNG: one file per frame, or a labelled contact sheet of an id range. The quickest way to find usable art. |
| `PanelMockup.java` | Renders a `ComponentBuilder`'s components the way the client lays them out: real sprites, real font metrics (archive 13), and an emulation of `[proc,steelborder]` for `onLoad=[227, ...]`. Prints `OVERFLOW` for any label wider than its component. |
| `NamesDump.java` | Prints the `.data/symbols/.local/component.sym` block for a builder that exposes `INTERFACE` and `componentNames` (see `TeleportPanelBuilder`). |

The mockup is an approximation for layout and colour: it does not run clientscripts other than
the steelborder frame, and hover is simulated by applying each hovered component's
`onMouseOver` value. Always check the real client before calling a panel done.

## Usage

```bash
R=$(git rev-parse --show-toplevel)
./gradlew :content:custom:teleports:compileKotlin   # or whichever module holds the builder
CP="$(ls $R/server/app/build/install/app/lib/*.jar | tr '\n' ':')$R/content/custom/teleports/build/classes/kotlin/main:."
cd tools/interface-mockup
javac -cp "$CP" -d /tmp/mockup *.java
CP="$CP:/tmp/mockup"

# A contact sheet of sprites 990-1010, and single sprites at 4x.
java -cp "$CP" SpriteDump $R/.data/cache/game out sheet 990 1010 tabs
java -cp "$CP" SpriteDump $R/.data/cache/game out each 812 813

# The teleport panel with the Cities tab filled in, hovering the second tile.
java -cp "$CP" PanelMockup $R/.data/cache/game out/panel.png \
    org.rsmod.content.custom.teleports.configs.TeleportPanelBuilder \
    teleport_panel_cities.txt slot_1

# The symbol block for the builder.
java -cp "$CP" NamesDump org.rsmod.content.custom.teleports.configs.TeleportPanelBuilder
```

Ignore the `BufferedFileChannel.close` exception at exit; the output is complete.

The state file sets what the server would push at runtime: `text <name> <text>`, `show <name>`,
`hide <name>`, one per line, with names as in the builder.
