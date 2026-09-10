// The simple editor's widget catalogue: every widget Dylan can place, what he can set on it, its
// defaults, and the starting templates. compile.js turns these into components; nothing here
// knows about components.

export const FORMAT = 1;

/** Below the steelborder title bar, and in from the frame's edges; see TeleportPanelLayout. */
export const CONTENT_TOP = 36;
export const INSET = 8;

export const PANEL_SIZES = {
  small: { label: 'Small', w: 300, h: 200 },
  medium: { label: 'Medium', w: 400, h: 260 },
  large: { label: 'Large', w: 488, h: 320 },
};

/** Text colours offered as swatches. Everything but black gets the vanilla drop shadow. */
export const COLOURS = {
  orange: 'ff981f',
  white: 'ffffff',
  yellow: 'ffff00',
  green: '0dc10d',
  red: 'ff0000',
  black: '000000',
};

/**
 * Button looks. Stone is the slayer_rewards inset box and stretches to any size; the others are
 * vanilla button sprites, which only draw at their own size.
 */
export const BUTTON_STYLES = {
  stone: { label: 'Stone', part: 'inset', w: 120, h: 24 },
  grey: { label: 'Grey', part: 'button_85', sprite: 812, w: 85, h: 22 },
  big: { label: 'Big grey', part: 'button_72', sprite: 295, w: 72, h: 36 },
  dark: { label: 'Dark', part: 'button_58', sprite: 5779, w: 58, h: 24 },
  wide: { label: 'Wide', part: 'button_132', sprite: 1701, w: 132, h: 28 },
};

/** resize: which edges Dylan can drag. Everything else about a widget is fixed by its type. */
export const WIDGETS = {
  button: {
    label: 'Button', hint: 'Something to click', resize: 'both',
    defaults: { w: 120, h: 24, style: 'stone', label: 'Button', action: '' },
  },
  heading: {
    label: 'Heading', hint: 'Big orange title text', resize: 'x',
    defaults: { w: 160, h: 16, text: 'Heading', align: 'left' },
  },
  text: {
    label: 'Text', hint: 'A line or paragraph of text', resize: 'x',
    defaults: { w: 200, h: 14, text: 'Some text', colour: 'orange', align: 'left' },
  },
  tabs: {
    label: 'Tabs', hint: 'A row of tabs, like the teleport menu', resize: 'x',
    defaults: { w: 360, h: 20, labels: ['Tab 1', 'Tab 2', 'Tab 3'], action: '' },
  },
  grid: {
    label: 'Grid', hint: 'Rows and columns of buttons or item slots', resize: 'both',
    defaults: { w: 300, h: 96, rows: 3, cols: 3, cells: 'buttons', labels: [], action: '' },
  },
  icon: {
    label: 'Icon', hint: 'Any picture from the game', resize: 'none',
    defaults: { w: 26, h: 23, sprite: 535 },
  },
  item: {
    label: 'Item slot', hint: 'Shows an item; the server picks which', resize: 'none',
    defaults: { w: 36, h: 32, item: 'Rune scimitar', action: '' },
  },
  box: {
    label: 'Box', hint: 'A dark area to group things', resize: 'both',
    defaults: { w: 200, h: 100 },
  },
};

export const ITEM_SIZE = { w: 36, h: 32 };
export const GRID_GAP = 4;
export const TAB_HEIGHT = 20;

export function resizeAxes(widget) {
  if (widget.type === 'button') return widget.style === 'stone' ? 'both' : 'none';
  return WIDGETS[widget.type].resize;
}

/** The first free `<type>_<n>` id in a panel. */
export function nextId(panel, type) {
  const taken = new Set(panel.widgets.map((w) => w.id));
  for (let n = 1; ; n++) if (!taken.has(`${type}_${n}`)) return `${type}_${n}`;
}

/** A new widget of `type` at x, y, with the type's defaults. */
export function makeWidget(panel, type, x, y, extra = {}) {
  return { id: nextId(panel, type), type, x, y, ...structuredClone(WIDGETS[type].defaults), ...extra };
}

/** An interface name from a title: "Bank tabs!" becomes bank_tabs, unique among `taken`. */
export function interfaceName(title, taken = new Set()) {
  const base = title.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '') || 'panel';
  if (!taken.has(base)) return base;
  for (let n = 2; ; n++) if (!taken.has(`${base}_${n}`)) return `${base}_${n}`;
}

const at = (type, x, y, extra) => ({ type, x, y, ...extra });

/**
 * Starting points, so nobody faces a blank page. Positions are inside the frame's content area:
 * x from 8, y from 36.
 */
export const TEMPLATES = [
  {
    id: 'blank', label: 'Blank', hint: 'An empty panel with a title',
    size: 'medium', widgets: [],
  },
  {
    id: 'tabbed', label: 'Tabbed list', hint: 'Tabs over a grid of buttons, like the teleport menu',
    size: 'large',
    widgets: [
      at('box', 8, 56, { w: 472, h: 220 }),
      at('tabs', 11, 36, { w: 450, labels: ['Cities', 'Skilling', 'Bosses'], action: 'Shows that tab\'s buttons.' }),
      at('grid', 15, 62, { w: 458, h: 208, rows: 6, cols: 3, labels: ['Varrock', 'Lumbridge', 'Falador'], action: '' }),
      at('button', 15, 284, { w: 150, h: 28, label: 'Home', action: '' }),
    ],
  },
  {
    id: 'shop', label: 'Shop', hint: 'Rows of item slots with a note underneath',
    size: 'large',
    widgets: [
      at('box', 8, 36, { w: 472, h: 236 }),
      at('grid', 20, 44, { w: 448, h: 220, rows: 5, cols: 8, cells: 'items', action: 'Right-click to buy.' }),
      at('text', 8, 284, { w: 472, h: 14, text: 'Right-click an item to buy it.', colour: 'orange', align: 'centre' }),
    ],
  },
  {
    id: 'confirm', label: 'Yes / No', hint: 'A question with two buttons',
    size: 'small',
    widgets: [
      at('text', 8, 70, { w: 284, h: 14, text: 'Are you sure? This cannot be undone.', colour: 'orange', align: 'centre' }),
      at('button', 40, 130, { w: 100, h: 28, label: 'Yes', action: '' }),
      at('button', 160, 130, { w: 100, h: 28, label: 'No', action: 'Closes the panel.' }),
    ],
  },
];

/** A new panel document from a template. */
export function newPanel(title, templateId, taken = new Set()) {
  const template = TEMPLATES.find((t) => t.id === templateId) ?? TEMPLATES[0];
  const size = PANEL_SIZES[template.size];
  const panel = {
    format: FORMAT,
    kind: 'panel',
    interface: interfaceName(title, taken),
    title,
    w: size.w,
    h: size.h,
    notes: '',
    widgets: [],
  };
  for (const { type, x, y, ...extra } of template.widgets) {
    panel.widgets.push(makeWidget(panel, type, x, y, structuredClone(extra)));
  }
  return panel;
}
