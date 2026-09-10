// The sprite picker, shared by both editors. It builds its own <dialog>, so a page needs no markup
// for it; the look comes from style.css (.picker, .picker-grid).
//
// Resolved straight from clicks and cancel, never from the dialog's `close` event: that event is
// queued, and Chrome holds it back entirely in a tab that is not rendering.

import { FAVOURITE_SPRITES } from './parts.js';

const LIMIT = 400;
let dialog = null;

function build() {
  dialog = document.createElement('dialog');
  dialog.className = 'picker';
  dialog.innerHTML = `
    <form method="dialog">
      <header>
        <h3>Choose a picture</h3>
        <input class="picker-search" placeholder="Search by number, range (990-1010) or size (85x22)" autocomplete="off">
        <button type="button" class="picker-close">Close</button>
      </header>
      <div class="picker-grid"></div>
      <p class="picker-more hint"></p>
    </form>`;
  // Enter in the search box would otherwise submit the form and close the picker.
  dialog.querySelector('form').addEventListener('submit', (e) => e.preventDefault());
  document.body.append(dialog);
}

/** Resolves to the chosen sprite id, or null if closed. */
export function pickSprite(assets, current) {
  if (!dialog) build();
  const search = dialog.querySelector('.picker-search');
  const grid = dialog.querySelector('.picker-grid');
  const more = dialog.querySelector('.picker-more');
  let finish = () => {};

  const tile = (id, label) => {
    const meta = assets.spriteIndex.get(id);
    const b = document.createElement('button');
    b.type = 'button';
    b.title = label ?? '';
    const img = document.createElement('img');
    img.src = assets.spriteUrl(id);
    img.loading = 'lazy';
    img.alt = '';
    const caption = document.createElement('span');
    caption.className = 'label';
    caption.textContent = `${id}  ${meta ? `${meta.w}x${meta.h}` : '?'}${label ? `\n${label}` : ''}`;
    b.append(img, caption);
    if (id === current) b.style.borderColor = 'var(--accent)';
    b.addEventListener('click', () => finish(id));
    return b;
  };

  const heading = (text) => {
    const div = document.createElement('div');
    div.className = 'fav';
    div.textContent = text;
    return div;
  };

  const show = () => {
    const q = search.value.trim().toLowerCase();
    grid.innerHTML = '';
    let ids = [...assets.spriteIndex.keys()];
    const range = q.match(/^(\d+)\s*-\s*(\d+)$/);
    const size = q.match(/^(\d+)\s*x\s*(\d+)$/);
    if (range) ids = ids.filter((id) => id >= +range[1] && id <= +range[2]);
    else if (size) {
      ids = ids.filter((id) => {
        const m = assets.spriteIndex.get(id);
        return m.w === +size[1] && m.h === +size[2];
      });
    } else if (/^\d+$/.test(q)) ids = ids.filter((id) => String(id).startsWith(q));
    if (!q) {
      grid.append(heading('Good for panels'));
      for (const [id, label] of FAVOURITE_SPRITES) grid.append(tile(id, label));
      grid.append(heading('Everything in the game'));
    }
    for (const id of ids.slice(0, LIMIT)) grid.append(tile(id));
    more.textContent = ids.length > LIMIT
      ? `Showing ${LIMIT} of ${ids.length}; search to narrow it down.`
      : `${ids.length} pictures`;
  };

  search.value = '';
  search.oninput = show;
  show();
  return new Promise((resolve) => {
    const cancel = (e) => {
      e.preventDefault();
      finish(null);
    };
    finish = (id) => {
      dialog.removeEventListener('cancel', cancel);
      if (dialog.open) dialog.close();
      resolve(id);
    };
    dialog.addEventListener('cancel', cancel);
    dialog.querySelector('.picker-close').onclick = () => finish(null);
    dialog.showModal();
    search.focus();
  });
}
