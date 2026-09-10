# interface-designer

Lay out an in-game panel with the real sprites and fonts from `.data/cache/game`, and hand it to
Claude to build. What you place is exactly what gets packed into the cache; Claude only writes what
the buttons do, from the notes you leave.

```bash
tools/interface-designer/designer.sh          # then open http://127.0.0.1:8098/
```

The first run exports the cache art into `assets/` (about 6,400 sprites and 15 fonts, 27 MB,
gitignored). Re-export with `designer.sh export` after the cache changes. The export and import
halves compile against the installDist jars, so run `./gradlew installDist` (or
`tools/local/dev.sh restart --build`) first on a fresh checkout.

## Designing a panel

1. Type a name and pick a starting point: Blank, Tabbed list (like the teleport menu), Shop, or
   Yes / No.
2. Add things from the left by clicking or dragging them onto the panel:

   | Widget | You choose |
   |---|---|
   | Button | Label, look (Stone stretches to any size; Grey, Big grey, Dark and Wide are the vanilla button sprites), what clicking does |
   | Heading | Text |
   | Text | Text, colour, left or centred |
   | Tabs | Tab names, one per line |
   | Grid | Rows, columns, and whether each cell is a button (with a name) or an item slot |
   | Icon | Any picture from the game |
   | Item slot | An example item for the picture; the server decides the real one |
   | Box | Nothing: a dark area to group things, always behind everything else |

3. Drag to move (things line up with each other and the panel's centre as you go), drag the blue
   squares to resize, and the orange square at the panel's corner to resize the whole panel.
4. Write what things do in the **When clicked…** boxes, and what the panel is for in the panel's
   own box (click an empty spot to get it). Plain English.

It saves by itself. A red outline means something to fix (a label too long for its button, a
widget off the panel); click it to see what. **Save picture** exports a PNG to share.

Hover effects, fonts, spacing and the frame are decided for you, the way vanilla panels do them.

## Handing a design to Claude

Tell Claude "implement designs/<name>.panel.json". Every save writes two files in `designs/`:

- `<name>.panel.json`: the panel as you laid it out (widgets and notes). This is what you edit.
- `<name>.interface.json`: the same panel compiled into components, which is what gets packed.

What Claude then does:

1. Allocates the next id in `.data/symbols/.local/interface.sym`, and moves both files into the
   owning content module's `src/main/resources/<package path>/`.
2. Renames widgets to say what they are (widget ids become component names that scripts use),
   then saves once through the editor, which writes `component.sym` (or runs
   `python3 tools/interface-designer/serve.py --sync-symbols <interface file>`).
3. Adds the builder, which is one line:
   `object BankTabsBuilder : DesignedComponentBuilder("bank_tabs.interface.json")`
   (`api/type/type-builders/.../comp/DesignedComponentBuilder.kt`), then a `*Components` reference
   object and the script, from the notes. Item slots are filled with `ifSetObj`.
4. Tests, `./gradlew packCache` with the server stopped, and a look in the real client.

After that, a change is edit (it saves itself) and `packCache`. Saving an **implemented** panel
(one in a content module whose interface has an id) rewrites that interface's block of
`.data/symbols/.local/component.sym`, since moving things renumbers components. Drafts never touch
symbols. Deleting a widget the game's code uses asks first: it breaks boot until the script changes.

## The advanced editor

`app/advanced.html` (linked from the simple editor) edits an `.interface.json` directly: every
component, the layer tree, position and size modes, hover hooks, ops. It is for Claude and for
anything widgets cannot express. Opening a panel's compiled design there is fine for a look, but
the next save of the panel overwrites it.

| Where | What |
|---|---|
| Add | Basic shapes, and vanilla parts (frame, content area, inset button, dark well, tab, sprite buttons, item slot) |
| Components | The tree, in child-index order (which is also draw order). Drag a row onto a layer to move it inside |
| Canvas | Click to select, click again to reach what is underneath. Arrows nudge, Esc selects the parent |
| Checks | Text wider than its box, non-tiling sprites in a box of a different size, anything off the modal area |

## The files

### Panel (`.panel.json`)

A header (`format`, `kind: "panel"`, `interface`, `title`, `w`, `h`, `notes`) and one widget per
line: `id`, `type`, `x`, `y`, `w`, `h` relative to the panel's top-left, plus the type's own fields
(`label`, `style`, `text`, `colour`, `align`, `labels`, `rows`, `cols`, `cells`, `sprite`, `item`,
`action`). `app/widgets.js` defines them; `app/compile.js` turns them into components.

### Design (`.interface.json`)

One component per line. Array order is child-index order; index 0 is the root, and a parent always
comes before its children. Defaults are left out.

| Key | Meaning | Default |
|---|---|---|
| `name`, `parent` | Symbol name (`[a-z0-9_]`), and the parent's name (absent on the root) | |
| `type` | `layer`, `rect`, `text`, `graphic`, `item` (an empty slot the server fills with `ifSetObj`) | |
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
The defaults must agree in three places: `DEFAULTS` in `serve.py` and `app/render.js`, and the data
class defaults in `DesignedComponentBuilder.kt`.

## Opening our existing panels

```bash
tools/interface-designer/designer.sh import org.rsmod.content.interfaces.firstlogin.configs.FirstLoginBuilder
EXTRA_CP=content/custom/teleports/build/classes/kotlin/main \
    tools/interface-designer/designer.sh import org.rsmod.content.custom.teleports.configs.TeleportPanelBuilder
```

`DesignImport` turns a hand-written builder back into a design, strictly: a field the format cannot
express is an error, not a silent drop. `designs/teleport_panel` and `designs/first_login_setup`
were imported this way and rebuild field-for-field equal to the Kotlin builders. They open in the
advanced editor only; those panels still pack from Kotlin.

## Code

| File | Does |
|---|---|
| `designer.sh` | Export, import and serve |
| `serve.py` | Stdlib HTTP server: both editors, the art, reading and saving panels and designs, symbol sync. `test_serve.py` tests it |
| `app/index.html`, `app/simple.js` | The simple editor |
| `app/widgets.js`, `app/compile.js` | The widget catalogue and templates, and the panel-to-design compiler. `compile.test.mjs` tests it: `node --test tools/interface-designer/app/compile.test.mjs` |
| `app/advanced.html`, `app/designer.js` | The advanced editor |
| `app/render.js` | The client's layout and drawing, ported from `tools/interface-mockup/PanelMockup.java` plus layer clipping |
| `app/parts.js`, `app/picker.js` | Vanilla parts (also what widgets compile to), and the sprite picker both editors share |
| `export/DesignerExport.java` | Sprites and fonts out of the cache (reuses `tools/interface-mockup/SpriteDump.java`) |
| `export/DesignImport.java` | A Kotlin `ComponentBuilder` into a design file |
| `designs/` | Drafts |

The renderer is faithful for layout, sprites, fonts and hover, but it does not run clientscripts
other than the steelborder frame, and item slots are empty until the server fills them. Check the
real client before calling a panel done.
