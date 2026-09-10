// The interface designer. Holds one design in memory as full component objects (defaults filled
// in), and sends it to serve.py, which validates it and writes the canonical file.
//
// A design's component array is its child-index order, which is also the client's draw order
// among siblings. Every structural edit keeps two invariants the Kotlin loader relies on: a
// parent always sits above its children, and only layers have children.

import {
  Assets, CANVAS, CONTAINER, DEFAULTS, MODAL, hex, inside, intersect, invPos, layout, render,
  stripDefaults, withDefaults,
} from './render.js';
import { PARTS } from './parts.js';
import { pickSprite as pickSpriteDialog } from './picker.js';

const $ = (selector) => document.querySelector(selector);
const NAME = /^[a-z0-9_]+$/;
const DRAFTS = 'tools/interface-designer/designs/';
const SUFFIX = '.interface.json';
const HANDLE = 6;

const assets = new Assets('/assets/');
const state = {
  path: null,
  design: null,
  implemented: false,
  referenced: new Set(),
  selected: null,
  zoom: 2,
  hoverPreview: false,
  showHidden: false,
  dirty: false,
  undo: [],
  redo: [],
  mouse: null,
  drag: null,
  designs: [],
};
let lay = null;
let editing = false; // one undo step per focused properties field

const view = $('#canvas');
const vctx = view.getContext('2d');
const buffer = document.createElement('canvas');
buffer.width = CANVAS.w;
buffer.height = CANVAS.h;
const bctx = buffer.getContext('2d');

// ---------------------------------------------------------------------------------------------
// Model helpers

const comps = () => state.design.components;
const indexOf = (name) => comps().findIndex((c) => c.name === name);
const comp = (name) => comps()[indexOf(name)];
const selectedComp = () => (state.selected === null ? null : comp(state.selected) ?? null);
const childrenOf = (name) => comps().filter((c) => c.parent === name);

function subtreeNames(name) {
  const out = [name];
  for (let i = 0; i < out.length; i++) {
    for (const c of comps()) if (c.parent === out[i]) out.push(c.name);
  }
  return new Set(out);
}

function endOfSubtree(name) {
  const names = subtreeNames(name);
  let last = -1;
  comps().forEach((c, i) => {
    if (names.has(c.name)) last = i;
  });
  return last;
}

/** Removes a component and everything under it, keeping their relative order. */
function takeBlock(name) {
  const names = subtreeNames(name);
  const block = comps().filter((c) => names.has(c.name));
  state.design.components = comps().filter((c) => !names.has(c.name));
  return block;
}

function uniqueName(base, taken = new Set(comps().map((c) => c.name))) {
  if (!taken.has(base)) return base;
  for (let n = 2; ; n++) if (!taken.has(`${base}_${n}`)) return `${base}_${n}`;
}

/** Where new children go: into the selection if it is a layer, else beside it. */
function targetLayer() {
  const c = selectedComp();
  if (!c) return comps()[0].name;
  return c.type === 'layer' ? c.name : c.parent ?? comps()[0].name;
}

function parentRect(i) {
  return i === 0 ? CONTAINER : lay.rects[lay.index.get(comps()[i].parent)];
}

/** Sets a component's authored geometry so it lands on absolute rect `r`, whatever its modes. */
function setAbsRect(name, r) {
  const i = indexOf(name);
  const c = comps()[i];
  const p = parentRect(i);
  const w = Math.max(0, r.w);
  const h = Math.max(0, r.h);
  c.w = c.wMode === 'minus' ? p.w - w : w;
  c.h = c.hMode === 'minus' ? p.h - h : h;
  c.x = invPos(r.x - p.x, c.xMode, p.w, w);
  c.y = invPos(r.y - p.y, c.yMode, p.h, h);
}

function relayout() {
  lay = layout(comps(), state.showHidden);
}

// ---------------------------------------------------------------------------------------------
// History and dirty state

function snapshot() {
  return JSON.stringify({ design: state.design, selected: state.selected });
}

function pushUndo() {
  state.undo.push(snapshot());
  if (state.undo.length > 300) state.undo.shift();
  state.redo = [];
}

function restore(text) {
  const saved = JSON.parse(text);
  state.design = saved.design;
  state.selected = saved.selected;
  markDirty();
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

function markDirty() {
  state.dirty = true;
  document.title = `● ${state.design.interface} - Interface designer`;
  status('Unsaved changes');
}

/** A structural edit: one undo step, then everything redrawn. */
function mutate(fn) {
  pushUndo();
  fn();
  markDirty();
  refreshAll();
}

/** A field edit from the properties panel: one undo step per focused field. */
function edit(fn) {
  if (!editing) {
    pushUndo();
    editing = true;
  }
  fn();
  markDirty();
  refreshView();
}

function status(text, kind = '') {
  const el = $('#status');
  el.textContent = text;
  el.className = `status ${kind}`;
}

// ---------------------------------------------------------------------------------------------
// Server

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

async function refreshDesignList() {
  const { designs } = await api('GET', '/api/designs');
  state.designs = designs;
  const select = $('#open');
  select.innerHTML = '';
  select.append(new Option('Open…', ''));
  for (const [label, implemented] of [['Drafts', false], ['Implemented', true]]) {
    const group = document.createElement('optgroup');
    group.label = label;
    for (const d of designs.filter((d) => d.implemented === implemented)) {
      group.append(new Option(d.implemented ? `${d.interface}  (${d.path.split('/src/')[0]})` : d.interface, d.path));
    }
    if (group.children.length) select.append(group);
  }
  select.value = state.path ?? '';
}

async function open(path) {
  const data = await api('GET', `/api/design?path=${encodeURIComponent(path)}`);
  state.path = path;
  state.design = {
    format: data.design.format,
    interface: data.design.interface,
    notes: data.design.notes ?? '',
    components: data.design.components.map(withDefaults),
  };
  state.implemented = data.implemented;
  state.referenced = new Set(data.referenced);
  state.selected = state.design.components[0]?.name ?? null;
  state.undo = [];
  state.redo = [];
  state.dirty = false;
  document.title = `${state.design.interface} - Interface designer`;
  history.replaceState(null, '', `?path=${encodeURIComponent(path)}`);
  $('#open').value = path;
  status(state.implemented ? 'Implemented: saving updates component.sym' : 'Draft');
  refreshAll();
}

async function save() {
  if (!state.design) return;
  const body = {
    format: 1,
    interface: state.design.interface,
    notes: state.design.notes,
    components: comps().map(stripDefaults),
  };
  try {
    const result = await api('PUT', `/api/design?path=${encodeURIComponent(state.path)}`, body);
    state.dirty = false;
    document.title = `${state.design.interface} - Interface designer`;
    status(result.symbolsSynced ? 'Saved; component.sym updated' : 'Saved', 'good');
    await refreshDesignList();
  } catch (e) {
    status(`Not saved: ${e.message}`, 'bad');
  }
}

async function newDesign() {
  if (!(await confirmDiscard())) return;
  const taken = new Set(state.designs.map((d) => d.interface));
  const name = await ask({
    title: 'New design',
    text: 'The interface name, as it will appear in interface.sym (lowercase, digits, underscores).',
    value: 'my_panel',
    validate: (v) =>
      !NAME.test(v) ? 'Use a-z, 0-9 and _ only.' : taken.has(v) ? 'A design with that name exists.' : '',
  });
  if (!name) return;
  const title = name.replace(/_/g, ' ').replace(/^./, (ch) => ch.toUpperCase());
  const frame = PARTS.find((p) => p.id === 'frame').make('frame', 'root');
  frame[0].frame = title;
  state.design = {
    format: 1,
    interface: name,
    notes: '',
    components: [
      { name: 'root', type: 'layer', w: 488, h: 320, xMode: 'centre', yMode: 'centre', clickThrough: false },
      ...frame,
      ...PARTS.find((p) => p.id === 'contents').make('contents', 'root'),
    ].map(withDefaults),
  };
  state.path = `${DRAFTS}${name}${SUFFIX}`;
  $('#open').value = '';
  state.implemented = false;
  state.referenced = new Set();
  state.selected = 'contents';
  state.undo = [];
  state.redo = [];
  history.replaceState(null, '', `?path=${encodeURIComponent(state.path)}`);
  markDirty();
  status('New draft: Save to write it');
  refreshAll();
}

async function confirmDiscard() {
  if (!state.dirty) return true;
  return ask({ title: 'Discard unsaved changes?', text: 'The open design has unsaved changes.', ok: 'Discard' });
}

// ---------------------------------------------------------------------------------------------
// Structural edits

async function addPart(part) {
  if (!state.design) return;
  const parent = targetLayer();
  const taken = new Set(comps().map((c) => c.name));
  const suggestion = freeBase(part, parent, taken);
  const base = await ask({
    title: `Add ${part.label.toLowerCase()}`,
    text: `${part.hint} It goes inside “${parent}”.`,
    value: suggestion,
    validate: (v) => {
      if (!NAME.test(v)) return 'Use a-z, 0-9 and _ only.';
      const clash = part.make(v, parent).find((c) => taken.has(c.name));
      return clash ? `“${clash.name}” already exists.` : '';
    },
  });
  if (!base) return;
  mutate(() => {
    const block = part.make(base, parent).map(withDefaults);
    comps().splice(endOfSubtree(parent) + 1, 0, ...block);
    state.selected = base;
  });
}

function freeBase(part, parent, taken) {
  for (let n = 1; ; n++) {
    const base = n === 1 ? part.base : `${part.base}_${n}`;
    if (!part.make(base, parent).some((c) => taken.has(c.name))) return base;
  }
}

async function removeSelected() {
  const c = selectedComp();
  if (!c) return;
  if (indexOf(c.name) === 0) {
    status('The root cannot be deleted', 'bad');
    return;
  }
  const names = subtreeNames(c.name);
  const used = [...names].filter((n) => state.referenced.has(n));
  if (used.length && !(await ask({
    title: 'Delete a component scripts use?',
    text: `Kotlin refers to ${used.join(', ')} by name. Deleting breaks boot until the script is updated.`,
    ok: 'Delete anyway',
  }))) return;
  mutate(() => {
    const parent = c.parent;
    takeBlock(c.name);
    state.selected = parent;
  });
}

function duplicateSelected() {
  const c = selectedComp();
  if (!c || indexOf(c.name) === 0) return;
  mutate(() => {
    const taken = new Set(comps().map((x) => x.name));
    const names = subtreeNames(c.name);
    const block = structuredClone(comps().filter((x) => names.has(x.name)));
    const renamed = new Map();
    for (const x of block) {
      const name = uniqueName(`${x.name}_copy`, taken);
      taken.add(name);
      renamed.set(x.name, name);
    }
    for (const x of block) {
      x.name = renamed.get(x.name);
      if (renamed.has(x.parent)) x.parent = renamed.get(x.parent);
    }
    comps().splice(endOfSubtree(c.name) + 1, 0, ...block);
    relayout();
    const r = lay.rects[indexOf(block[0].name)];
    setAbsRect(block[0].name, { ...r, x: r.x + 4, y: r.y + 4 });
    state.selected = block[0].name;
  });
}

/** Moves the selection one step up (drawn later, on top) or down among its siblings. */
function reorder(direction) {
  const c = selectedComp();
  if (!c || indexOf(c.name) === 0) return;
  const siblings = childrenOf(c.parent);
  const at = siblings.indexOf(c);
  const other = siblings[at + direction];
  if (!other) return;
  mutate(() => {
    const block = takeBlock(c.name);
    const position = direction > 0 ? endOfSubtree(other.name) + 1 : indexOf(other.name);
    comps().splice(position, 0, ...block);
  });
}

/** Reparents, keeping the component where it is on screen. */
function moveInto(name, parent) {
  const c = comp(name);
  const target = comp(parent);
  if (!c || !target || c.parent === parent || indexOf(name) === 0) return;
  if (target.type !== 'layer') {
    status('Only a layer can hold other components', 'bad');
    return;
  }
  if (subtreeNames(name).has(parent)) {
    status('A component cannot go inside itself', 'bad');
    return;
  }
  mutate(() => {
    const before = { ...lay.rects[indexOf(name)] };
    const block = takeBlock(name);
    block[0].parent = parent;
    comps().splice(endOfSubtree(parent) + 1, 0, ...block);
    relayout();
    setAbsRect(name, before);
  });
}

async function rename(c, value) {
  if (value === c.name) return true;
  if (!NAME.test(value)) {
    status('Names use a-z, 0-9 and _ only', 'bad');
    return false;
  }
  if (comps().some((x) => x.name === value)) {
    status(`“${value}” already exists`, 'bad');
    return false;
  }
  if (state.referenced.has(c.name) && !(await ask({
    title: 'Rename a component scripts use?',
    text: `Kotlin refers to “${c.name}” by name. Renaming breaks boot until the script is updated.`,
    ok: 'Rename anyway',
  }))) return false;
  mutate(() => {
    const old = c.name;
    for (const x of comps()) if (x.parent === old) x.parent = value;
    comp(old).name = value;
    state.selected = value;
  });
  return true;
}

function select(name) {
  state.selected = name;
  editing = false;
  refreshAll();
}

// ---------------------------------------------------------------------------------------------
// Drawing

function refreshAll() {
  const has = !!state.design;
  $('#empty').hidden = has;
  $('.canvas-wrap').hidden = !has;
  if (!has) return;
  relayout();
  buildTree();
  buildProps();
  draw();
  validate();
}

/** After a field edit: everything except the properties panel, which the user is typing in. */
function refreshView() {
  relayout();
  buildTree();
  draw();
  validate();
  syncGeometryInputs();
}

function draw() {
  if (!state.design) return;
  const z = state.zoom;
  if (view.width !== CANVAS.w * z) {
    view.width = CANVAS.w * z;
    view.height = CANVAS.h * z;
  }
  render(bctx, comps(), lay, assets, state.hoverPreview ? state.mouse : null);
  vctx.imageSmoothingEnabled = false;
  vctx.drawImage(buffer, 0, 0, view.width, view.height);

  // The modal area the panel is opened into.
  vctx.setLineDash([4, 4]);
  vctx.strokeStyle = 'rgba(255,255,255,.35)';
  vctx.strokeRect(CONTAINER.x * z - 0.5, CONTAINER.y * z - 0.5, MODAL.w * z + 1, MODAL.h * z + 1);
  vctx.setLineDash([]);

  if (!state.hoverPreview && state.mouse && !state.drag) {
    const hit = hitsAt(state.mouse)[0];
    if (hit !== undefined && comps()[hit].name !== state.selected) outlineRect(lay.rects[hit], 'rgba(63,167,255,.5)');
  }
  const c = selectedComp();
  if (c) {
    const r = lay.rects[indexOf(c.name)];
    outlineRect(r, '#3fa7ff', !lay.visible[indexOf(c.name)]);
    vctx.fillStyle = '#3fa7ff';
    for (const h of handles(r)) vctx.fillRect(h.x - HANDLE / 2, h.y - HANDLE / 2, HANDLE, HANDLE);
  }
}

function outlineRect(r, colour, dashed = false) {
  const z = state.zoom;
  vctx.strokeStyle = colour;
  vctx.lineWidth = 1;
  if (dashed) vctx.setLineDash([3, 3]);
  vctx.strokeRect(r.x * z - 0.5, r.y * z - 0.5, r.w * z + 1, r.h * z + 1);
  vctx.setLineDash([]);
}

/** Resize handles in canvas pixels (zoomed), named by the edges they move. */
function handles(r) {
  const z = state.zoom;
  const [l, t, rt, b] = [r.x * z, r.y * z, (r.x + r.w) * z, (r.y + r.h) * z];
  const mx = (l + rt) / 2;
  const my = (t + b) / 2;
  return [
    { id: 'nw', x: l, y: t }, { id: 'n', x: mx, y: t }, { id: 'ne', x: rt, y: t },
    { id: 'w', x: l, y: my }, { id: 'e', x: rt, y: my },
    { id: 'sw', x: l, y: b }, { id: 's', x: mx, y: b }, { id: 'se', x: rt, y: b },
  ];
}

/** Visible components under a point, topmost first. */
function hitsAt(p) {
  return lay.order
    .filter((i) => inside(p, intersect(lay.rects[i], lay.clips[i])))
    .reverse();
}

// ---------------------------------------------------------------------------------------------
// Canvas interaction

function canvasPoint(e) {
  const r = view.getBoundingClientRect();
  return {
    x: Math.floor((e.clientX - r.left) / state.zoom),
    y: Math.floor((e.clientY - r.top) / state.zoom),
    zx: e.clientX - r.left,
    zy: e.clientY - r.top,
  };
}

function bindCanvas() {
  view.addEventListener('mousedown', (e) => {
    if (!state.design || e.button !== 0) return;
    const p = canvasPoint(e);
    const c = selectedComp();
    if (c) {
      const r = lay.rects[indexOf(c.name)];
      const handle = handles(r).find((h) => Math.abs(h.x - p.zx) <= HANDLE && Math.abs(h.y - p.zy) <= HANDLE);
      if (handle) {
        state.drag = { kind: 'resize', handle: handle.id, name: c.name, start: { ...r }, origin: p, moved: false };
        return;
      }
    }
    const hits = hitsAt(p).map((i) => comps()[i].name);
    if (!hits.length) return;
    const cycle = hits.includes(state.selected);
    if (!cycle) select(hits[0]);
    const r = lay.rects[indexOf(state.selected)];
    state.drag = { kind: 'move', name: state.selected, start: { ...r }, origin: p, moved: false, hits, cycle };
  });

  window.addEventListener('mousemove', (e) => {
    if (!state.design) return;
    const p = canvasPoint(e);
    const over = p.x >= 0 && p.y >= 0 && p.x < CANVAS.w && p.y < CANVAS.h;
    state.mouse = over ? p : null;
    const d = state.drag;
    if (!d) {
      if (over || state.hoverPreview) draw();
      view.style.cursor = over ? cursorAt(p) : 'default';
      return;
    }
    const dx = p.x - d.origin.x;
    const dy = p.y - d.origin.y;
    if (!d.moved && dx === 0 && dy === 0) return;
    if (!d.moved) {
      pushUndo();
      d.moved = true;
    }
    const s = d.start;
    let r = { ...s };
    if (d.kind === 'move') {
      r = { ...s, x: s.x + dx, y: s.y + dy };
    } else {
      if (d.handle.includes('w')) r = { ...r, x: Math.min(s.x + dx, s.x + s.w - 1), w: Math.max(1, s.w - dx) };
      if (d.handle.includes('e')) r.w = Math.max(1, s.w + dx);
      if (d.handle.includes('n')) r = { ...r, y: Math.min(s.y + dy, s.y + s.h - 1), h: Math.max(1, s.h - dy) };
      if (d.handle.includes('s')) r.h = Math.max(1, s.h + dy);
    }
    setAbsRect(d.name, r);
    markDirty();
    refreshView();
  });

  window.addEventListener('mouseup', () => {
    const d = state.drag;
    state.drag = null;
    if (d && d.kind === 'move' && !d.moved && d.cycle) {
      // A second click on the same spot reaches the next component underneath.
      const at = d.hits.indexOf(state.selected);
      select(d.hits[(at + 1) % d.hits.length]);
    }
  });

  view.addEventListener('mouseleave', () => {
    state.mouse = null;
    draw();
  });
}

function cursorAt(p) {
  const c = selectedComp();
  if (c) {
    const r = lay.rects[indexOf(c.name)];
    const h = handles(r).find((h) => Math.abs(h.x - p.zx) <= HANDLE && Math.abs(h.y - p.zy) <= HANDLE);
    if (h) return { n: 'ns-resize', s: 'ns-resize', e: 'ew-resize', w: 'ew-resize', nw: 'nwse-resize', se: 'nwse-resize', ne: 'nesw-resize', sw: 'nesw-resize' }[h.id];
  }
  return hitsAt(p).length ? 'move' : 'default';
}

function nudge(dx, dy) {
  const c = selectedComp();
  if (!c) return;
  pushUndo();
  const r = lay.rects[indexOf(c.name)];
  setAbsRect(c.name, { ...r, x: r.x + dx, y: r.y + dy });
  markDirty();
  refreshView();
}

function bindKeys() {
  window.addEventListener('keydown', (e) => {
    const mod = e.metaKey || e.ctrlKey;
    if (mod && e.key.toLowerCase() === 's') {
      e.preventDefault();
      save();
      return;
    }
    const typing = e.target.closest('input, textarea, select, dialog');
    if (typing || !state.design) return;
    const step = e.shiftKey ? 10 : 1;
    if (mod && e.key.toLowerCase() === 'z') {
      e.preventDefault();
      e.shiftKey ? redo() : undo();
    } else if (mod && e.key.toLowerCase() === 'y') {
      e.preventDefault();
      redo();
    } else if (mod && e.key.toLowerCase() === 'd') {
      e.preventDefault();
      duplicateSelected();
    } else if (e.key === 'Delete' || e.key === 'Backspace') {
      e.preventDefault();
      removeSelected();
    } else if (e.key === 'Escape') {
      const c = selectedComp();
      if (c?.parent) select(c.parent);
    } else if (e.key.startsWith('Arrow')) {
      e.preventDefault();
      const [dx, dy] = { ArrowLeft: [-step, 0], ArrowRight: [step, 0], ArrowUp: [0, -step], ArrowDown: [0, step] }[e.key];
      nudge(dx, dy);
    }
  });
}

// ---------------------------------------------------------------------------------------------
// Palette and tree

function buildPalette() {
  const el = $('#palette');
  const groups = new Map();
  for (const part of PARTS) {
    if (!groups.has(part.group)) {
      const g = document.createElement('div');
      g.className = 'group';
      g.innerHTML = `<span class="group-name">${part.group}</span>`;
      el.append(g);
      groups.set(part.group, g);
    }
    const b = document.createElement('button');
    b.textContent = part.label;
    b.title = part.hint;
    b.addEventListener('click', () => addPart(part));
    groups.get(part.group).append(b);
  }
}

function buildTree() {
  const el = $('#tree');
  el.innerHTML = '';
  if (!state.design) return;
  const walk = (i, depth) => {
    const c = comps()[i];
    const row = document.createElement('div');
    row.className = `node${c.name === state.selected ? ' selected' : ''}${c.hidden ? ' hidden-node' : ''}`;
    row.style.setProperty('--depth', depth);
    row.draggable = i !== 0;
    row.dataset.name = c.name;
    row.innerHTML = `<span class="type type-${c.type}">${c.type[0].toUpperCase()}</span>`;
    const name = document.createElement('span');
    name.className = 'name';
    name.textContent = c.name;
    row.append(name);
    if (state.referenced.has(c.name)) {
      const lock = document.createElement('span');
      lock.className = 'lock';
      lock.textContent = 'used';
      lock.title = 'Kotlin refers to this component by name';
      row.append(lock);
    }
    const eye = document.createElement('button');
    eye.className = 'eye';
    eye.textContent = c.hidden ? '◌' : '●';
    eye.title = c.hidden ? 'Starts hidden: click to show' : 'Visible: click to start hidden';
    eye.addEventListener('click', (e) => {
      e.stopPropagation();
      mutate(() => {
        comp(c.name).hidden = !c.hidden;
      });
    });
    row.append(eye);
    row.addEventListener('click', () => select(c.name));
    row.addEventListener('dragstart', (e) => e.dataTransfer.setData('text/plain', c.name));
    row.addEventListener('dragover', (e) => {
      if (c.type === 'layer') {
        e.preventDefault();
        row.classList.add('drop');
      }
    });
    row.addEventListener('dragleave', () => row.classList.remove('drop'));
    row.addEventListener('drop', (e) => {
      e.preventDefault();
      row.classList.remove('drop');
      moveInto(e.dataTransfer.getData('text/plain'), c.name);
    });
    el.append(row);
    for (const k of lay.children[i]) walk(k, depth + 1);
  };
  walk(0, 0);
  el.querySelector('.selected')?.scrollIntoView({ block: 'nearest' });
}

// ---------------------------------------------------------------------------------------------
// Properties panel

const POS_OPTIONS = {
  x: [['start', 'from left'], ['centre', 'centred'], ['end', 'from right']],
  y: [['start', 'from top'], ['centre', 'centred'], ['end', 'from bottom']],
};
const SIZE_OPTIONS = [['fixed', 'fixed'], ['minus', 'parent minus']];
const ALIGN_H = [['left', 'left'], ['centre', 'centre'], ['right', 'right']];
const ALIGN_V = [['top', 'top'], ['centre', 'centre'], ['bottom', 'bottom']];

function el(tag, props = {}, ...children) {
  const node = document.createElement(tag);
  Object.assign(node, props);
  node.append(...children);
  return node;
}

function row(label, ...inputs) {
  const wrap = el('div', { className: 'inline' }, ...inputs);
  return el('label', { className: 'row' }, el('span', { textContent: label }), wrap);
}

/** An input bound to `c[key]`: every keystroke updates the design, one undo step per field. */
function bind(input, c, key, parse = (v) => v) {
  input.addEventListener('input', () => {
    const value = parse(input.type === 'checkbox' ? input.checked : input.value);
    if (value === undefined) return;
    edit(() => {
      comp(c.name)[key] = value;
    });
  });
  input.addEventListener('change', () => (editing = false));
  input.addEventListener('blur', () => (editing = false));
  return input;
}

function number(c, key, attrs = {}) {
  const input = el('input', { type: 'number', value: c[key], step: 1, ...attrs });
  input.dataset.key = key;
  return bind(input, c, key, (v) => (v === '' || Number.isNaN(+v) ? undefined : Math.trunc(+v)));
}

function text(c, key, attrs = {}) {
  return bind(el('input', { type: 'text', value: c[key] ?? '', spellcheck: false, ...attrs }), c, key);
}

function checkbox(c, key) {
  return bind(el('input', { type: 'checkbox', checked: !!c[key] }), c, key);
}

function select_(c, key, options) {
  const s = el('select');
  for (const [value, label] of options) s.append(new Option(label, value, false, c[key] === value));
  return bind(s, c, key, (v) => (typeof options[0][0] === 'number' ? +v : v));
}

/** A colour well plus its hex, both bound to a six-digit lowercase string. */
function colour(get, set) {
  const well = el('input', { type: 'color', value: '#' + get() });
  const hexInput = el('input', { type: 'text', value: get(), size: 7, spellcheck: false });
  hexInput.style.width = '72px';
  well.addEventListener('input', () => {
    hexInput.value = well.value.slice(1);
    edit(() => set(well.value.slice(1)));
  });
  hexInput.addEventListener('input', () => {
    const v = hexInput.value.trim().replace(/^#/, '').toLowerCase();
    if (!/^[0-9a-f]{6}$/.test(v)) return;
    well.value = '#' + v;
    edit(() => set(v));
  });
  for (const input of [well, hexInput]) {
    input.addEventListener('change', () => (editing = false));
    input.addEventListener('blur', () => (editing = false));
  }
  return [well, hexInput];
}

function spriteField(get, set, fit) {
  const thumb = el('img', { className: 'thumb', alt: '' });
  const input = el('input', { type: 'number', value: get(), step: 1 });
  const refreshThumb = () => {
    const id = get();
    thumb.src = assets.spriteIndex.has(id) ? assets.spriteUrl(id) : '';
    const meta = assets.spriteIndex.get(id);
    thumb.title = meta ? `${id}: ${meta.w}x${meta.h}` : 'not in the cache';
  };
  input.addEventListener('input', () => {
    if (input.value === '') return;
    edit(() => set(Math.trunc(+input.value)));
    refreshThumb();
  });
  input.addEventListener('change', () => (editing = false));
  const browse = el('button', { type: 'button', textContent: 'Browse…' });
  browse.addEventListener('click', async () => {
    const id = await pickSprite(get());
    if (id === null) return;
    editing = false;
    edit(() => set(id));
    editing = false;
    input.value = id;
    refreshThumb();
  });
  refreshThumb();
  const parts = [thumb, input, browse];
  if (fit) {
    const fitButton = el('button', { type: 'button', textContent: 'Fit', title: 'Size the component to the sprite' });
    fitButton.addEventListener('click', () => {
      const meta = assets.spriteIndex.get(get());
      if (!meta) return;
      mutate(fit(meta));
    });
    parts.push(fitButton);
  }
  return parts;
}

function buildProps() {
  const panel = $('#props');
  panel.innerHTML = '';
  panel.className = 'right props';
  if (!state.design) return;

  const design = el('section');
  design.append(
    el('h2', { textContent: 'Design' }),
    row('Interface', el('strong', { textContent: state.design.interface }),
      el('span', {
        className: `badge${state.implemented ? ' implemented' : ''}`,
        textContent: state.implemented ? 'implemented' : 'draft',
      })),
    el('div', { className: 'hint', textContent: state.path }),
  );
  const notes = el('textarea', { value: state.design.notes, placeholder: 'What the panel is for and how it opens. Claude reads this.' });
  notes.addEventListener('input', () => edit(() => (state.design.notes = notes.value)));
  notes.addEventListener('blur', () => (editing = false));
  design.append(el('label', { className: 'row' }, el('span', { textContent: 'Notes' }), notes));
  panel.append(design);

  const c = selectedComp();
  if (!c) return;
  const i = indexOf(c.name);

  const head = el('section');
  const name = el('input', { type: 'text', value: c.name, spellcheck: false });
  name.addEventListener('change', async () => {
    if (!(await rename(c, name.value.trim()))) name.value = c.name;
  });
  head.append(
    el('h2', { textContent: `${c.type}${i === 0 ? ' (root)' : ''} · child ${i}` }),
    row('Name', name),
  );
  if (state.referenced.has(c.name)) {
    head.append(el('div', { className: 'warning-box', textContent: 'Kotlin refers to this component by name. Renaming or deleting it breaks boot until the script changes.' }));
  }
  const actions = el('div', { className: 'actions' });
  const button = (label, title, fn, extra = '') => {
    const b = el('button', { type: 'button', textContent: label, title, className: extra });
    b.addEventListener('click', fn);
    actions.append(b);
  };
  if (i > 0) {
    button('Forward', 'Draw above the next sibling', () => reorder(1));
    button('Backward', 'Draw below the previous sibling', () => reorder(-1));
    button('Duplicate', 'Ctrl+D', duplicateSelected);
    button('Delete', 'Delete / Backspace', removeSelected, 'danger');
  }
  if (c.parent) button('Parent', 'Esc', () => select(c.parent));
  head.append(actions);
  panel.append(head);

  const geo = el('section');
  geo.append(
    el('h2', { textContent: 'Position and size' }),
    row('x', number(c, 'x'), select_(c, 'xMode', POS_OPTIONS.x)),
    row('y', number(c, 'y'), select_(c, 'yMode', POS_OPTIONS.y)),
    row('width', number(c, 'w'), select_(c, 'wMode', SIZE_OPTIONS)),
    row('height', number(c, 'h'), select_(c, 'hMode', SIZE_OPTIONS)),
    el('div', { className: 'hint', id: 'abs-rect' }),
    row('Starts hidden', checkbox(c, 'hidden')),
  );
  if (c.type === 'layer') {
    const through = el('input', { type: 'checkbox', checked: !c.clickThrough });
    through.addEventListener('input', () => edit(() => (comp(c.name).clickThrough = !through.checked)));
    through.addEventListener('change', () => (editing = false));
    geo.append(row('Swallow clicks', through));
  }
  panel.append(geo);

  const look = el('section');
  look.append(el('h2', { textContent: 'Look' }));
  if (c.type === 'layer') {
    const on = el('input', { type: 'checkbox', checked: !!c.frame });
    const title = el('input', { type: 'text', value: c.frame, placeholder: 'Title', disabled: !c.frame });
    on.addEventListener('change', () => {
      editing = false;
      edit(() => (comp(c.name).frame = on.checked ? title.value || 'Title' : ''));
      editing = false;
      title.disabled = !on.checked;
      if (on.checked && !title.value) title.value = 'Title';
    });
    title.addEventListener('input', () => edit(() => (comp(c.name).frame = title.value || 'Title')));
    title.addEventListener('blur', () => (editing = false));
    look.append(row('Steelborder', on, title));
    look.append(el('div', { className: 'hint', textContent: 'The vanilla frame: stone background, steel edges, title and a close button that works client-side.' }));
  }
  if (c.type === 'rect') {
    look.append(
      row('Colour', ...colour(() => comp(c.name).colour, (v) => (comp(c.name).colour = v))),
      row('Filled', checkbox(c, 'filled')),
      row('Trans', number(c, 'trans', { min: 0, max: 255 }), el('span', { className: 'hint', textContent: '0 solid, 255 invisible' })),
    );
  }
  if (c.type === 'text') {
    const fonts = [...assets.fonts.values()].map((f) => [f.id, `${f.name} (${f.id})`]);
    const body = el('textarea', { value: c.text, spellcheck: false });
    bind(body, c, 'text');
    look.append(
      el('label', { className: 'row' }, el('span', { textContent: 'Text' }), body),
      row('Font', select_(c, 'font', fonts)),
      row('Colour', ...colour(() => comp(c.name).colour, (v) => (comp(c.name).colour = v))),
      row('Shadow', checkbox(c, 'shadow')),
      row('Align', select_(c, 'alignH', ALIGN_H), select_(c, 'alignV', ALIGN_V)),
      row('Line height', number(c, 'lineHeight', { min: 0 }), el('span', { className: 'hint', textContent: '0 = font height' })),
      el('div', { className: 'hint', textContent: '<br> breaks a line; <col=ff0000>red</col> recolours part of it.' }),
    );
  }
  if (c.type === 'graphic') {
    look.append(
      row('Sprite', ...spriteField(() => comp(c.name).sprite, (v) => (comp(c.name).sprite = v), (meta) => () => {
        const x = comp(c.name);
        x.w = meta.w;
        x.h = meta.h;
        x.wMode = 'fixed';
        x.hMode = 'fixed';
      })),
      row('Tiling', checkbox(c, 'tiling')),
      row('Trans', number(c, 'trans', { min: 0, max: 255 }), el('span', { className: 'hint', textContent: '0 solid, 255 invisible' })),
    );
  }
  if (look.children.length > 1) panel.append(look);

  if (c.type !== 'layer' && c.type !== 'item') panel.append(hoverSection(c));
  panel.append(menuSection(c));

  const behaviour = el('section');
  const cNotes = el('textarea', { value: c.notes, placeholder: 'What should happen: on click, when it shows, what text the server pushes…' });
  bind(cNotes, c, 'notes');
  behaviour.append(el('h2', { textContent: 'Behaviour notes for Claude' }), cNotes);
  panel.append(behaviour);
  syncGeometryInputs();
}

function hoverSection(c) {
  const section = el('section');
  section.append(el('h2', { textContent: 'Hover (runs client-side, no packets)' }));
  const kinds = { text: ['colour'], rect: ['trans'], graphic: ['trans', 'sprite'] }[c.type];
  const current = c.hover ? Object.keys(c.hover)[0] : 'none';
  const pick = el('select');
  pick.append(new Option('none', 'none', false, current === 'none'));
  for (const k of kinds) pick.append(new Option(k === 'colour' ? 'text colour' : k === 'trans' ? 'transparency' : 'swap sprite', k, false, current === k));
  pick.addEventListener('change', () => {
    editing = false;
    mutate(() => {
      const x = comp(c.name);
      x.hover = pick.value === 'none' ? null
        : pick.value === 'colour' ? { colour: 'ffffff' }
        : pick.value === 'trans' ? { trans: Math.max(0, x.trans - 40) }
        : { sprite: x.sprite };
    });
  });
  section.append(row('On hover', pick));
  if (c.hover?.colour !== undefined) {
    section.append(row('Colour', ...colour(() => comp(c.name).hover.colour, (v) => (comp(c.name).hover = { colour: v }))));
  }
  if (c.hover?.trans !== undefined) {
    const input = el('input', { type: 'number', min: 0, max: 255, value: c.hover.trans });
    input.addEventListener('input', () => {
      if (input.value !== '') edit(() => (comp(c.name).hover = { trans: Math.trunc(+input.value) }));
    });
    input.addEventListener('change', () => (editing = false));
    section.append(row('Trans', input));
  }
  if (c.hover?.sprite !== undefined) {
    section.append(row('Sprite', ...spriteField(() => comp(c.name).hover.sprite, (v) => (comp(c.name).hover = { sprite: v }))));
  }
  return section;
}

function menuSection(c) {
  const section = el('section');
  section.append(el('h2', { textContent: 'Right-click menu' }));
  for (let n = 0; n < 5; n++) {
    const input = el('input', { type: 'text', value: c.ops[n] ?? '', placeholder: n === 0 ? 'e.g. Select (left-click)' : '' });
    input.addEventListener('input', () => edit(() => {
      const ops = [...comp(c.name).ops];
      while (ops.length <= n) ops.push('');
      ops[n] = input.value;
      while (ops.length && !ops.at(-1)) ops.pop();
      comp(c.name).ops = ops;
    }));
    input.addEventListener('blur', () => (editing = false));
    section.append(row(`Op ${n + 1}`, input));
  }
  section.append(
    row('Target', text(c, 'opBase', { placeholder: '<col=ff9040>Item</col>' })),
    el('div', { className: 'hint', textContent: 'Op 1 is the left-click action. Target is the text after the verb, e.g. “Teleport Varrock”.' }),
  );
  return section;
}

/** Keeps the numeric inputs in step while the component is dragged. */
function syncGeometryInputs() {
  const c = selectedComp();
  if (!c) return;
  for (const input of document.querySelectorAll('#props input[data-key]')) {
    if (document.activeElement !== input) input.value = c[input.dataset.key];
  }
  const r = lay.rects[indexOf(c.name)];
  const abs = $('#abs-rect');
  if (abs) abs.textContent = `On screen: ${r.w}x${r.h} at ${r.x - CONTAINER.x}, ${r.y - CONTAINER.y} in the modal area`;
}

// ---------------------------------------------------------------------------------------------
// Checks

function validate() {
  const issues = [];
  const add = (name, level, message) => issues.push({ name, level, message });
  const seen = new Set();
  comps().forEach((c, i) => {
    if (!NAME.test(c.name)) add(c.name, 'error', `${c.name}: names use a-z, 0-9 and _ only`);
    if (seen.has(c.name)) add(c.name, 'error', `${c.name}: name used twice`);
    seen.add(c.name);
    const r = lay.rects[i];
    if (!r) return;
    if (c.type === 'text') {
      if (!assets.fonts.has(c.font)) add(c.name, 'error', `${c.name}: no font chosen`);
      else {
        for (const w of assets.lineWidths(c.font, c.text)) {
          if (w > r.w) add(c.name, 'warn', `${c.name}: text is ${w}px wide in a ${r.w}px box`);
        }
      }
    }
    if (c.type === 'graphic') {
      const meta = assets.spriteIndex.get(c.sprite);
      if (!meta) add(c.name, 'error', `${c.name}: sprite ${c.sprite} is not in the cache`);
      else if (!c.tiling && (meta.w !== r.w || meta.h !== r.h)) {
        add(c.name, 'warn', `${c.name}: sprite is ${meta.w}x${meta.h} but the box is ${r.w}x${r.h}; untiled sprites draw at natural size (stretching is unproven)`);
      }
      if (c.hover?.sprite !== undefined && !assets.spriteIndex.has(c.hover.sprite)) {
        add(c.name, 'error', `${c.name}: hover sprite ${c.hover.sprite} is not in the cache`);
      }
    }
    if (c.ops.some((op) => op) && (r.w <= 0 || r.h <= 0)) add(c.name, 'warn', `${c.name}: has ops but a ${r.w}x${r.h} click area`);
    if (c.opBase && !c.ops.some((op) => op)) add(c.name, 'warn', `${c.name}: has a menu target but no op`);
    if (c.type !== 'layer' && lay.children[i].length) add(c.name, 'error', `${c.name}: only layers can hold components`);
    // Report where a subtree first leaves the modal area, not every descendant with it.
    const p = i === 0 ? null : lay.rects[lay.index.get(c.parent)];
    const out = (rect) => rect.x < CONTAINER.x || rect.y < CONTAINER.y
      || rect.x + rect.w > CONTAINER.x + MODAL.w || rect.y + rect.h > CONTAINER.y + MODAL.h;
    if (out(r) && (!p || !out(p))) add(c.name, 'warn', `${c.name}: extends outside the ${MODAL.w}x${MODAL.h} modal area and will be cut off`);
  });

  const list = $('#issues');
  list.innerHTML = '';
  for (const issue of issues) {
    const li = el('li', { className: issue.level, textContent: issue.message });
    li.addEventListener('click', () => select(issue.name));
    list.append(li);
  }
  if (!issues.length) list.append(el('li', { className: 'ok', textContent: 'No problems found' }));
  const errors = issues.filter((x) => x.level === 'error').length;
  $('#issue-count').textContent = issues.length ? `(${errors} errors, ${issues.length - errors} checks)` : '';
}

// ---------------------------------------------------------------------------------------------
// Dialogs

/**
 * An in-page dialog. With `value` it asks for text (resolving to the string, or null); without,
 * it asks for confirmation (resolving true/false). No window.prompt: those block everything.
 */
function ask({ title, text: body, value, ok = 'OK', validate: check }) {
  const dialog = $('#ask');
  const input = $('#ask-input');
  const error = $('#ask-error');
  $('#ask-title').textContent = title;
  $('#ask-text').textContent = body ?? '';
  $('#ask-ok').textContent = ok;
  error.textContent = '';
  input.hidden = value === undefined;
  input.value = value ?? '';
  return new Promise((resolve) => {
    // Resolved straight from submit and cancel rather than from the dialog's `close` event. That
    // event is queued: the next ask() can reuse the shared input before it fires, and Chrome holds
    // it back entirely in a tab that is not rendering.
    const form = dialog.querySelector('form');
    const cancelButton = $('#ask-cancel');
    const finish = (answer) => {
      form.removeEventListener('submit', submit);
      cancelButton.removeEventListener('click', cancel);
      dialog.removeEventListener('cancel', cancel);
      if (dialog.open) dialog.close();
      resolve(answer);
    };
    const submit = (e) => {
      e.preventDefault();
      const value = input.value.trim();
      const problem = check && !input.hidden ? check(value) : '';
      if (problem) {
        error.textContent = problem;
        return;
      }
      finish(input.hidden ? true : value);
    };
    const cancel = (e) => {
      e.preventDefault();
      finish(input.hidden ? false : null);
    };
    form.addEventListener('submit', submit);
    cancelButton.addEventListener('click', cancel);
    dialog.addEventListener('cancel', cancel);
    dialog.showModal();
    if (!input.hidden) input.select();
  });
}

/** The shared sprite picker (picker.js), bound to this editor's art. */
function pickSprite(current) {
  return pickSpriteDialog(assets, current);
}

// ---------------------------------------------------------------------------------------------
// Toolbar and boot

function exportPng() {
  if (!state.design) return;
  render(bctx, comps(), lay, assets, null);
  const out = document.createElement('canvas');
  out.width = CANVAS.w * state.zoom;
  out.height = CANVAS.h * state.zoom;
  const g = out.getContext('2d');
  g.imageSmoothingEnabled = false;
  g.drawImage(buffer, 0, 0, out.width, out.height);
  out.toBlob((blob) => {
    const a = el('a', { href: URL.createObjectURL(blob), download: `${state.design.interface}.png` });
    a.click();
    setTimeout(() => URL.revokeObjectURL(a.href), 1000);
  });
  draw();
}

function bindToolbar() {
  $('#open').addEventListener('change', async (e) => {
    const path = e.target.value;
    if (!path) return;
    if (!(await confirmDiscard())) {
      e.target.value = state.path ?? '';
      return;
    }
    await open(path);
  });
  $('#new').addEventListener('click', newDesign);
  $('#save').addEventListener('click', save);
  $('#undo').addEventListener('click', undo);
  $('#redo').addEventListener('click', redo);
  $('#export').addEventListener('click', exportPng);
  $('#zoom').addEventListener('change', (e) => {
    state.zoom = +e.target.value;
    draw();
  });
  $('#hover-preview').addEventListener('change', (e) => {
    state.hoverPreview = e.target.checked;
    draw();
  });
  $('#show-hidden').addEventListener('change', (e) => {
    state.showHidden = e.target.checked;
    if (state.design) refreshAll();
  });
  window.addEventListener('beforeunload', (e) => {
    if (state.dirty) e.preventDefault();
  });
}

async function boot() {
  try {
    await assets.init();
  } catch (e) {
    $('#empty').innerHTML = '<p>No cache art found. Run <code>tools/interface-designer/designer.sh export</code>, then reload.</p>';
    return;
  }
  assets.onchange = () => draw();
  bindToolbar();
  bindCanvas();
  bindKeys();
  buildPalette();
  await refreshDesignList();
  const path = new URLSearchParams(location.search).get('path');
  if (path) {
    try {
      await open(path);
      return;
    } catch (e) {
      status(`Could not open ${path}: ${e.message}`, 'bad');
    }
  }
  refreshAll();
}

// Exposed for debugging from the console.
window.designer = { state, assets, hex, DEFAULTS };
boot();
