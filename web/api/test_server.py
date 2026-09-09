"""Tests for the HTTP layer.

These boot the real handler against a temporary database and speak to it over a socket, because
the bug they exist for - a HEAD leaving state on a reused keep-alive connection, so the next GET
returned headers with a Content-Length and no body - is invisible to any test that calls the
routing functions directly.
"""

from __future__ import annotations

import http.client
import json
import os
import sqlite3
import sys
import tempfile
import threading
import unittest
from http.server import ThreadingHTTPServer

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from test_onyxdb import Fixture  # noqa: E402


class ServerTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.dir = tempfile.TemporaryDirectory()
        path = os.path.join(cls.dir.name, "game.db")
        fixture = Fixture(path)
        fixture.add("zezima", {"attack": (50, 101_333)}, display="Zezima")
        fixture.conn.close()

        os.environ["ONYX_DB"] = path
        os.environ["ONYX_REALM"] = "2"
        for module in ("hiscores", "onyxdb"):
            sys.modules.pop(module, None)
        import hiscores

        cls.hiscores = hiscores
        cls.server = ThreadingHTTPServer(("127.0.0.1", 0), hiscores.Handler)
        cls.server.daemon_threads = True
        cls.port = cls.server.server_address[1]
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()
        cls.dir.cleanup()

    def conn(self):
        return http.client.HTTPConnection("127.0.0.1", self.port, timeout=5)

    def test_get_returns_json(self):
        c = self.conn()
        c.request("GET", "/api/status")
        res = c.getresponse()
        body = json.loads(res.read())
        self.assertEqual(res.status, 200)
        self.assertEqual(body["characters"], 1)
        c.close()

    def test_head_returns_headers_without_a_body(self):
        c = self.conn()
        c.request("HEAD", "/api/status")
        res = c.getresponse()
        self.assertEqual(res.status, 200)
        self.assertEqual(res.read(), b"")
        self.assertTrue(res.getheader("Content-Length"))
        c.close()

    def test_a_get_after_a_head_on_the_same_connection_still_has_a_body(self):
        """The regression. Caddy pools upstream connections, so this sequence is routine."""
        c = self.conn()
        c.request("HEAD", "/api/status")
        c.getresponse().read()
        c.request("GET", "/api/status")
        res = c.getresponse()
        body = res.read()
        self.assertEqual(res.status, 200)
        self.assertTrue(body, "GET after HEAD returned an empty body — head_only leaked")
        self.assertEqual(json.loads(body)["characters"], 1)
        c.close()

    def test_many_requests_on_one_connection(self):
        c = self.conn()
        for i in range(6):
            method = "HEAD" if i % 2 else "GET"
            c.request(method, "/api/hiscores?skill=overall")
            res = c.getresponse()
            payload = res.read()
            self.assertEqual(res.status, 200)
            if method == "GET":
                self.assertTrue(payload, f"empty body on request {i}")
        c.close()

    def test_player_endpoint(self):
        c = self.conn()
        c.request("GET", "/api/player?name=Zezima")
        res = c.getresponse()
        self.assertEqual(res.status, 200)
        self.assertEqual(json.loads(res.read())["name"], "Zezima")
        c.close()

    def test_unknown_player_is_404_not_500(self):
        c = self.conn()
        c.request("GET", "/api/player?name=nobody")
        self.assertEqual(c.getresponse().status, 404)
        c.close()

    def test_bad_input_is_rejected(self):
        for path, want in (
            ("/api/hiscores?skill=nonsense", 400),
            ("/api/hiscores?mode=nonsense", 400),
            ("/api/player", 400),
            ("/api/nope", 404),
        ):
            c = self.conn()
            c.request("GET", path)
            self.assertEqual(c.getresponse().status, want, path)
            c.close()

    def test_limit_is_clamped(self):
        c = self.conn()
        c.request("GET", "/api/hiscores?skill=overall&limit=99999")
        res = c.getresponse()
        self.assertEqual(res.status, 200)
        c.close()


if __name__ == "__main__":
    unittest.main()
