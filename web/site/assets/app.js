/* Onyx — shared front-end helpers. No dependencies; every page is plain HTML that calls into this.
 *
 * The API is same-origin in production (Caddy routes /api/* to the local service). For local
 * development, set window.ONYX_API before this script loads to point elsewhere.
 */
(function () {
  "use strict";

  var API = window.ONYX_API || "/api";

  /* ---- utilities -------------------------------------------------------------------------- */

  function el(tag, attrs, children) {
    var node = document.createElement(tag);
    if (attrs) {
      Object.keys(attrs).forEach(function (k) {
        if (k === "class") node.className = attrs[k];
        else if (k === "text") node.textContent = attrs[k];
        else if (k === "html") node.innerHTML = attrs[k];
        else if (attrs[k] != null) node.setAttribute(k, attrs[k]);
      });
    }
    (children || []).forEach(function (c) {
      if (c) node.appendChild(typeof c === "string" ? document.createTextNode(c) : c);
    });
    return node;
  }

  function num(n) {
    return (n == null ? 0 : n).toLocaleString("en-GB");
  }

  /* The API hands back ISO-8601 with an explicit offset. It has to: the game's schema stores
     last_login in the host's local zone and everything else in UTC, and only the server is in a
     position to tell them apart. */
  function parseStamp(s) {
    if (!s) return null;
    var d = new Date(s);
    return isNaN(d.getTime()) ? null : d;
  }

  function ago(stamp) {
    var d = parseStamp(stamp);
    if (!d) return "unknown";
    var secs = Math.max(0, (Date.now() - d.getTime()) / 1000);
    var units = [[60, "second"], [60, "minute"], [24, "hour"], [7, "day"], [4.35, "week"],
                 [12, "month"]];
    var value = secs, name = "second";
    for (var i = 0; i < units.length; i++) {
      if (value < units[i][0]) { name = units[i][1]; break; }
      value = value / units[i][0];
      name = i + 1 < units.length ? units[i + 1][1] : "year";
    }
    var rounded = Math.floor(value);
    return rounded + " " + name + (rounded === 1 ? "" : "s") + " ago";
  }

  function get(path) {
    return fetch(API + path, { headers: { Accept: "application/json" } }).then(function (res) {
      return res.json().then(function (body) {
        if (!res.ok) throw new Error(body.error || "request failed");
        return body;
      });
    });
  }

  function getJson(path) {
    return fetch(path).then(function (res) {
      if (!res.ok) throw new Error("could not load " + path);
      return res.json();
    });
  }

  function fail(target, err) {
    if (!target) return;
    target.innerHTML = "";
    target.appendChild(el("div", { class: "error", text: "Could not load this: " + err.message }));
  }

  /* ---- shared chrome ---------------------------------------------------------------------- */

  var MODE_LABEL = { standard: null, ironman: "IM", uim: "UIM", hcim: "HCIM" };

  function modeBadge(mode) {
    var label = MODE_LABEL[mode];
    return label ? el("span", { class: "badge " + mode, text: label }) : null;
  }

  function playerLink(name) {
    return el("a", { href: "/player.html?name=" + encodeURIComponent(name), text: name });
  }

  /* The header pill on every page. Also the site's only liveness check. */
  function mountStatusPill() {
    var pill = document.querySelector("[data-status-pill]");
    if (!pill) return;
    get("/status").then(function (s) {
      pill.innerHTML = "";
      pill.appendChild(el("span", { class: "dot on" }));
      pill.appendChild(el("span", { text: s.online + (s.online === 1 ? " player" : " players")
                                          + " online" }));
    }).catch(function () {
      pill.innerHTML = "";
      pill.appendChild(el("span", { class: "dot off" }));
      pill.appendChild(el("span", { text: "status unavailable" }));
    });
  }

  function markCurrentNav() {
    var here = location.pathname.replace(/index\.html$/, "").replace(/\/$/, "") || "/";
    document.querySelectorAll(".site-nav a").forEach(function (link) {
      var target = link.getAttribute("href").replace(/index\.html$/, "").replace(/\/$/, "") || "/";
      // A guide sub-page should still light up the Guides tab.
      if (target === here || (target !== "/" && here.indexOf(target) === 0)) {
        link.setAttribute("aria-current", "page");
      }
    });
  }

  window.Onyx = {
    el: el, num: num, ago: ago, get: get, getJson: getJson, fail: fail,
    modeBadge: modeBadge, playerLink: playerLink, parseStamp: parseStamp,
  };

  document.addEventListener("DOMContentLoaded", function () {
    markCurrentNav();
    mountStatusPill();
    var year = document.querySelector("[data-year]");
    if (year) year.textContent = new Date().getFullYear();
  });
})();
