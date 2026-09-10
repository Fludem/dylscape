# interface-designer

A visual editor for server-authored interfaces. You draw a panel with the real sprites and fonts
from `.data/cache/game`; Save writes a design file; that file *is* the builder input, so what you
drew is exactly what `packCache` puts in the cache. Claude only writes the behaviour.

```bash
tools/interface-designer/designer.sh          # then open http://127.0.0.1:8098/
```

The first run exports the cache art into `assets/` (about 6,400 sprites and 15 fonts, 27 MB,
gitignored). Re-export with `designer.sh export` after the cache changes. The export and import
halves compile against the installDist jars, so run `./gradlew installDist` (or
`tools/local/dev.sh restart --build`) first on a fresh checkout.

## What a design can be

Only what rev 233 is known to render from a server-authored interface. The editor offers nothing
else, and the Kotlin loader refuses anything else at boot:

- **Four component types.** Layer (an invisible box that holds others), rectangle, text, sprite.
- **Existing art only.** Any sprite or font already in the cache; no new images or fonts.
- **Fixed pixel layout** inside the 512x334 modal area, positioned from an edge or centred, sized
  fixed or "parent minus". Children are clipped to their layer, as in the client.
- **Client-side hover, one per component.** Text can change colour, rectangles and sprites can
  change transparency, sprites can swap to another sprite. These run vanilla clientscripts, so they
  cost no packets.
- **Right-click ops baked in.** Up to five ops and a target text per component. What an op *does*
  is server code, written from your notes.
- **The steelborder frame.** Stone background, steel edges, orange title and a close button that
  works client-side, as on 127 vanilla panels.

Anything that changes at runtime (a label's text, which tab is selected, hiding a slot) is authored
in its starting state; the server changes it with `ifSetText` / `ifSetHide`. "Starts hidden" plus
"Show hidden" in the toolbar is how to lay out things that appear later.

## Using the editor

| Where | What |
|---|---|
| Add | Basic shapes, and vanilla parts (frame, content area, inset button, dark well, tab, sprite buttons) that insert several components already dressed and hooked. New components go inside the selected layer. |
| Components | The tree, in child-index order (which is also draw order: later draws on top). Drag a row onto a layer to move it inside. `used` marks names Kotlin refers to. |
| Canvas | Click to select, click again to reach what is underneath. Drag to move, handles to resize. Arrows nudge, Shift+arrows by 10. Esc selects the parent. |
| Properties | Position and size, look, hover, right-click menu, and **behaviour notes**: say what clicking should do, when things appear, what text the server fills in. |
| Checks | Live problems: text wider than its box, a non-tiling sprite in a box of a different size (stretching is unproven, so it draws at natural size), anything outside the modal area, missing art. |
| Toolbar | Preview hover, show hidden, zoom, undo/redo (Ctrl+Z / Ctrl+Shift+Z), Ctrl+D duplicate, Ctrl+S save, Export PNG. |

## Handing a design to Claude

1. Design it, write the notes (the design-level notes: what the panel is for and how it opens), save.
   Drafts land in `designs/<interface>.interface.json`.
2. Ask Claude to "implement designs/<interface>.interface.json".

What Claude then does:

1. Allocates the next id in `.data/symbols/.local/interface.sym` and moves the file into the owning
   content module's `src/main/resources/<package path>/`.
2. Writes the component symbols with `python3 tools/interface-designer/serve.py --sync-symbols
   <file>` (saving it once through the editor does the same).
3. Adds the builder, which is one line:
   `object BankTabsBuilder : DesignedComponentBuilder("bank_tabs.interface.json")`
   (`api/type/type-builders/.../comp/DesignedComponentBuilder.kt`), then a `*Components` reference
   object and the script, from the notes.
4. Tests, `./gradlew packCache` with the server stopped, and a look in the real client.

After that a restyle is edit, save, `packCache`. Saving an **implemented** design (one in a content
module whose interface has an id) rewrites that interface's block of
`.data/symbols/.local/component.sym`, because moving a layer renumbers every child after it. Drafts
never touch symbols. Renaming or deleting a component Kotlin uses asks first: it breaks boot until
the script changes.

## The design file

One component per line, so a moved button is a one-line diff. Array order is child-index order;
index 0 is the root, and a parent always comes before its children. Defaults are left out.

| Key | Meaning | Default |
|---|---|---|
| `name`, `parent` | Symbol name (`[a-z0-9_]`), and the parent's name (absent on the root) | |
| `type` | `layer`, `rect`, `text`, `graphic` | |
| `x`, `y`, `w`, `h` | Pixels | 0 |
| `xMode`, `yMode` | `start` (from left/top), `centre`, `end` (from right/bottom) | `start` |
| `wMode`, `hMode` | `fixed`, or `minus` (parent size minus the value) | `fixed` |
| `hidden` | Starts hidden | false |
| `clickThrough` | false swallows clicks (set on the root, so clicks do not reach the game world) | true |
| `frame` | Layer only: steelborder frame with this title | |
| `colour` | Rect and text, six hex digits | `000000` |
| `filled`, `trans` | Rect fill; transparency 0 (solid) to 255 (invisible), also on graphics | false, 0 |
| `sprite`, `tiling` | Graphic sprite id; whether it repeats to fill | |
| `text`, `font`, `alignH`, `alignV`, `lineHeight`, `shadow` | Text; `<br>` and `<col=rrggbb>` work | |
| `hover` | One of `{"colour": ...}` (text), `{"trans": n}` (rect, graphic), `{"sprite": n}` (graphic) | |
| `ops`, `opBase` | Right-click ops (op 1 is left-click) and the target text | |
| `notes` | Behaviour for whoever implements it; never packed | |

`serve.py` validates and normalises every save; `DesignedComponentBuilder` validates again at boot.
The three must agree on defaults: `DEFAULTS` in `serve.py` and `app/render.js`, and the data class
defaults in `DesignedComponentBuilder.kt`.

## Opening our existing panels

```bash
tools/interface-designer/designer.sh import org.rsmod.content.interfaces.firstlogin.configs.FirstLoginBuilder
EXTRA_CP=content/custom/teleports/build/classes/kotlin/main \
    tools/interface-designer/designer.sh import org.rsmod.content.custom.teleports.configs.TeleportPanelBuilder
```

`DesignImport` turns a hand-written builder's components back into a design, strictly: a field the
format cannot express is an error, not a silent drop. `designs/teleport_panel` and
`designs/first_login_setup` were imported this way, and loading them through
`DesignedComponentBuilder` gives components equal, field for field, to the Kotlin builders'. They are
drafts: those panels are still packed from Kotlin until one is restyled here and switched over.

## Files

| File | Does |
|---|---|
| `designer.sh` | Export, import and serve |
| `serve.py` | Stdlib HTTP server: the editor, the art, reading and saving designs, symbol sync. `test_serve.py` tests it |
| `app/` | The editor. Vanilla JS, no build step. `render.js` is a port of `tools/interface-mockup/PanelMockup.java` plus layer clipping; `parts.js` is the parts palette |
| `export/DesignerExport.java` | Sprites and fonts out of the cache (reuses `tools/interface-mockup/SpriteDump.java`) |
| `export/DesignImport.java` | A Kotlin `ComponentBuilder` into a design file |
| `designs/` | Drafts |

The renderer is faithful for layout, sprites, fonts and hover, but it does not run clientscripts
other than the steelborder frame. Check the real client before calling a panel done.
