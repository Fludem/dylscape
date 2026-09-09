#!/usr/bin/env python3
"""Build a patched RuneLite bundle that talks straight to our server -- no RSProx.

    python3 tools/client/build_client.py --platform macos --arch aarch64 --out build/client

Downloads the rev-233-pinned RuneLite 1.11.19 artifacts, verifies every SHA-256 against the
bootstrap, applies four byte-level patches and writes a runnable directory of jars.

See docs/CLIENT.md for why each patch is needed and how to run the result.
"""
import argparse, hashlib, io, json, os, re, shutil, sys, urllib.request, zipfile

# RuneLite bootstrap pinned to OSRS rev 233 (= RuneLite 1.11.19). Do not float this: the
# server is frozen at 233, so a newer RuneLite is rejected at login with OutOfDateReload.
BOOTSTRAP_COMMIT = "cea91b9921a3647683ba8a5c22ec75c752c91b07"
BOOTSTRAP_URL = (
    f"https://raw.githubusercontent.com/runelite/static.runelite.net/{BOOTSTRAP_COMMIT}/bootstrap.json"
)
REPO_ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


def read_modulus(path):
    """Modulus from $ONYX_MODULUS if set (CI passes it as a secret, since .data/ is
    gitignored), else parsed out of .data/client.key."""
    env = os.environ.get("ONYX_MODULUS", "").strip()
    if env:
        mod = env
    else:
        if not os.path.exists(path):
            sys.exit(f"no modulus: {path} not found and ONYX_MODULUS unset")
        text = open(path, encoding="utf-8").read()
        mod = re.search(r"Modulus:\s*([0-9a-f]+)", text).group(1)
    if len(mod) != 256:
        sys.exit(f"modulus is {len(mod)} chars, expected 256 -- 1024-bit key required")
    return mod.encode()


def fetch(url, cache_dir):
    os.makedirs(cache_dir, exist_ok=True)
    dest = os.path.join(cache_dir, os.path.basename(url))
    if not os.path.exists(dest):
        with urllib.request.urlopen(url, timeout=120) as r, open(dest, "wb") as f:
            shutil.copyfileobj(r, f)
    return dest


def wanted(artifact, platform, arch):
    plat = artifact.get("platform")
    if not plat:
        return True
    for entry in plat:
        if entry.get("name") != platform:
            continue
        if entry.get("arch") in (None, arch):
            return True
    return False


def blank_utf8(data, text):
    """Rewrite a CONSTANT_Utf8 entry to the empty string. Class files carry no absolute
    offsets, so shrinking the entry in place is safe."""
    raw = text.encode()
    needle = b"\x01" + len(raw).to_bytes(2, "big") + raw
    return data.replace(needle, b"\x01\x00\x00"), data.count(needle)


def rewrite_jar(src, dst, transform, drop=()):
    with zipfile.ZipFile(src) as zin, zipfile.ZipFile(dst, "w", zipfile.ZIP_DEFLATED) as zout:
        for item in zin.infolist():
            if item.filename in drop:
                continue
            zout.writestr(item, transform(item.filename, zin.read(item.filename)))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--platform", default="macos", choices=["macos", "win", "linux"])
    ap.add_argument("--arch", default="aarch64")
    ap.add_argument("--out", default="build/client")
    ap.add_argument("--port", type=int, default=43594, help="only patched if not 43594")
    ap.add_argument("--client-key", default=os.path.join(REPO_ROOT, ".data", "client.key"))
    ap.add_argument("--cache-dir", default=os.path.join(REPO_ROOT, "build", "runelite-artifacts"))
    args = ap.parse_args()

    modulus = read_modulus(args.client_key)
    with urllib.request.urlopen(BOOTSTRAP_URL, timeout=60) as r:
        bootstrap = json.load(r)
    artifacts = [a for a in bootstrap["artifacts"] if wanted(a, args.platform, args.arch)]
    print(f"RuneLite {bootstrap['version']}: {len(artifacts)} artifacts for {args.platform}/{args.arch}")

    os.makedirs(args.out, exist_ok=True)
    hits = {"modulus": 0, "localhost": 0, "jagex": 0, "port": 0}

    def patch_injected(name, data):
        if not name.endswith(".class"):
            return data
        m = re.search(rb"(?<![0-9a-f])[0-9a-f]{256}(?![0-9a-f])", data)
        if m:
            data = data[: m.start()] + modulus + data[m.end():]
            hits["modulus"] += 1
        data, n = blank_utf8(data, "127.0.0.1")
        hits["localhost"] += n
        if args.port != 43594:
            old = b"\x03" + (43594).to_bytes(4, "big")
            hits["port"] += data.count(old)
            data = data.replace(old, b"\x03" + args.port.to_bytes(4, "big"))
        return data

    def patch_runelite(name, data):
        if name == "META-INF/MANIFEST.MF":
            return b"Manifest-Version: 1.0\r\n\r\n"
        if name.endswith(".class"):
            data, n = blank_utf8(data, ".jagex.com")
            hits["jagex"] += n
        return data

    for a in artifacts:
        src = fetch(a["path"], args.cache_dir)
        digest = hashlib.sha256(open(src, "rb").read()).hexdigest()
        if digest != a["hash"]:
            sys.exit(f"{a['name']}: SHA-256 mismatch -- not the pinned build")
        dst = os.path.join(args.out, a["name"])
        if a["name"].startswith("injected-client-"):
            rewrite_jar(src, dst, patch_injected)
        elif a["name"].startswith("client-"):
            # the jar is signed; editing any entry without dropping these throws
            # SecurityException: signer information does not match
            rewrite_jar(src, dst, patch_runelite, drop=("META-INF/RL.SF", "META-INF/RL.RSA"))
        else:
            shutil.copy2(src, dst)

    for k, v in hits.items():
        print(f"  {k:10} {v}")
    # Each constant occurs exactly once in the pinned build. If that ever stops being true,
    # RuneLite has moved and the patch must be re-derived -- fail rather than ship a dud.
    for key in ("modulus", "localhost", "jagex"):
        if hits[key] != 1:
            sys.exit(f"expected exactly one {key} site, found {hits[key]} -- re-derive the patch")
    print("wrote", args.out)


if __name__ == "__main__":
    main()
