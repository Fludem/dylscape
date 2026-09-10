#!/usr/bin/env python3
"""Build plain zipped clients to hand out: a folder with Onyx.jar, a launch script and a JRE.

    python3 tools/client/make_zips.py                  # both targets
    python3 tools/client/make_zips.py --only windows

Unlike package.sh (jpackage), this cross-builds from one machine: nothing is compiled per
platform, the JRE is Temurin's own prebuilt one. Output lands in build/client-zip/.
See docs/CLIENT.md.
"""
import argparse, hashlib, io, os, shutil, stat, subprocess, sys, tarfile, time, urllib.request, zipfile

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
APP = "Onyx"

# Temurin 21 JREs, pinned. Checksums from api.adoptium.net.
JRE_BASE = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/"
TARGETS = {
    "windows": {
        "platform": "win", "arch": "amd64",
        "jre": "OpenJDK21U-jre_x64_windows_hotspot_21.0.12.1_1.zip",
        "sha256": "d35f31e712f0fcf6ac5a093edc90204fbff22f720ba3950bd09d331d5e621636",
    },
    "macos": {
        "platform": "macos", "arch": "aarch64",
        "jre": "OpenJDK21U-jre_aarch64_mac_hotspot_21.0.12.1_1.tar.gz",
        "sha256": "dec50fc6f9fcd4fe3ae8cabf5a5fa68f6afc48841f7698e468e9aa5d54beed84",
    },
}

# RuneLite's clientJvm17Arguments; the add-opens is mandatory on macOS or startup dies in
# OSXUtil.tryEnableFullscreen with IllegalAccessError.
JVM_ARGS = "-XX:+DisableAttachMechanism -Xmx768m -Xss2m -XX:CompileThreshold=1500"
MAC_JVM_ARGS = (JVM_ARGS + " --add-opens=java.desktop/com.apple.eawt=ALL-UNNAMED"
                f" -Dapple.awt.application.name={APP} -Xdock:name={APP}")

WINDOWS_BAT = f"""@echo off
rem Starts {APP}. Keep this file next to {APP}.jar, lib and jre.
cd /d "%~dp0"
start "" "jre\\bin\\javaw.exe" {JVM_ARGS} -jar {APP}.jar
"""

MAC_COMMAND = f"""#!/bin/bash
# Starts {APP}. Keep this file next to {APP}.jar, lib and jre.
cd "$(dirname "$0")"
nohup ./jre/bin/java {MAC_JVM_ARGS} -jar {APP}.jar >/dev/null 2>&1 &
disown
"""

README = f"""{APP}
====

Unzip this folder anywhere, then:

  Windows: double-click {APP}.bat
  Mac:     double-click {APP}.command

Java is included; you do not need to install anything.

The first login downloads about 190 MB of game data, so give it a minute.
Your settings live in their own folder and never touch a normal RuneLite install.

Mac: the first time, macOS says it "cannot be opened" / "cannot verify the developer".
  Click Done, open System Settings > Privacy & Security, scroll down and click
  "Open Anyway" next to {APP}.command. You only do this once.
  (Or, in Terminal:  xattr -dr com.apple.quarantine <drag the {APP} folder here> )

Windows: if a blue "Windows protected your PC" box appears, click More info > Run anyway.

Please:
  - Don't use the world switcher. It lists real OSRS worlds and hopping breaks your session.
  - If you get disconnected, log in again from the start.

If it won't start, send over the file launcher-error.log from:
  Windows: %LOCALAPPDATA%\\{APP}
  Mac:     ~/Library/Application Support/{APP}
"""


def fetch(url, sha256, cache_dir):
    os.makedirs(cache_dir, exist_ok=True)
    path = os.path.join(cache_dir, url.rsplit("/", 1)[1])
    if not os.path.exists(path):
        print(f"  downloading {os.path.basename(path)}")
        with urllib.request.urlopen(url) as r, open(path + ".part", "wb") as f:
            shutil.copyfileobj(r, f)
        os.replace(path + ".part", path)
    with open(path, "rb") as f:
        got = hashlib.sha256(f.read()).hexdigest()
    if got != sha256:
        os.remove(path)
        sys.exit(f"checksum mismatch for {path}: {got}")
    return path


def extract_jre(archive, dest):
    """Unpack the JRE so that dest/bin/java(.exe) exists; drops Temurin's top-level dir
    (and the macOS Contents/Home bundle wrapper)."""
    tmp = dest + ".tmp"
    shutil.rmtree(tmp, ignore_errors=True)
    if archive.endswith(".zip"):
        with zipfile.ZipFile(archive) as z:
            z.extractall(tmp)
    else:
        with tarfile.open(archive) as t:
            t.extractall(tmp, filter="tar")
    for root, _, files in os.walk(tmp):
        if os.path.basename(root) == "bin" and ("java" in files or "java.exe" in files):
            shutil.move(os.path.dirname(root), dest)
            break
    else:
        sys.exit(f"no bin/java inside {archive}")
    shutil.rmtree(tmp)
    with open(os.path.join(dest, "release")) as f:
        modules = f.read()
    if "jdk.httpserver" not in modules:
        sys.exit("JRE lacks jdk.httpserver; RuneLite dies without it")


def manifest(lib_jars):
    """Manifest lines are capped at 72 bytes; longer values continue on lines starting ' '."""
    attrs = [
        ("Manifest-Version", "1.0"),
        ("Main-Class", "net.onyx.launcher.Main"),
        ("Class-Path", " ".join(f"lib/{j}" for j in lib_jars)),
        # Honoured for `java -jar`, so double-clicking the jar with a system Java works too.
        ("Add-Opens", "java.desktop/com.apple.eawt"),
    ]
    out = []
    for key, value in attrs:
        line = f"{key}: {value}".encode()
        out.append(line[:72])
        line = line[72:]
        while line:
            out.append(b" " + line[:71])
            line = line[71:]
    return b"\r\n".join(out) + b"\r\n\r\n"


def build_launcher(classes_dir, client_jar, javac):
    shutil.rmtree(classes_dir, ignore_errors=True)
    src = os.path.join(REPO_ROOT, "tools/client/launcher/src/net/onyx/launcher/Main.java")
    subprocess.run([javac, "--release", "11", "-d", classes_dir, "-cp", client_jar, src],
                   check=True)


def write_launcher_jar(path, classes_dir, lib_jars):
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("META-INF/MANIFEST.MF", manifest(lib_jars))
        for root, _, files in os.walk(classes_dir):
            for name in files:
                full = os.path.join(root, name)
                z.write(full, os.path.relpath(full, classes_dir))


def zip_dir(src, dest_zip):
    """Zip src as a top-level folder, keeping exec bits and symlinks (Archive Utility and
    unzip both honour them)."""
    base = os.path.dirname(src)
    with zipfile.ZipFile(dest_zip, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for root, dirs, files in os.walk(src):
            dirs.sort()
            for name in sorted(files) + [d for d in dirs if os.path.islink(os.path.join(root, d))]:
                full = os.path.join(root, name)
                arc = os.path.relpath(full, base)
                st = os.lstat(full)
                info = zipfile.ZipInfo(arc, time.localtime(st.st_mtime)[:6])
                info.create_system = 3  # unix, so external_attr carries the mode
                info.external_attr = (st.st_mode & 0xFFFF) << 16
                if stat.S_ISLNK(st.st_mode):
                    z.writestr(info, os.readlink(full))
                else:
                    info.compress_type = zipfile.ZIP_DEFLATED
                    with open(full, "rb") as f:
                        z.writestr(info, f.read())


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", choices=sorted(TARGETS))
    ap.add_argument("--out", default=os.path.join(REPO_ROOT, "build", "client-zip"))
    ap.add_argument("--javac", default=shutil.which("javac") or "javac")
    args = ap.parse_args()
    cache = os.path.join(REPO_ROOT, "build", "jre-downloads")

    for name, t in TARGETS.items():
        if args.only and name != args.only:
            continue
        print(f"==> {name}")
        stage = os.path.join(args.out, name)
        folder = os.path.join(stage, APP)
        lib = os.path.join(folder, "lib")
        shutil.rmtree(stage, ignore_errors=True)
        subprocess.run([sys.executable, os.path.join(REPO_ROOT, "tools/client/build_client.py"),
                        "--platform", t["platform"], "--arch", t["arch"], "--out", lib],
                       check=True)
        lib_jars = sorted(os.listdir(lib))

        classes = os.path.join(stage, "classes")
        client_jar = next(os.path.join(lib, j) for j in lib_jars if j.startswith("client-"))
        build_launcher(classes, client_jar, args.javac)
        write_launcher_jar(os.path.join(folder, f"{APP}.jar"), classes, lib_jars)
        shutil.rmtree(classes)

        extract_jre(fetch(JRE_BASE + t["jre"], t["sha256"], cache), os.path.join(folder, "jre"))

        if name == "windows":
            with open(os.path.join(folder, f"{APP}.bat"), "w", newline="\r\n") as f:
                f.write(WINDOWS_BAT)
        else:
            script = os.path.join(folder, f"{APP}.command")
            with open(script, "w") as f:
                f.write(MAC_COMMAND)
            os.chmod(script, 0o755)
        with open(os.path.join(folder, "README.txt"), "w",
                  newline="\r\n" if name == "windows" else "\n") as f:
            f.write(README)

        dest = os.path.join(args.out, f"{APP}-{'Windows' if name == 'windows' else 'macOS'}.zip")
        zip_dir(folder, dest)
        print(f"  {dest} ({os.path.getsize(dest) / 1e6:.0f} MB)")


if __name__ == "__main__":
    main()
