// Lays out and draws a design the way the OSRS client does. A port of
// tools/interface-mockup/PanelMockup.java (mode maths, sprite tiling, bitmap text, the
// steelborder emulation) plus layer clipping, which the client does and the mockup does not:
// a component never draws outside the layer it sits in.

export const MARGIN = 20;
export const MODAL = { w: 512, h: 334 };
export const CANVAS = { w: MODAL.w + MARGIN * 2, h: MODAL.h + MARGIN * 2 };
export const CONTAINER = { x: MARGIN, y: MARGIN, w: MODAL.w, h: MODAL.h };

// A game-world-ish backdrop so translucency reads as it would in game (PanelMockup's colour).
const BACKDROP = '#4a5a3a';

// Must match DEFAULTS in serve.py and DesignComponent in DesignedComponentBuilder.kt.
export const DEFAULTS = {
  x: 0, y: 0, w: 0, h: 0,
  xMode: 'start', yMode: 'start', wMode: 'fixed', hMode: 'fixed',
  hidden: false, clickThrough: true, frame: '', colour: '000000', filled: false,
  trans: 0, sprite: -1, tiling: false, text: '', font: -1, alignH: 'left',
  alignV: 'top', lineHeight: 0, shadow: false, hover: null, ops: [], opBase: '',
  notes: '',
};

export const KEY_ORDER = [
  'name', 'parent', 'type', 'x', 'y', 'w', 'h', 'xMode', 'yMode', 'wMode', 'hMode',
  'hidden', 'clickThrough', 'frame', 'colour', 'filled', 'trans', 'sprite', 'tiling',
  'text', 'font', 'alignH', 'alignV', 'lineHeight', 'shadow', 'hover', 'ops', 'opBase', 'notes',
];

const ALIGN = { left: 0, top: 0, centre: 1, right: 2, bottom: 2 };

/** A component with every default filled in, so the editor never has to ask "is it set?". */
export function withDefaults(c) {
  return { ...structuredClone(DEFAULTS), parent: null, ...structuredClone(c) };
}

/** Back to the on-disk shape: canonical key order, defaults and nulls left out. */
export function stripDefaults(c) {
  const out = {};
  for (const key of KEY_ORDER) {
    const value = c[key];
    if (value === undefined || value === null) continue;
    if (key in DEFAULTS && JSON.stringify(value) === JSON.stringify(DEFAULTS[key])) continue;
    out[key] = value;
  }
  return out;
}

export const hex = (n) => '#' + (n >>> 0).toString(16).padStart(6, '0').slice(-6);

// ---------------------------------------------------------------------------------------------
// Cache art

export class Assets {
  constructor(base) {
    this.base = base;
    this.spriteIndex = new Map(); // id -> { w, h, frames }
    this.fonts = new Map(); // id -> { name, cellW, cellH, ascent, advances, img }
    this.sprites = new Map(); // id -> Image
    this.tinted = new Map();
    this.onchange = () => {};
  }

  async init() {
    const [sprites, fonts] = await Promise.all([
      fetch(this.base + 'sprites.json').then((r) => r.json()),
      fetch(this.base + 'fonts.json').then((r) => r.json()),
    ]);
    for (const [id, w, h, frames] of sprites.sprites) this.spriteIndex.set(id, { w, h, frames });
    await Promise.all(fonts.fonts.map(async (font) => {
      const img = await loadImage(`${this.base}fonts/${font.id}.png`);
      this.fonts.set(font.id, { ...font, img });
    }));
  }

  spriteUrl(id) {
    return `${this.base}sprites/${id}.png`;
  }

  /**
   * The decoded sprite, or null while it loads. Readiness is read off the image itself: `onload`
   * only schedules the redraw, and a browser can deliver it late (Chrome holds it back in a tab
   * that is not rendering) while the pixels are already there.
   */
  sprite(id) {
    let img = this.sprites.get(id);
    if (!img) {
      if (!this.spriteIndex.has(id)) return null;
      img = new Image();
      img.onload = () => this.onchange();
      img.src = this.spriteUrl(id);
      this.sprites.set(id, img);
    }
    return img.complete && img.naturalWidth > 0 ? img : null;
  }

  /** The font's glyph atlas recoloured, since the client draws every opaque glyph pixel in one colour. */
  glyphs(fontId, colour) {
    const key = `${fontId}:${colour}`;
    let atlas = this.tinted.get(key);
    if (!atlas) {
      const font = this.fonts.get(fontId);
      atlas = document.createElement('canvas');
      atlas.width = font.img.width;
      atlas.height = font.img.height;
      const g = atlas.getContext('2d');
      g.drawImage(font.img, 0, 0);
      g.globalCompositeOperation = 'source-in';
      g.fillStyle = hex(colour);
      g.fillRect(0, 0, atlas.width, atlas.height);
      this.tinted.set(key, atlas);
    }
    return atlas;
  }

  /** Pixel width of each line of `text`, ignoring <col> and <br> tags. */
  lineWidths(fontId, text) {
    const font = this.fonts.get(fontId);
    if (!font) return [];
    return tokens(text, 0).map((line) => lineWidth(font, line));
  }
}

function loadImage(src) {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error(`could not load ${src}`));
    img.src = src;
  });
}

// ---------------------------------------------------------------------------------------------
// Layout

/** Size modes: fixed pixels, or the parent's size minus the value. */
export function size(value, mode, parent) {
  return mode === 'minus' ? parent - value : value;
}

/** Position modes. Java's integer division truncates toward zero, hence Math.trunc. */
export function pos(value, mode, parent, self) {
  if (mode === 'centre') return Math.trunc((parent - self) / 2) + value;
  if (mode === 'end') return parent - self - value;
  return value;
}

/** The authored value that places a component of size `self` at `offset` inside its parent. */
export function invPos(offset, mode, parent, self) {
  if (mode === 'centre') return offset - Math.trunc((parent - self) / 2);
  if (mode === 'end') return parent - self - offset;
  return offset;
}

/**
 * Absolute rects for every component, the clip each draws within, and the visible draw order:
 * depth first, children in child-index order, as the client draws them.
 */
export function layout(components, showHidden) {
  const index = new Map(components.map((c, i) => [c.name, i]));
  const children = components.map(() => []);
  components.forEach((c, i) => {
    if (i > 0 && index.has(c.parent)) children[index.get(c.parent)].push(i);
  });
  const rects = new Array(components.length);
  const clips = new Array(components.length);
  const visible = new Array(components.length).fill(false);
  const order = [];

  const place = (i, parent, clip, shown) => {
    const c = components[i];
    const w = size(c.w, c.wMode, parent.w);
    const h = size(c.h, c.hMode, parent.h);
    const rect = {
      x: parent.x + pos(c.x, c.xMode, parent.w, w),
      y: parent.y + pos(c.y, c.yMode, parent.h, h),
      w,
      h,
    };
    rects[i] = rect;
    clips[i] = clip;
    visible[i] = shown && (!c.hidden || showHidden);
    if (visible[i]) order.push(i);
    const inner = c.type === 'layer' ? intersect(clip, rect) : clip;
    for (const k of children[i]) place(k, rect, inner, visible[i]);
  };
  if (components.length) place(0, CONTAINER, CONTAINER, true);
  return { index, children, rects, clips, visible, order };
}

export function intersect(a, b) {
  const x = Math.max(a.x, b.x);
  const y = Math.max(a.y, b.y);
  return {
    x,
    y,
    w: Math.max(0, Math.min(a.x + a.w, b.x + b.w) - x),
    h: Math.max(0, Math.min(a.y + a.h, b.y + b.h) - y),
  };
}

export function inside(p, r) {
  return p.x >= r.x && p.y >= r.y && p.x < r.x + r.w && p.y < r.y + r.h;
}

// ---------------------------------------------------------------------------------------------
// Drawing

/**
 * Draws the design into `ctx` at 1x. `hoverPoint` applies each component's hover value while the
 * point is over that component, as the client fires onMouseOver per component: a sibling drawn
 * on top does not block it.
 */
export function render(ctx, components, lay, assets, hoverPoint) {
  ctx.save();
  ctx.fillStyle = BACKDROP;
  ctx.fillRect(0, 0, CANVAS.w, CANVAS.h);
  for (const i of lay.order) {
    const c = components[i];
    const r = lay.rects[i];
    const hovered = hoverPoint && c.hover && inside(hoverPoint, intersect(r, lay.clips[i]));
    const clip = c.type === 'layer' ? intersect(lay.clips[i], r) : lay.clips[i];
    ctx.save();
    ctx.beginPath();
    ctx.rect(clip.x, clip.y, clip.w, clip.h);
    ctx.clip();
    draw(ctx, c, r, assets, hovered ? c.hover : null);
    ctx.restore();
  }
  ctx.restore();
}

function draw(ctx, c, r, assets, hover) {
  switch (c.type) {
    case 'layer':
      if (c.frame) steelborder(ctx, assets, r.x, r.y, r.w, r.h, c.frame);
      break;
    case 'rect': {
      const trans = hover?.trans ?? c.trans;
      ctx.globalAlpha = (255 - trans) / 255;
      ctx.fillStyle = '#' + c.colour;
      if (c.filled) ctx.fillRect(r.x, r.y, r.w, r.h);
      else outline(ctx, r.x, r.y, r.w, r.h);
      ctx.globalAlpha = 1;
      break;
    }
    case 'text': {
      const colour = parseInt(hover?.colour ?? c.colour, 16);
      text(ctx, assets, c.font, c.text, r, colour, c.shadow, ALIGN[c.alignH], ALIGN[c.alignV], c.lineHeight);
      break;
    }
    case 'graphic': {
      const trans = hover?.trans ?? c.trans;
      ctx.globalAlpha = (255 - trans) / 255;
      sprite(ctx, assets, hover?.sprite ?? c.sprite, r.x, r.y, r.w, r.h, c.tiling);
      ctx.globalAlpha = 1;
      break;
    }
  }
}

function outline(ctx, x, y, w, h) {
  if (w <= 0 || h <= 0) return;
  ctx.fillRect(x, y, w, 1);
  if (h > 1) ctx.fillRect(x, y + h - 1, w, 1);
  if (h > 2) {
    ctx.fillRect(x, y + 1, 1, h - 2);
    if (w > 1) ctx.fillRect(x + w - 1, y + 1, 1, h - 2);
  }
}

/** Tiled sprites repeat from the top-left; others draw once at natural size, never stretched. */
function sprite(ctx, assets, id, x, y, w, h, tiling) {
  const img = assets.sprite(id);
  if (!img) return;
  if (!tiling) {
    ctx.drawImage(img, x, y);
    return;
  }
  for (let ty = 0; ty < h; ty += img.height) {
    for (let tx = 0; tx < w; tx += img.width) {
      const cw = Math.min(img.width, w - tx);
      const ch = Math.min(img.height, h - ty);
      ctx.drawImage(img, 0, 0, cw, ch, x + tx, y + ty, cw, ch);
    }
  }
}

/** [proc,steelborder] as decoded from its bytecode; see PanelMockup.steelborder. */
function steelborder(ctx, assets, x, y, w, h, title) {
  sprite(ctx, assets, 297, x + 1, y + 1, w - 2, h - 2, true);
  text(ctx, assets, 496, title, { x: x + 6, y: y + 6, w: w - 12, h: 24 }, 0xff981f, true, 1, 1, 0);
  sprite(ctx, assets, 310, x, y, 25, 30, false);
  sprite(ctx, assets, 311, x + w - 25, y, 25, 30, false);
  sprite(ctx, assets, 312, x, y + h - 30, 25, 30, false);
  sprite(ctx, assets, 313, x + w - 25, y + h - 30, 25, 30, false);
  sprite(ctx, assets, 172, x - 15, y + 30, 36, h - 60, true);
  sprite(ctx, assets, 315, x + w - 36 + 15, y + 30, 36, h - 60, true);
  sprite(ctx, assets, 314, x + 25, y - 15, w - 50, 36, true);
  sprite(ctx, assets, 173, x + 25, y + h - 36 + 15, w - 50, 36, true);
  sprite(ctx, assets, 535, x + w - 26 - 3, y + 6, 26, 23, false);
}

/** Splits text into lines of coloured runs, honouring <col=rrggbb>, </col> and <br>. */
export function tokens(s, base) {
  const lines = [[]];
  const re = /<col=([0-9a-fA-F]{6})>|<\/col>|<br>/g;
  let colour = base;
  let last = 0;
  let m;
  while ((m = re.exec(s))) {
    if (m.index > last) lines.at(-1).push({ text: s.slice(last, m.index), colour });
    if (m[1]) colour = parseInt(m[1], 16);
    else if (m[0] === '</col>') colour = base;
    else lines.push([]);
    last = re.lastIndex;
  }
  if (last < s.length) lines.at(-1).push({ text: s.slice(last), colour });
  return lines;
}

function lineWidth(font, runs) {
  let w = 0;
  for (const run of runs) for (const ch of run.text) w += font.advances[ch.charCodeAt(0) & 0xff];
  return w;
}

/**
 * Bitmap text. A single line is placed exactly as PanelMockup places it (centred on the font's
 * ascent); extra <br> lines step down by `lineHeight`, or the cell height when that is 0.
 */
function text(ctx, assets, fontId, s, r, colour, shadow, alignH, alignV, lineHeight) {
  const font = assets.fonts.get(fontId);
  if (!font || !s) return;
  const lines = tokens(s, colour);
  const step = lineHeight > 0 ? lineHeight : font.cellH;
  const blockH = font.ascent + (lines.length - 1) * step;
  const top =
    alignV === 1 ? r.y + Math.trunc((r.h - blockH) / 2) : alignV === 2 ? r.y + r.h - blockH : r.y;
  lines.forEach((runs, n) => {
    const tw = lineWidth(font, runs);
    const penX =
      alignH === 1 ? r.x + Math.trunc((r.w - tw) / 2) : alignH === 2 ? r.x + r.w - tw : r.x;
    const y = top + n * step;
    for (let pass = shadow ? 0 : 1; pass < 2; pass++) {
      let pen = penX + (pass === 0 ? 1 : 0);
      for (const run of runs) {
        const atlas = assets.glyphs(fontId, pass === 0 ? 0 : run.colour);
        for (const ch of run.text) {
          const code = ch.charCodeAt(0) & 0xff;
          const sx = (code % 16) * font.cellW;
          const sy = Math.floor(code / 16) * font.cellH;
          ctx.drawImage(atlas, sx, sy, font.cellW, font.cellH, pen, y + (pass === 0 ? 1 : 0), font.cellW, font.cellH);
          pen += font.advances[code];
        }
      }
    }
  });
}
