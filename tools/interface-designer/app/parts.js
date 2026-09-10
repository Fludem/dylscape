// The vanilla parts palette. Each part inserts plain components, already dressed and hooked the
// way the Kotlin panels do it (TeleportPanelBuilder), with every sprite and script copied off a
// vanilla interface that renders it. After insertion they are ordinary components.

const ORANGE = 'ff981f';
const WHITE = 'ffffff';
const OUTLINE = '0e0e0c';
const INNER_OUTLINE = '474745';

const fill = { wMode: 'minus', hMode: 'minus' };
const insetOne = { x: 1, y: 1, w: 2, h: 2, wMode: 'minus', hMode: 'minus' };

/** Sprites known to render well in a server-authored panel; pinned at the top of the picker. */
export const FAVOURITE_SPRITES = [
  [297, 'Stone panel background (tile)'],
  [1040, 'Parchment background (tile)'],
  [998, 'Tab selected, left'], [999, 'Tab selected, middle'], [1000, 'Tab selected, right'],
  [1001, 'Tab unselected, left'], [1002, 'Tab unselected, middle'], [1003, 'Tab unselected, right'],
  [812, 'Button 85x22 grey'], [813, 'Button 85x22 red'],
  [295, 'Button 72x36 grey'], [296, 'Button 72x36 red'],
  [1701, 'Button 132x28'],
  [5779, 'Button 58x24 dark'], [5780, 'Button 58x24 light'], [5781, 'Button 58x24 red'],
  [535, 'Close button'],
];

/** The label on every button part: centred, shadowed orange that turns white under the mouse. */
function label(name, parent, text, font) {
  return {
    name, parent, type: 'text', ...fill, colour: ORANGE, text, font,
    alignH: 'centre', alignV: 'centre', shadow: true, hover: { colour: WHITE },
  };
}

function spriteButton(base, parent, rest, hover, w, h, text) {
  return [
    { name: base, parent, type: 'layer', x: 8, y: 40, w, h, ops: ['Select'] },
    { name: `${base}_bg`, parent: base, type: 'graphic', w, h, sprite: rest, hover: hover ? { sprite: hover } : null },
    label(`${base}_label`, base, text, 494),
  ];
}

function tabArt(base, parent, [left, middle, right], hidden) {
  return [
    { name: base, parent, type: 'layer', ...fill, hidden },
    { name: `${base}_left`, parent: base, type: 'graphic', w: 20, h: 20, sprite: left },
    { name: `${base}_middle`, parent: base, type: 'graphic', xMode: 'centre', w: 40, wMode: 'minus', h: 20, sprite: middle, tiling: true },
    { name: `${base}_right`, parent: base, type: 'graphic', xMode: 'end', w: 20, h: 20, sprite: right },
  ];
}

export const PARTS = [
  {
    group: 'Basic', id: 'layer', label: 'Layer', base: 'group',
    hint: 'An invisible box that holds other components. Give it ops to make an area clickable.',
    make: (n, p) => [{ name: n, parent: p, type: 'layer', x: 8, y: 40, w: 120, h: 60 }],
  },
  {
    group: 'Basic', id: 'rect', label: 'Rectangle', base: 'box',
    hint: 'A flat colour, filled or outlined, optionally translucent.',
    make: (n, p) => [{ name: n, parent: p, type: 'rect', x: 8, y: 40, w: 120, h: 60, colour: OUTLINE, filled: true }],
  },
  {
    group: 'Basic', id: 'text', label: 'Text', base: 'label',
    hint: 'One or more lines in a cache font. <br> breaks a line, <col=rrggbb> recolours.',
    make: (n, p) => [{ name: n, parent: p, type: 'text', x: 8, y: 40, w: 120, h: 16, colour: ORANGE, text: 'Text', font: 495, shadow: true }],
  },
  {
    group: 'Basic', id: 'graphic', label: 'Sprite', base: 'sprite',
    hint: 'Any cache sprite. Tiled sprites repeat to fill; others draw at natural size.',
    make: (n, p) => [{ name: n, parent: p, type: 'graphic', x: 8, y: 40, w: 36, h: 32, sprite: 535 }],
  },
  {
    group: 'Vanilla', id: 'frame', label: 'Steelborder frame', base: 'frame',
    hint: 'Stone background, steel edges, orange title and a working close button (127 vanilla panels).',
    make: (n, p) => [{ name: n, parent: p, type: 'layer', ...fill, frame: 'Title' }],
  },
  {
    group: 'Vanilla', id: 'contents', label: 'Content area', base: 'contents',
    hint: 'A layer filling the frame below its title bar: y 36, 8px in from each side.',
    make: (n, p) => [{ name: n, parent: p, type: 'layer', y: 36, xMode: 'centre', w: 16, wMode: 'minus', h: 44, hMode: 'minus' }],
  },
  {
    group: 'Vanilla', id: 'inset', label: 'Inset button', base: 'button',
    hint: 'slayer_rewards box: dark outline, faint fill that brightens on hover, label that turns white.',
    make: (n, p) => [
      { name: n, parent: p, type: 'layer', x: 8, y: 40, w: 150, h: 22, ops: ['Select'] },
      { name: `${n}_outline`, parent: n, type: 'rect', ...fill, colour: OUTLINE },
      { name: `${n}_fill`, parent: n, type: 'rect', ...insetOne, colour: WHITE, filled: true, trans: 238, hover: { trans: 205 } },
      { name: `${n}_inner`, parent: n, type: 'rect', ...insetOne, colour: INNER_OUTLINE },
      label(`${n}_label`, n, 'Button', 494),
    ],
  },
  {
    group: 'Vanilla', id: 'well', label: 'Dark well', base: 'well',
    hint: 'A recessed area darker than the stone, for grids and lists to sit in.',
    make: (n, p) => [
      { name: n, parent: p, type: 'layer', x: 8, y: 40, w: 200, h: 100 },
      { name: `${n}_bg`, parent: n, type: 'rect', ...fill, colour: '000000', filled: true, trans: 190 },
      { name: `${n}_outline`, parent: n, type: 'rect', ...fill, colour: OUTLINE },
      { name: `${n}_inner`, parent: n, type: 'rect', ...insetOne, colour: INNER_OUTLINE },
    ],
  },
  {
    group: 'Vanilla', id: 'tab', label: 'Tab', base: 'tab',
    hint: 'ii_tracker tab: selected and unselected art both packed; the server swaps them with ifSetHide.',
    make: (n, p) => [
      { name: n, parent: p, type: 'layer', x: 3, y: 0, w: 150, h: 20, ops: ['View'] },
      ...tabArt(`${n}_on`, n, [998, 999, 1000], false),
      ...tabArt(`${n}_off`, n, [1001, 1002, 1003], true),
      label(`${n}_label`, n, 'Tab', 496),
    ],
  },
  {
    group: 'Buttons', id: 'button_85', label: 'Grey button 85x22', base: 'button',
    hint: 'Sprite 812, swapping to red 813 under the mouse.',
    make: (n, p) => spriteButton(n, p, 812, 813, 85, 22, 'Button'),
  },
  {
    group: 'Buttons', id: 'button_72', label: 'Grey button 72x36', base: 'button',
    hint: 'Sprite 295, swapping to red 296 under the mouse.',
    make: (n, p) => spriteButton(n, p, 295, 296, 72, 36, 'Button'),
  },
  {
    group: 'Buttons', id: 'button_58', label: 'Dark button 58x24', base: 'button',
    hint: 'Sprite 5779, swapping to light 5780 under the mouse.',
    make: (n, p) => spriteButton(n, p, 5779, 5780, 58, 24, 'OK'),
  },
  {
    group: 'Buttons', id: 'button_132', label: 'Wide button 132x28', base: 'button',
    hint: 'Sprite 1701; only the label reacts to hover.',
    make: (n, p) => spriteButton(n, p, 1701, null, 132, 28, 'Button'),
  },
];
