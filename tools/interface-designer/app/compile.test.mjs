// node --test tools/interface-designer/app/compile.test.mjs
//
// Every template and widget compiles to a design serve.py accepts, with the layout maths and
// naming the simple editor promises.

import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { test } from 'node:test';
import { fileURLToPath } from 'node:url';

import { compile, gridCells } from './compile.js';
import { makeWidget, newPanel, TEMPLATES, WIDGETS } from './widgets.js';

const SERVE = join(dirname(fileURLToPath(import.meta.url)), '..', 'serve.py');

/** Runs the design through serve.py --normalise, the same validation a save gets. */
function normalise(design) {
  const file = join(mkdtempSync(join(tmpdir(), 'compile-')), 'd.interface.json');
  writeFileSync(file, JSON.stringify(design));
  return JSON.parse(execFileSync('python3', [SERVE, '--normalise', file], { encoding: 'utf8' }));
}

function panelWith(...types) {
  const panel = newPanel('Demo', 'blank');
  for (const type of types) panel.widgets.push(makeWidget(panel, type, 20, 40));
  return panel;
}

const byName = (design, name) => design.components.find((c) => c.name === name);

test('every template compiles to a design serve.py accepts', () => {
  for (const template of TEMPLATES) {
    const design = compile(newPanel(template.label, template.id));
    const names = design.components.map((c) => c.name);
    assert.equal(new Set(names).size, names.length, `${template.id}: duplicate names`);
    assert.deepEqual(normalise(design).components.map((c) => c.name), names);
  }
});

test('every widget type compiles alone', () => {
  for (const type of Object.keys(WIDGETS)) {
    const design = compile(panelWith(type));
    assert.ok(design.components.length > 2, type);
    normalise(design);
  }
});

test('the root is the panel, centred and swallowing clicks, with a titled frame', () => {
  const design = compile(newPanel('Bank tabs', 'blank'));
  assert.equal(design.interface, 'bank_tabs');
  assert.deepEqual(design.components[0], {
    name: 'root', type: 'layer', w: 400, h: 260, xMode: 'centre', yMode: 'centre', clickThrough: false,
  });
  assert.equal(byName(design, 'frame').frame, 'Bank tabs');
});

test('a button is placed where the widget is, with its label and what it does', () => {
  const panel = panelWith('button');
  Object.assign(panel.widgets[0], { x: 30, y: 50, w: 140, h: 30, label: 'Buy', action: 'Buys one.' });
  const design = compile(panel);
  assert.deepEqual(
    { ...byName(design, 'button_1') },
    { name: 'button_1', parent: 'root', type: 'layer', x: 30, y: 50, w: 140, h: 30, ops: ['Buy'], notes: 'Buys one.' },
  );
  assert.equal(byName(design, 'button_1_label').text, 'Buy');
  // 30 high is a tall button, so the label goes bold.
  assert.equal(byName(design, 'button_1_label').font, 496);
});

test('sprite button styles keep their own size', () => {
  const panel = panelWith('button');
  Object.assign(panel.widgets[0], { style: 'grey', w: 300, h: 90 });
  const design = compile(panel);
  assert.equal(byName(design, 'button_1').w, 85);
  assert.equal(byName(design, 'button_1_bg').sprite, 812);
});

test('tabs share the width and only the first starts selected', () => {
  const panel = panelWith('tabs');
  Object.assign(panel.widgets[0], { x: 10, w: 360, labels: ['A', 'B', ' ', 'C'] });
  const design = compile(panel);
  // The blank label is dropped, so three tabs of 120.
  assert.deepEqual([0, 1, 2].map((i) => byName(design, `tabs_1_${i}`).x), [10, 130, 250]);
  assert.equal(byName(design, 'tabs_1_2').w, 120);
  assert.equal(byName(design, 'tabs_1_0_on').hidden, undefined);
  assert.equal(byName(design, 'tabs_1_0_off').hidden, true);
  assert.equal(byName(design, 'tabs_1_1_on').hidden, true);
  assert.equal(byName(design, 'tabs_1_2_label').text, 'C');
});

test('grid cells fill row by row with a 4px gap', () => {
  const cells = gridCells({ x: 10, y: 20, w: 300, h: 100, rows: 2, cols: 3 });
  assert.equal(cells.length, 6);
  assert.deepEqual(cells[0], { x: 10, y: 20, w: 97, h: 48 });
  assert.deepEqual(cells[4], { x: 111, y: 72, w: 97, h: 48 });
});

test('grid labels fill the cells and item grids centre a slot in each cell', () => {
  const panel = panelWith('grid');
  Object.assign(panel.widgets[0], { x: 0, y: 0, w: 300, h: 100, rows: 2, cols: 3, labels: ['Varrock', 'Falador'] });
  let design = compile(panel);
  assert.equal(byName(design, 'grid_1_1_label').text, 'Falador');
  assert.equal(byName(design, 'grid_1_2_label').text, undefined);
  assert.equal(byName(design, 'grid_1_0').opBase, '<col=ff9040>Varrock</col>');

  panel.widgets[0].cells = 'items';
  design = compile(panel);
  const slot = byName(design, 'grid_1_4');
  assert.equal(slot.type, 'item');
  // Cell 4 is at x 101, 97 wide: the 36-wide slot sits 30 in.
  assert.deepEqual([slot.x, slot.y, slot.w, slot.h], [131, 60, 36, 32]);
});

test('boxes compile first, so they sit behind', () => {
  const panel = panelWith('button', 'box');
  const names = compile(panel).components.map((c) => c.name);
  assert.ok(names.indexOf('box_1') < names.indexOf('button_1'));
});

test('an item slot notes its example and only gets an op when it does something', () => {
  const panel = panelWith('item');
  let slot = byName(compile(panel), 'item_1');
  assert.equal(slot.type, 'item');
  assert.equal(slot.ops, undefined);
  assert.equal(slot.notes, 'Shows e.g. Rune scimitar.');
  panel.widgets[0].action = 'Buys it.';
  slot = byName(compile(panel), 'item_1');
  assert.deepEqual(slot.ops, ['Select']);
});

test('text keeps line breaks and drops the shadow only for black', () => {
  const panel = panelWith('text');
  Object.assign(panel.widgets[0], { text: 'one\ntwo', colour: 'black' });
  const text = byName(compile(panel), 'text_1');
  assert.equal(text.text, 'one<br>two');
  assert.equal(text.shadow, undefined);
  assert.equal(text.colour, undefined); // black is the default colour
});
