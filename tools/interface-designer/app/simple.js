// The simple editor: a panel made of widgets. widgets.js says what exists, compile.js what each
// becomes. Nothing here shows components, names, modes or hooks; that is the whole point.
//
// The panel file is saved shortly after every change, together with the design compiled from it,
// so there is no Save button to forget.

import { Assets, CANVAS, CONTAINER, MODAL, inside, layout, render, withDefaults } from './render.js';
import { compile, gridCells, tabLabels } from './compile.js';
import { pickSprite } from './picker.js';
import {
  BUTTON_STYLES, COLOURS, CONTENT_TOP, INSET, ITEM_SIZE, PANEL_SIZES, TEMPLATES, WIDGETS,
  makeWidget, newPanel, nextId, resizeAxes,
} from './widgets.js';

const $ = (selector) => document.querySelector(selector);
const DRAFTS = 'tools/interface-designer/designs/';
const SNAP = 4; // the grid everything lands on
const PULL = 4; // how close an edge must come before it lines up with another
const HANDLE = 8;
const TITLE_BAR = 30; // the steelborder title; nothing should sit above this
const MIN = { w: 16, h: 12 };
const PANEL_MIN = { w: 160, h: 100 };
const FONT = { small: 494, regular: 495, bold: 496 };

const assets = new Assets('/assets/');
const state = {
  path: null,
  panel: null,
  implemented: false,
  referenced: [],
  selected: null,
  undo: [],
  redo: [],
  zoom: 2,
  mouse: null,
  drag: null,
  guides: [],
  problems: new Map(),
  saveTimer: null,
  taken: new Set(),
};
let comps = [];
let lay = null;
let editing = false; // one undo step per focused field

const view = $('#canvas');
const vctx = view.getContext('2d');
const buffer = document.createElement('canvas');
buffer.width = CANVAS.w;
buffer.height = CANVAS.h;
const bctx = buffer.getContext('2d');

function el(tag, props = {}, ...children) {
  const node = document.createElement(tag);
  Object.assign(node, props);
  node.append(...children.filter((c) => c !== null && c !== undefined && c !== false));
  return node;
}

const round = (v) => Math.round(v / SNAP) * SNAP;
const clamp = (v, lo, hi) => Math.max(lo, Math.min(hi, v));

// ---------------------------------------------------------------------------------------------
// Model

const widget = (id) => state.panel.widgets.find((w) => w.id === id);
const selected = () => (state.selected ? widget(state.selected) ?? null : null);

/** Boxes draw first (compile.js puts them behind), then everything in the order added. */
function drawOrder() {
  const ws = state.panel.widgets;
  return [...ws.filter((w) => w.type === 'box'), ...ws.filter((w) => w.type !== 'box')];
}

/** The panel's rect on the 1x canvas. */
const origin = () => lay.rects[0];

function rectOf(w) {
  const o = origin();
  return { x: o.x + w.x, y: o.y + w.y, w: w.w, h: w.h };
}

/** Text widgets are exactly as tall as their lines, so only their width is ever dragged. */
function fitTextHeight(w) {
  if (w.type !== 'heading' && w.type !== 'text') return;
  const font = assets.fonts.get(w.type === 'heading' ? FONT.bold : FONT.regular);
  const lines = w.type === 'text' ? (w.text ?? '').split('\n').length : 1;
  w.h = Math.max(12, lines * (font?.cellH ?? 14));
}

// ---------------------------------------------------------------------------------------------
// History and saving

function snapshot() {
  return JSON.stringify({ panel: state.panel, selected: state.selected });
}

function pushUndo() {
  state.undo.push(snapshot());
  if (state.undo.length > 300) state.undo.shift();
  state.redo = [];
}

function restore(text) {
  const saved = JSON.parse(text);
  state.panel = saved.panel;
  state.selected = saved.selected;
  changed();
  refreshAll();
}

function undo() {
  if (!state.undo.length) return;
  state.redo.push(snapshot());
  restore(state.undo.pop());
}

function redo() {
  if (!state.redo.length) return;
  state.undo.push(snapshot());
  restore(state.redo.pop());
}

/** A structural change: one undo step, then everything redrawn, inspector included. */
function mutate(fn) {
  pushUndo();
  fn();
  changed();
  refreshAll();
}

/** Typing in the inspector: one undo step per field, and the inspector is left alone. */
function edit(fn) {
  if (!editing) {
    pushUndo();
    editing = true;
  }
  fn();
  changed();
  refreshCanvas();
}

function status(text, kind = '') {
  const node = $('#status');
  node.textContent = text;
  node.className = `status ${kind}`;
}

function changed() {
  status('Saving…');
  clearTimeout(state.saveTimer);
  state.saveTimer = setTimeout(save, 600);
}

async function api(method, url, body) {
  const response = await fetch(url, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined,
  });
  const json = await response.json();
  if (!response.ok || json.ok === false) throw new Error(json.error || response.statusText);
  return json;
}

async function save() {
  clearTimeout(state.saveTimer);
  state.saveTimer = null;
  if (!state.panel) return;
  try {
    const result = await api('PUT', `/api/panel?path=${encodeURIComponent(state.path)}`, {
      panel: state.panel,
      design: compile(state.panel),
    });
    status(result.symbolsSynced ? 'Saved (game symbols updated)' : 'Saved', 'good');
  } catch (e) {
    status(`Not saved: ${e.message}`, 'bad');
  }
}

// ---------------------------------------------------------------------------------------------
// Start screen

const thumbs = [];
let thumbTimer = null;

/** Draws a panel, cropped to itself, into `canvas`, with the real renderer. */
function paintThumb(canvas, panel) {
  const components = compile(panel).components.map(withDefaults);
  const l = layout(components, false);
  const tmp = document.createElement('canvas');
  tmp.width = CANVAS.w;
  tmp.height = CANVAS.h;
  render(tmp.getContext('2d'), components, l, assets, null);
  const r = l.rects[0];
  canvas.width = r.w + 4;
  canvas.height = r.h + 4;
  canvas.getContext('2d').drawImage(tmp, r.x - 2, r.y - 2, r.w + 4, r.h + 4, 0, 0, r.w + 4, r.h + 4);
}

function thumb(panel, className = '') {
  const canvas = el('canvas', { className });
  paintThumb(canvas, panel);
  thumbs.push({ canvas, panel });
  return canvas;
}

/** Sprites arrive after the first paint; repaint every thumbnail once they settle. */
function repaintThumbs() {
  clearTimeout(thumbTimer);
  thumbTimer = setTimeout(() => {
    for (const t of thumbs) if (t.canvas.isConnected) paintThumb(t.canvas, t.panel);
  }, 120);
}

async function showStart() {
  await flush();
  state.panel = null;
  $('#editor').hidden = true;
  $('#start').hidden = false;
  document.title = 'Interface designer';
  history.replaceState(null, '', 'index.html');

  const [{ panels }, { designs, interfaces }] = await Promise.all([
    api('GET', '/api/panels'),
    api('GET', '/api/designs'),
  ]);
  // A new panel must not take a name a design or an allocated interface already has.
  state.taken = new Set([...panels, ...designs].map((d) => d.interface).concat(Object.keys(interfaces)));

  const templates = $('#templates');
  templates.innerHTML = '';
  for (const template of TEMPLATES) {
    const card = el('button', { className: 'card', type: 'button' },
      thumb(newPanel(template.label, template.id)),
      el('strong', { textContent: template.label }),
      el('span', { className: 'hint', textContent: template.hint }));
    card.addEventListener('click', () => createPanel(template.id));
    templates.append(card);
  }

  const list = $('#panel-list');
  list.innerHTML = '';
  $('#saved').hidden = !panels.length;
  for (const entry of panels) {
    const card = el('button', { className: 'card', type: 'button' });
    card.addEventListener('click', () => openPanel(entry.path));
    list.append(card);
    api('GET', `/api/panel?path=${encodeURIComponent(entry.path)}`).then(({ panel }) => {
      card.append(
        thumb(panel),
        el('strong', { textContent: panel.title || entry.interface }),
        el('span', {
          className: `badge${entry.implemented ? ' implemented' : ''}`,
          textContent: entry.implemented ? 'in the game' : 'draft',
        }),
      );
    }).catch(() => card.append(el('strong', { textContent: entry.interface })));
  }
  $('#new-title').focus();
}

async function createPanel(templateId) {
  const template = TEMPLATES.find((t) => t.id === templateId);
  const title = $('#new-title').value.trim() || template.label;
  $('#new-title').value = '';
  const panel = newPanel(title, templateId, state.taken);
  for (const w of panel.widgets) fitTextHeight(w);
  enterEditor(`${DRAFTS}${panel.interface}.panel.json`, panel, false, []);
  await save();
}

async function openPanel(path) {
  const data = await api('GET', `/api/panel?path=${encodeURIComponent(path)}`);
  enterEditor(path, data.panel, data.implemented, data.referenced);
}

function enterEditor(path, panel, implemented, referenced) {
  state.path = path;
  state.panel = panel;
  state.implemented = implemented;
  state.referenced = referenced;
  state.selected = null;
  state.undo = [];
  state.redo = [];
  $('#start').hidden = true;
  $('#editor').hidden = false;
  $('#advanced').href = `advanced.html?path=${encodeURIComponent(path.replace(/\.panel\.json$/, '.interface.json'))}`;
  history.replaceState(null, '', `?panel=${encodeURIComponent(path)}`);
  status(implemented ? 'In the game: changes apply after a repack' : 'Draft');
  refreshAll();
}

/** Saves now if a save is waiting, so leaving the editor never drops the last change. */
async function flush() {
  if (state.saveTimer) await save();
}

// ---------------------------------------------------------------------------------------------
// Drawing

function refreshCanvas() {
  comps = compile(state.panel).components.map(withDefaults);
  lay = layout(comps, false);
  state.problems = findProblems();
  $('#panel-title').textContent = state.panel.title;
  document.title = `${state.panel.title} - Interface designer`;
  draw();
  showProblems();
}

function refreshAll() {
  refreshCanvas();
  buildInspector();
}

function draw() {
  if (!state.panel) return;
  const z = state.zoom;
  if (view.width !== CANVAS.w * z) {
    view.width = CANVAS.w * z;
    view.height = CANVAS.h * z;
  }
  render(bctx, comps, lay, assets, state.drag ? null : state.mouse);
  vctx.imageSmoothingEnabled = false;
  vctx.drawImage(buffer, 0, 0, view.width, view.height);

  // Item slots are empty until the server fills them; say what goes there.
  vctx.save();
  vctx.font = `${Math.max(9, 4 * z)}px system-ui, sans-serif`;
  vctx.textAlign = 'center';
  vctx.textBaseline = 'middle';
  for (const w of state.panel.widgets) {
    if (w.type !== 'item' || !w.item) continue;
    const r = rectOf(w);
    vctx.fillStyle = '#000';
    vctx.fillText(w.item, (r.x + r.w / 2) * z + 1, (r.y + r.h / 2) * z + 1, r.w * z - 4);
    vctx.fillStyle = '#fff';
    vctx.fillText(w.item, (r.x + r.w / 2) * z, (r.y + r.h / 2) * z, r.w * z - 4);
  }
  vctx.restore();

  // The edge of the game view the panel opens in.
  vctx.setLineDash([4, 4]);
  vctx.strokeStyle = 'rgba(255,255,255,.3)';
  vctx.strokeRect(CONTAINER.x * z - 0.5, CONTAINER.y * z - 0.5, MODAL.w * z + 1, MODAL.h * z + 1);
  vctx.setLineDash([]);

  for (const id of state.problems.keys()) {
    const w = widget(id);
    if (w) box(rectOf(w), '#ff5a4a', 2);
  }
  if (!state.drag && state.mouse) {
    const w = hitWidget(state.mouse);
    if (w && w.id !== state.selected) box(rectOf(w), 'rgba(63,167,255,.6)', 1);
  }
  const s = selected();
  if (s) {
    box(rectOf(s), '#3fa7ff', 2);
    vctx.fillStyle = '#3fa7ff';
    for (const h of handlesFor(s)) vctx.fillRect(h.x - HANDLE / 2, h.y - HANDLE / 2, HANDLE, HANDLE);
  }
  const ph = panelHandle();
  vctx.fillStyle = '#ff981f';
  vctx.fillRect(ph.x - HANDLE / 2, ph.y - HANDLE / 2, HANDLE, HANDLE);

  // Alignment guides while dragging.
  const o = origin();
  vctx.strokeStyle = '#ff4fd8';
  vctx.lineWidth = 1;
  for (const g of state.guides) {
    vctx.beginPath();
    if (g.axis === 'x') {
      vctx.moveTo((o.x + g.at) * z + 0.5, o.y * z);
      vctx.lineTo((o.x + g.at) * z + 0.5, (o.y + o.h) * z);
    } else {
      vctx.moveTo(o.x * z, (o.y + g.at) * z + 0.5);
      vctx.lineTo((o.x + o.w) * z, (o.y + g.at) * z + 0.5);
    }
    vctx.stroke();
  }
}

function box(r, colour, width) {
  const z = state.zoom;
  vctx.strokeStyle = colour;
  vctx.lineWidth = width;
  vctx.strokeRect(r.x * z - width / 2, r.y * z - width / 2, r.w * z + width, r.h * z + width);
  vctx.lineWidth = 1;
}

/** Resize handles in zoomed canvas pixels, only on the edges the widget can stretch. */
function handlesFor(w) {
  const r = rectOf(w);
  const z = state.zoom;
  const axes = resizeAxes(w);
  const out = [];
  if (axes === 'x' || axes === 'both') out.push({ id: 'e', x: (r.x + r.w) * z, y: (r.y + r.h / 2) * z });
  if (axes === 'both') {
    out.push({ id: 's', x: (r.x + r.w / 2) * z, y: (r.y + r.h) * z });
    out.push({ id: 'se', x: (r.x + r.w) * z, y: (r.y + r.h) * z });
  }
  return out;
}

/** The panel's own corner, for resizing the whole panel. */
function panelHandle() {
  const o = origin();
  return { x: (o.x + o.w) * state.zoom, y: (o.y + o.h) * state.zoom };
}

function hitWidget(p) {
  return drawOrder().reverse().find((w) => inside(p, rectOf(w))) ?? null;
}

// ---------------------------------------------------------------------------------------------
// Problems, shown on the widget and in the inspector instead of a list of checks

function findProblems() {
  const panel = state.panel;
  const out = new Map();
  const add = (w, message) => {
    if (!out.has(w.id)) out.set(w.id, []);
    out.get(w.id).push(message);
  };
  const widest = (font, text) => Math.max(0, ...assets.lineWidths(font, text ?? ''));

  for (const w of panel.widgets) {
    if (w.x < 0 || w.x + w.w > panel.w || w.y + w.h > panel.h || w.y < 0) {
      add(w, 'Part of it is outside the panel.');
    } else if (w.y < TITLE_BAR) {
      add(w, 'It overlaps the title bar.');
    }
    switch (w.type) {
      case 'button': {
        const style = BUTTON_STYLES[w.style] ?? BUTTON_STYLES.stone;
        const font = !style.sprite && w.h >= 28 ? FONT.bold : FONT.small;
        if (widest(font, w.label) > w.w - 6) {
          add(w, style.sprite
            ? 'The label is too long for this style. Stone buttons can be made wider.'
            : 'The label is too long. Make the button wider or shorten the label.');
        }
        break;
      }
      case 'heading':
      case 'text':
        if (widest(w.type === 'heading' ? FONT.bold : FONT.regular, w.text) > w.w) {
          add(w, 'The text is wider than its box. Drag the blue square to widen it.');
        }
        break;
      case 'tabs': {
        const labels = tabLabels(w);
        const tabW = Math.floor(w.w / labels.length);
        if (labels.some((l) => widest(FONT.bold, l) > tabW - 20)) {
          add(w, 'Some tab names are too long. Make the tabs wider.');
        }
        break;
      }
      case 'grid': {
        const cells = gridCells(w);
        const labels = w.labels ?? [];
        if (labels.slice(cells.length).some((l) => l.trim())) {
          add(w, `There are more names than cells (${cells.length}). Add rows or columns.`);
        }
        if (w.cells === 'items') {
          if (cells[0].w < ITEM_SIZE.w || cells[0].h < ITEM_SIZE.h) {
            add(w, 'The cells are too small for item slots. Make the grid bigger or use fewer cells.');
          }
        } else {
          const font = cells[0].h >= 28 ? FONT.bold : FONT.small;
          if (labels.some((l) => widest(font, l) > cells[0].w - 6)) add(w, 'Some names are too long for their cells.');
          if (cells[0].h < 14) add(w, 'The cells are too short to read. Make the grid taller.');
        }
        break;
      }
    }
  }
  return out;
}

function showProblems() {
  const node = $('#problems');
  if (!node) return;
  node.innerHTML = '';
  for (const message of state.problems.get(state.selected) ?? []) node.append(el('p', { textContent: message }));
}

// ---------------------------------------------------------------------------------------------
// Canvas interaction

function canvasPoint(e) {
  const r = view.getBoundingClientRect();
  return {
    x: (e.clientX - r.left) / state.zoom,
    y: (e.clientY - r.top) / state.zoom,
    zx: e.clientX - r.left,
    zy: e.clientY - r.top,
  };
}

const near = (h, p) => Math.abs(h.x - p.zx) <= HANDLE && Math.abs(h.y - p.zy) <= HANDLE;

/**
 * Snaps one axis of a moving widget: its start, centre or end lines up with another widget's
 * edge or centre, or the panel's, when within PULL; otherwise it lands on the SNAP grid.
 */
function snapAxis(pos, size, lines) {
  let best = null;
  for (const offset of [0, size / 2, size]) {
    for (const at of lines) {
      const d = at - (pos + offset);
      if (Math.abs(d) <= PULL && (!best || Math.abs(d) < Math.abs(best.d))) best = { d, at };
    }
  }
  return best ? { pos: Math.round(pos + best.d), guide: best.at } : { pos: round(pos), guide: null };
}

function alignmentLines(except) {
  const panel = state.panel;
  const others = panel.widgets.filter((w) => w.id !== except);
  return {
    x: [INSET, panel.w - INSET, panel.w / 2, ...others.flatMap((o) => [o.x, o.x + o.w, o.x + o.w / 2])],
    y: [CONTENT_TOP, panel.h - INSET, panel.h / 2, ...others.flatMap((o) => [o.y, o.y + o.h, o.y + o.h / 2])],
  };
}

function bindCanvas() {
  view.addEventListener('mousedown', (e) => {
    if (!state.panel || e.button !== 0) return;
    const p = canvasPoint(e);
    const s = selected();
    const handle = s && handlesFor(s).find((h) => near(h, p));
    if (handle) {
      state.drag = { kind: 'resize', edge: handle.id, id: s.id, start: { ...s }, origin: p, moved: false };
      return;
    }
    if (near(panelHandle(), p)) {
      state.drag = { kind: 'panel', start: { w: state.panel.w, h: state.panel.h }, origin: p, moved: false };
      return;
    }
    const w = hitWidget(p);
    if (!w) {
      select(null);
      return;
    }
    if (w.id !== state.selected) select(w.id);
    state.drag = { kind: 'move', id: w.id, start: { x: w.x, y: w.y }, origin: p, moved: false };
  });

  window.addEventListener('mousemove', (e) => {
    if (!state.panel || $('#editor').hidden) return;
    const p = canvasPoint(e);
    const over = p.x >= 0 && p.y >= 0 && p.x < CANVAS.w && p.y < CANVAS.h;
    state.mouse = over ? p : null;
    const d = state.drag;
    if (!d) {
      view.style.cursor = over ? cursorAt(p) : 'default';
      draw();
      return;
    }
    const dx = p.x - d.origin.x;
    const dy = p.y - d.origin.y;
    if (!d.moved && Math.abs(dx) < 1 && Math.abs(dy) < 1) return;
    if (!d.moved) {
      pushUndo();
      d.moved = true;
    }
    dragTo(d, dx, dy);
    refreshCanvas();
  });

  window.addEventListener('mouseup', () => {
    const d = state.drag;
    state.drag = null;
    state.guides = [];
    if (d?.moved) {
      changed();
      refreshAll();
    }
  });

  view.addEventListener('mouseleave', () => {
    state.mouse = null;
    draw();
  });

  view.addEventListener('dragover', (e) => {
    if (e.dataTransfer.types.includes('text/widget')) e.preventDefault();
  });
  view.addEventListener('drop', (e) => {
    e.preventDefault();
    const type = e.dataTransfer.getData('text/widget');
    if (WIDGETS[type]) addWidget(type, canvasPoint(e));
  });
}

function dragTo(d, dx, dy) {
  const panel = state.panel;
  if (d.kind === 'panel') {
    // The panel is centred, so its corner moves half as far as its size changes.
    panel.w = clamp(round(d.start.w + 2 * dx), PANEL_MIN.w, MODAL.w);
    panel.h = clamp(round(d.start.h + 2 * dy), PANEL_MIN.h, MODAL.h);
    return;
  }
  const w = widget(d.id);
  const lines = alignmentLines(w.id);
  state.guides = [];
  if (d.kind === 'move') {
    const x = snapAxis(d.start.x + dx, w.w, lines.x);
    const y = snapAxis(d.start.y + dy, w.h, lines.y);
    w.x = x.pos;
    w.y = y.pos;
    if (x.guide !== null) state.guides.push({ axis: 'x', at: x.guide });
    if (y.guide !== null) state.guides.push({ axis: 'y', at: y.guide });
    return;
  }
  // Resizing moves the right or bottom edge; it lines up like a moving edge does.
  if (d.edge.includes('e')) {
    const edge = snapAxis(d.start.x + d.start.w + dx, 0, lines.x);
    w.w = Math.max(MIN.w, edge.pos - w.x);
    if (edge.guide !== null) state.guides.push({ axis: 'x', at: edge.guide });
  }
  if (d.edge.includes('s')) {
    const edge = snapAxis(d.start.y + d.start.h + dy, 0, lines.y);
    w.h = Math.max(MIN.h, edge.pos - w.y);
    if (edge.guide !== null) state.guides.push({ axis: 'y', at: edge.guide });
  }
}

function cursorAt(p) {
  const s = selected();
  const h = s && handlesFor(s).find((x) => near(x, p));
  if (h) return { e: 'ew-resize', s: 'ns-resize', se: 'nwse-resize' }[h.id];
  if (near(panelHandle(), p)) return 'nwse-resize';
  return hitWidget(p) ? 'move' : 'default';
}

function select(id) {
  state.selected = id;
  editing = false;
  refreshAll();
}

function addWidget(type, point) {
  mutate(() => {
    const panel = state.panel;
    const size = WIDGETS[type].defaults;
    let x;
    let y;
    if (point) {
      const o = origin();
      x = round(point.x - o.x - size.w / 2);
      y = round(point.y - o.y - size.h / 2);
    } else {
      x = round((panel.w - size.w) / 2);
      y = round(Math.max(CONTENT_TOP, (panel.h - size.h) / 2));
    }
    const w = makeWidget(panel, type, x, y);
    fitTextHeight(w);
    panel.widgets.push(w);
    state.selected = w.id;
  });
}

function duplicateSelected() {
  const w = selected();
  if (!w) return;
  mutate(() => {
    const copy = { ...structuredClone(w), id: nextId(state.panel, w.type), x: w.x + 8, y: w.y + 8 };
    state.panel.widgets.push(copy);
    state.selected = copy.id;
  });
}

async function removeSelected() {
  const w = selected();
  if (!w) return;
  // Once a panel is in the game, its script refers to these by name.
  const used = state.referenced.filter((n) => n === w.id || n.startsWith(`${w.id}_`));
  if (used.length && !(await confirmBox(
    `The game's code uses this ${WIDGETS[w.type].label.toLowerCase()}. Deleting it breaks the server until Claude updates the code.`,
    'Delete anyway',
  ))) return;
  mutate(() => {
    state.panel.widgets = state.panel.widgets.filter((x) => x.id !== w.id);
    state.selected = null;
  });
}

function nudge(dx, dy) {
  const w = selected();
  if (!w) return;
  pushUndo();
  w.x += dx;
  w.y += dy;
  changed();
  refreshCanvas();
}

function bindKeys() {
  window.addEventListener('keydown', (e) => {
    if (!state.panel || $('#editor').hidden) return;
    if (e.target.closest('input, textarea, select, dialog')) return;
    const mod = e.metaKey || e.ctrlKey;
    const key = e.key.toLowerCase();
    if (mod && key === 'z') {
      e.preventDefault();
      e.shiftKey ? redo() : undo();
    } else if (mod && key === 'y') {
      e.preventDefault();
      redo();
    } else if (mod && key === 'd') {
      e.preventDefault();
      duplicateSelected();
    } else if (e.key === 'Delete' || e.key === 'Backspace') {
      e.preventDefault();
      removeSelected();
    } else if (e.key === 'Escape') {
      select(null);
    } else if (e.key.startsWith('Arrow')) {
      e.preventDefault();
      const step = e.shiftKey ? 8 : 1;
      const [dx, dy] = { ArrowLeft: [-step, 0], ArrowRight: [step, 0], ArrowUp: [0, -step], ArrowDown: [0, step] }[e.key];
      nudge(dx, dy);
    }
  });
}

// ---------------------------------------------------------------------------------------------
// Palette

function buildPalette() {
  const palette = $('#palette');
  palette.innerHTML = '';
  for (const [type, def] of Object.entries(WIDGETS)) {
    // Each card shows the widget itself, drawn on a small blank panel.
    const sample = newPanel(' ', 'blank');
    sample.w = Math.min(MODAL.w, Math.max(def.defaults.w + 40, 100));
    sample.h = def.defaults.h + 52;
    const w = makeWidget(sample, type, 20, 36);
    if (type === 'grid') w.labels = ['A', 'B', 'C'];
    sample.widgets.push(w);
    const card = el('button', { className: 'widget-card', type: 'button', draggable: true, title: def.hint },
      thumb(sample),
      el('strong', { textContent: def.label }),
      el('span', { className: 'hint', textContent: def.hint }));
    card.addEventListener('click', () => addWidget(type));
    card.addEventListener('dragstart', (e) => e.dataTransfer.setData('text/widget', type));
    palette.append(card);
  }
}

// ---------------------------------------------------------------------------------------------
// Inspector: only what Dylan decides

function field(label, input, hint) {
  return el('label', { className: 'field' },
    el('span', { textContent: label }),
    input,
    hint ? el('small', { className: 'hint', textContent: hint }) : null);
}

function text(w, key, { multiline = false, big = false, placeholder = '', after } = {}) {
  const input = el(multiline ? 'textarea' : 'input', {
    value: w[key] ?? '', placeholder, spellcheck: multiline, className: big ? 'big' : '',
  });
  if (!multiline) input.type = 'text';
  input.addEventListener('input', () => edit(() => {
    const target = key === 'title' || key === 'notes' ? state.panel : widget(w.id);
    target[key] = input.value;
    after?.(target);
  }));
  input.addEventListener('blur', () => (editing = false));
  return input;
}

/** One entry per line, for tab names and grid labels. Blank lines keep their place. */
function lines(w, key, placeholder) {
  const input = el('textarea', { value: (w[key] ?? []).join('\n'), placeholder, className: 'big' });
  input.addEventListener('input', () => edit(() => (widget(w.id)[key] = input.value.split('\n'))));
  input.addEventListener('blur', () => (editing = false));
  return input;
}

function segmented(options, current, pick) {
  const wrap = el('div', { className: 'segmented' });
  for (const { value, label, visual } of options) {
    const b = el('button', { type: 'button', className: value === current ? 'on' : '', title: label });
    if (visual) b.append(visual);
    b.append(el('span', { textContent: label }));
    b.addEventListener('click', () => pick(value));
    wrap.append(b);
  }
  return wrap;
}

function stepper(w, key, min, max) {
  const value = el('output', { textContent: w[key] });
  const step = (by) => mutate(() => (widget(w.id)[key] = clamp(w[key] + by, min, max)));
  const minus = el('button', { type: 'button', textContent: '−' });
  const plus = el('button', { type: 'button', textContent: '+' });
  minus.addEventListener('click', () => step(-1));
  plus.addEventListener('click', () => step(1));
  return el('span', { className: 'stepper' }, minus, value, plus);
}

function alignChoice(w) {
  return segmented(
    [{ value: 'left', label: 'Left' }, { value: 'centre', label: 'Centre' }],
    w.align ?? 'left',
    (v) => mutate(() => (widget(w.id).align = v)),
  );
}

function whenClicked(w, label = 'When clicked…') {
  return field(label, text(w, 'action', {
    multiline: true, big: true, placeholder: 'e.g. Teleports the player home and closes the panel.',
  }), 'Plain English is fine: Claude writes the code from this.');
}

function buildInspector() {
  const box = $('#inspector');
  box.innerHTML = '';
  const w = selected();
  if (!w) {
    panelInspector(box);
    return;
  }
  const def = WIDGETS[w.type];
  box.append(el('h2', { textContent: def.label }), el('p', { className: 'hint', textContent: def.hint }));

  switch (w.type) {
    case 'button': {
      const styles = Object.entries(BUTTON_STYLES).map(([value, s]) => ({
        value,
        label: s.label,
        visual: s.sprite ? el('img', { src: assets.spriteUrl(s.sprite), alt: '' }) : el('div', { className: 'stone-swatch' }),
      }));
      box.append(
        field('Label', text(w, 'label')),
        field('Look', segmented(styles, w.style, (v) => mutate(() => {
          const x = widget(w.id);
          const style = BUTTON_STYLES[v];
          if (style.sprite) {
            x.w = style.w;
            x.h = style.h;
          } else if (BUTTON_STYLES[x.style]?.sprite) {
            x.w = style.w;
            x.h = style.h;
          }
          x.style = v;
        }))),
        whenClicked(w),
      );
      break;
    }
    case 'heading':
      box.append(field('Text', text(w, 'text', { after: fitTextHeight })), field('Line up', alignChoice(w)));
      break;
    case 'text':
      box.append(
        field('Text', text(w, 'text', { multiline: true, after: fitTextHeight })),
        field('Colour', segmented(
          Object.entries(COLOURS).map(([value, hexValue]) => ({
            value, label: value, visual: el('span', { className: 'swatch', style: `display:block;background:#${hexValue}` }),
          })),
          w.colour,
          (v) => mutate(() => (widget(w.id).colour = v)),
        )),
        field('Line up', alignChoice(w)),
      );
      break;
    case 'tabs':
      box.append(
        field('Tab names', lines(w, 'labels', 'One per line'), 'One name per line. The first tab starts selected.'),
        whenClicked(w, 'When a tab is clicked…'),
      );
      break;
    case 'grid':
      box.append(
        field('Size', el('div', { className: 'pair' },
          el('span', {}, 'Rows ', stepper(w, 'rows', 1, 12)),
          el('span', {}, 'Columns ', stepper(w, 'cols', 1, 12)))),
        field('Each cell is', segmented(
          [{ value: 'buttons', label: 'A button' }, { value: 'items', label: 'An item slot' }],
          w.cells,
          (v) => mutate(() => (widget(w.id).cells = v)),
        )),
      );
      if (w.cells !== 'items') {
        box.append(field('Names', lines(w, 'labels', 'One per line'),
          'One per line, filling left to right. Leave a line empty for an empty cell, or leave them all out if the server fills them in.'));
      }
      box.append(whenClicked(w, w.cells === 'items' ? 'When an item is clicked…' : 'When a cell is clicked…'));
      break;
    case 'icon': {
      const preview = el('img', { src: assets.spriteUrl(w.sprite), className: 'thumb', alt: '' });
      const choose = el('button', { type: 'button', textContent: 'Choose picture…' });
      choose.addEventListener('click', async () => {
        const id = await pickSprite(assets, w.sprite);
        if (id === null) return;
        const meta = assets.spriteIndex.get(id);
        mutate(() => Object.assign(widget(w.id), { sprite: id, w: meta.w, h: meta.h }));
      });
      box.append(field('Picture', el('div', { className: 'pair' }, preview, choose)));
      break;
    }
    case 'item':
      box.append(
        field('Example item', text(w, 'item'), 'Only for this picture. The server decides which item really shows.'),
        whenClicked(w),
      );
      break;
    case 'box':
      box.append(el('p', { className: 'hint', textContent: 'Put things on top of it to group them. It always sits behind everything else.' }));
      break;
  }

  box.append(el('div', { id: 'problems', className: 'problems' }));
  const duplicate = el('button', { type: 'button', textContent: 'Duplicate' });
  const remove = el('button', { type: 'button', textContent: 'Delete', className: 'danger' });
  duplicate.addEventListener('click', duplicateSelected);
  remove.addEventListener('click', removeSelected);
  box.append(el('div', { className: 'actions' }, duplicate, remove));
  showProblems();
}

function panelInspector(box) {
  const panel = state.panel;
  const sizes = Object.entries(PANEL_SIZES).map(([value, s]) => ({ value, label: s.label }));
  const current = Object.entries(PANEL_SIZES).find(([, s]) => s.w === panel.w && s.h === panel.h)?.[0];
  box.append(
    el('h2', { textContent: 'The panel' }),
    el('p', { className: 'hint', textContent: 'Click something on the panel to change it, or add something from the left.' }),
    field('Title', text(panel, 'title')),
    field('Size', segmented(sizes, current, (v) => mutate(() => Object.assign(state.panel, {
      w: PANEL_SIZES[v].w, h: PANEL_SIZES[v].h,
    }))), `${panel.w} x ${panel.h}. Drag the orange square at its corner to fine-tune.`),
    field('What is it for, and how does a player open it?', text(panel, 'notes', {
      multiline: true, big: true, placeholder: 'e.g. Opens when you talk to the bank tutor. Lets you pick a bank tab to jump to.',
    }), 'Claude reads this when building it.'),
  );
}

// ---------------------------------------------------------------------------------------------
// Dialogs, toolbar and boot

/** An in-page yes/no, resolved straight from its buttons (never from the queued close event). */
function confirmBox(message, yes = 'OK') {
  const dialog = $('#confirm');
  $('#confirm-text').textContent = message;
  $('#confirm-yes').textContent = yes;
  return new Promise((resolve) => {
    const done = (answer) => {
      $('#confirm-yes').onclick = null;
      $('#confirm-no').onclick = null;
      dialog.removeEventListener('cancel', cancel);
      if (dialog.open) dialog.close();
      resolve(answer);
    };
    const cancel = (e) => {
      e.preventDefault();
      done(false);
    };
    $('#confirm-yes').onclick = () => done(true);
    $('#confirm-no').onclick = () => done(false);
    dialog.addEventListener('cancel', cancel);
    dialog.showModal();
  });
}

function exportPng() {
  const saved = state.selected;
  state.selected = null;
  render(bctx, comps, lay, assets, null);
  state.selected = saved;
  const r = origin();
  const out = document.createElement('canvas');
  out.width = r.w * state.zoom;
  out.height = r.h * state.zoom;
  const g = out.getContext('2d');
  g.imageSmoothingEnabled = false;
  g.drawImage(buffer, r.x, r.y, r.w, r.h, 0, 0, out.width, out.height);
  out.toBlob((blob) => {
    const a = el('a', { href: URL.createObjectURL(blob), download: `${state.panel.interface}.png` });
    a.click();
    setTimeout(() => URL.revokeObjectURL(a.href), 1000);
  });
  draw();
}

function bindToolbar() {
  $('#back').addEventListener('click', showStart);
  $('#undo').addEventListener('click', undo);
  $('#redo').addEventListener('click', redo);
  $('#export').addEventListener('click', exportPng);
  $('#zoom').addEventListener('change', (e) => {
    state.zoom = +e.target.value;
    draw();
  });
  $('#new-title').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') createPanel('blank');
  });
  window.addEventListener('beforeunload', (e) => {
    if (state.saveTimer) e.preventDefault();
  });
}

async function boot() {
  try {
    await assets.init();
  } catch {
    $('#start').innerHTML = '<p>No game art found. Run <code>tools/interface-designer/designer.sh export</code>, then reload.</p>';
    return;
  }
  assets.onchange = () => {
    draw();
    repaintThumbs();
  };
  bindToolbar();
  bindCanvas();
  bindKeys();
  buildPalette();
  const path = new URLSearchParams(location.search).get('panel');
  if (path) {
    try {
      await openPanel(path);
      return;
    } catch {
      // Fall through to the start screen.
    }
  }
  await showStart();
}

// Exposed for debugging from the console.
window.simple = { state, assets, compile };
boot();
