#!/usr/bin/env python3
"""The Onyx website's JSON API.

A read-only view of the game's save database, served on localhost for Caddy to reverse-proxy under
`/api/`. Stdlib only, in keeping with the rest of `tools/` - there is no package manager anywhere in
this repo and this service is not worth introducing one for.

Run locally against the dev realm:

    ONYX_DB=.data/saves/game.db ONYX_REALM=1 python3 web/api/hiscores.py

In production systemd sets `ONYX_DB=/opt/rsmod/.data/saves/game.db` and `ONYX_REALM=2`, and the
service runs as the `rsmod` user so it can read the WAL sidecar files.
"""

from __future__ import annotations

import json
import os
import sqlite3
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from onyxdb import ACCOUNT_MODES, SKILLS, GameDatabase, skill_label  # noqa: E402

DB_PATH = os.environ.get("ONYX_DB", ".data/saves/game.db")
REALM_ID = int(os.environ.get("ONYX_REALM", "2"))
BIND = os.environ.get("ONYX_BIND", "127.0.0.1")
PORT = int(os.environ.get("ONYX_PORT", "8081"))
# Only used for local development; in production Caddy serves the static files and the API is
# reached through it, so same-origin applies and no CORS header is needed.
STATIC_ROOT = os.environ.get("ONYX_STATIC")

VALID_MODES = set(ACCOUNT_MODES.values()) | {"all"}

db = GameDatabase(DB_PATH, REALM_ID)


class Handler(BaseHTTPRequestHandler):
    server_version = "onyx-web"
    protocol_version = "HTTP/1.1"

    def log_message(self, fmt, *args):  # noqa: A002
        sys.stderr.write("%s - %s\n" % (self.address_string(), fmt % args))

    def _send(self, status: int, payload: dict | list) -> None:
        body = json.dumps(payload).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "public, max-age=15")
        if STATIC_ROOT:
            self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self) -> None:  # noqa: N802
        url = urlparse(self.path)
        query = parse_qs(url.query)
        route = url.path.rstrip("/") or "/"
        try:
            self._route(route, query)
        except sqlite3.Error as exc:
            # The database is a live file owned by the game. If it is mid-checkpoint or the service
            # lost read access, say so plainly rather than returning an empty leaderboard that looks
            # like "nobody has played yet".
            self._send(503, {"error": "database unavailable", "detail": str(exc)})
        except Exception as exc:  # noqa: BLE001
            self._send(500, {"error": "internal error", "detail": str(exc)})

    def _route(self, route: str, query: dict) -> None:
        first = lambda k, d="": (query.get(k) or [d])[0]  # noqa: E731

        if route == "/api/status":
            self._send(200, db.status())

        elif route == "/api/skills":
            self._send(200, {"skills": [{"key": s, "label": skill_label(s)} for s in SKILLS]})

        elif route == "/api/hiscores":
            skill = first("skill", "overall")
            if skill != "overall" and skill not in SKILLS:
                self._send(400, {"error": f"unknown skill: {skill}"})
                return
            mode = first("mode", "all")
            if mode not in VALID_MODES:
                self._send(400, {"error": f"unknown mode: {mode}"})
                return
            limit = max(1, min(int(first("limit", "50") or 50), 200))
            offset = max(0, int(first("offset", "0") or 0))
            self._send(200, db.hiscores(skill, mode, limit, offset))

        elif route == "/api/player":
            name = first("name").strip()
            if not name:
                self._send(400, {"error": "missing name"})
                return
            found = db.player(name)
            if found is None:
                self._send(404, {"error": "player not found", "name": name})
                return
            self._send(200, found)

        elif route == "/api/search":
            term = first("q").strip()
            self._send(200, {"results": db.search(term) if term else []})

        else:
            self._send(404, {"error": "no such endpoint", "path": route})


def main() -> int:
    if not os.path.exists(DB_PATH):
        sys.stderr.write(f"onyx-web: no database at {DB_PATH}\n")
        return 1
    if not db.available():
        sys.stderr.write(f"onyx-web: cannot read {DB_PATH} (permissions? WAL sidecars?)\n")
        return 1
    httpd = ThreadingHTTPServer((BIND, PORT), Handler)
    httpd.daemon_threads = True
    sys.stderr.write(f"onyx-web: serving {DB_PATH} (realm {REALM_ID}) on {BIND}:{PORT}\n")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        httpd.server_close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
