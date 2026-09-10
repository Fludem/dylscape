// Compiles a simple-editor panel (widgets) into a component design, the format
// DesignedComponentBuilder packs. Pure: no DOM, so node runs it too (compile.test.mjs).
//
// Every widget becomes components named after the widget's id and parented to the root, at the
// widget's position. The dressing comes from parts.js, the same vanilla idioms the advanced
// editor's palette inserts. Boxes compile first so they always sit behind everything else.

import { PARTS } from './parts.js';
import { stripDefaults } from './render.js';
import {
  BUTTON_STYLES, COLOURS, FORMAT, GRID_GAP, ITEM_SIZE, TAB_HEIGHT,
} from './widgets.js';

const FONT_SMALL = 494; // p11_full: labels on small buttons
const FONT_REGULAR = 495; // p12_full: body text
const FONT_BOLD = 496; // b12_full: headings, tab labels, tall buttons
const ORANGE = COLOURS.orange;

/** Buttons at least this tall get the bold font, as the teleport panel's footer does. */
const BOLD_BUTTON_HEIGHT = 28;

const part = (id) => PARTS.find((p) => p.id === id);

/** Menu target text in vanilla interface orange, e.g. "Select <col=ff9040>Varrock</col>". */
const target = (text) => (text ? `<col=ff9040>${text}</col>` : '');

export function compile(panel) {
  const components = [
    {
      name: 'root', type: 'layer', w: panel.w, h: panel.h,
      xMode: 'centre', yMode: 'centre', clickThrough: false,
    },
    // A frame with an empty title would read as "no frame"; a space draws nothing.
    { name: 'frame', parent: 'root', type: 'layer', wMode: 'minus', hMode: 'minus', frame: panel.title.trim() ? panel.title : ' ' },
  ];
  const ordered = [
    ...panel.widgets.filter((w) => w.type === 'box'),
    ...panel.widgets.filter((w) => w.type !== 'box'),
  ];
  for (const widget of ordered) components.push(...COMPILERS[widget.type](widget));
  return {
    format: FORMAT,
    interface: panel.interface,
    notes: panel.notes ?? '',
    components: components.map(stripDefaults),
  };
}

/** Replaces the top component's geometry and extras; the part's children fill it. */
function place(components, geometry) {
  const [top, ...rest] = components;
  return [{ ...top, ...geometry }, ...rest];
}

function label(components, text, font) {
  const last = components.at(-1);
  last.text = text;
  if (font) last.font = font;
  return components;
}

function stoneButton(id, x, y, w, h, text, extra) {
  const font = h >= BOLD_BUTTON_HEIGHT ? FONT_BOLD : FONT_SMALL;
  return place(label(part('inset').make(id, 'root'), text, font), { x, y, w, h, ...extra });
}

const COMPILERS = {
  button(w) {
    const text = w.label ?? '';
    const extra = { ops: [text.trim() || 'Select'], notes: w.action ?? '' };
    const style = BUTTON_STYLES[w.style] ?? BUTTON_STYLES.stone;
    if (!style.sprite) return stoneButton(w.id, w.x, w.y, w.w, w.h, text, extra);
    // Sprite buttons only draw at their own size, so the style fixes it.
    return place(label(part(style.part).make(w.id, 'root'), text), { x: w.x, y: w.y, w: style.w, h: style.h, ...extra });
  },

  heading(w) {
    return [{
      name: w.id, parent: 'root', type: 'text', x: w.x, y: w.y, w: w.w, h: w.h,
      colour: ORANGE, text: w.text ?? '', font: FONT_BOLD, shadow: true,
      alignH: w.align === 'centre' ? 'centre' : 'left',
    }];
  },

  text(w) {
    const colour = COLOURS[w.colour] ?? ORANGE;
    return [{
      name: w.id, parent: 'root', type: 'text', x: w.x, y: w.y, w: w.w, h: w.h,
      colour, text: (w.text ?? '').replace(/\n/g, '<br>'), font: FONT_REGULAR,
      shadow: colour !== COLOURS.black, alignH: w.align === 'centre' ? 'centre' : 'left',
    }];
  },

  /** N tabs share the width; the first is authored selected and the server swaps the rest. */
  tabs(w) {
    const labels = tabLabels(w);
    const tabW = Math.floor(w.w / labels.length);
    return labels.flatMap((text, i) => {
      const id = `${w.id}_${i}`;
      const components = label(part('tab').make(id, 'root'), text);
      components.find((c) => c.name === `${id}_on`).hidden = i !== 0;
      components.find((c) => c.name === `${id}_off`).hidden = i === 0;
      return place(components, {
        x: w.x + i * tabW, y: w.y, w: tabW, h: TAB_HEIGHT,
        opBase: target(text), notes: w.action ?? '',
      });
    });
  },

  /** Cells fill row by row; see gridCells for the maths. */
  grid(w) {
    const labels = w.labels ?? [];
    const action = (w.action ?? '').trim();
    return gridCells(w).flatMap((cell, i) => {
      const id = `${w.id}_${i}`;
      const text = labels[i] ?? '';
      const notes = i === 0 ? w.action ?? '' : '';
      if (w.cells === 'items') {
        return [{
          name: id, parent: 'root', type: 'item',
          x: cell.x + Math.floor((cell.w - ITEM_SIZE.w) / 2),
          y: cell.y + Math.floor((cell.h - ITEM_SIZE.h) / 2),
          w: ITEM_SIZE.w, h: ITEM_SIZE.h, ops: action ? ['Select'] : [], notes,
        }];
      }
      return stoneButton(id, cell.x, cell.y, cell.w, cell.h, text, {
        ops: ['Select'], opBase: target(text), notes,
      });
    });
  },

  icon(w) {
    return [{ name: w.id, parent: 'root', type: 'graphic', x: w.x, y: w.y, w: w.w, h: w.h, sprite: w.sprite }];
  },

  item(w) {
    const action = (w.action ?? '').trim();
    const example = (w.item ?? '').trim();
    return [{
      name: w.id, parent: 'root', type: 'item', x: w.x, y: w.y, w: ITEM_SIZE.w, h: ITEM_SIZE.h,
      ops: action ? ['Select'] : [],
      notes: [example && `Shows e.g. ${example}.`, action].filter(Boolean).join(' '),
    }];
  },

  box(w) {
    return place(part('well').make(w.id, 'root'), { x: w.x, y: w.y, w: w.w, h: w.h });
  },
};

export function tabLabels(w) {
  const labels = (w.labels ?? []).map((l) => l.trim()).filter(Boolean);
  return labels.length ? labels : ['Tab'];
}

/** Each cell's rect, relative to the panel, row-major, with GRID_GAP between cells. */
export function gridCells(w) {
  const rows = Math.max(1, Math.min(20, w.rows | 0));
  const cols = Math.max(1, Math.min(20, w.cols | 0));
  const cellW = Math.floor((w.w - (cols - 1) * GRID_GAP) / cols);
  const cellH = Math.floor((w.h - (rows - 1) * GRID_GAP) / rows);
  const cells = [];
  for (let i = 0; i < rows * cols; i++) {
    const col = i % cols;
    const row = Math.floor(i / cols);
    cells.push({ x: w.x + col * (cellW + GRID_GAP), y: w.y + row * (cellH + GRID_GAP), w: cellW, h: cellH });
  }
  return cells;
}
