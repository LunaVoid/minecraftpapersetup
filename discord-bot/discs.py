"""Disc pipeline (ADR 0006): source -> yt-dlp -> FFmpeg -> tracks/<name>.ogg -> discs.json -> disc pack.

Everything here runs in the disc bot's container. The Minecraft server only sees the files it writes
into the plugin folder (shared bind mount): tracks/, discs.json, URLCustomDiscsPack.zip(.sha1).
"""
import asyncio
import hashlib
import json
import logging
import os
import re
import shutil
import sys
import time
import uuid
import zipfile

log = logging.getLogger("disc-bot")

PLUGIN_DIR = os.environ.get("PLUGIN_DIR", "/data/plugins/URLCustomDiscs")
AUDIO_DIR = os.path.join(PLUGIN_DIR, "audio_to_send")
TRACKS_DIR = os.path.join(PLUGIN_DIR, "tracks")
DISCS_JSON = os.path.join(PLUGIN_DIR, "discs.json")
PACK_PATH = os.path.join(PLUGIN_DIR, "URLCustomDiscsPack.zip")
WORK_DIR = os.environ.get("WORK_DIR", "/tmp/discs")
BASE_PACK = os.path.join(os.path.dirname(os.path.abspath(__file__)), "pack-base.zip")
MAX_SECONDS = int(os.environ.get("MAX_SECONDS", "600"))

NAME = re.compile(r"^[a-z0-9_-]{1,64}$")
MP3_FILE = re.compile(r"^[A-Za-z0-9_.-]+\.mp3$")  # a bare file name in the drop folder, never a path
NICE = ["nice", "-n", "10"]  # the bot shares the host with the game: stay out of its way

_job = asyncio.Lock()  # one download/convert at a time


class DiscError(Exception):
    pass


# ---------- discs.json + pack ----------

def load_discs() -> dict:
    try:
        with open(DISCS_JSON) as f:
            return json.load(f)
    except FileNotFoundError:
        return {}


def _write_atomic(path: str, data: bytes) -> None:
    tmp = path + ".tmp"
    with open(tmp, "wb") as f:
        f.write(data)
    os.replace(tmp, path)


def save_discs(discs: dict) -> None:
    _write_atomic(DISCS_JSON, json.dumps(discs, indent=4).encode())


def new_model_data(discs: dict) -> tuple:
    # Same scheme as upstream (23 bits, multiple of 16), so discs made before the move keep working.
    used = {d["customModelData"] for d in discs.values()}
    while True:
        u = uuid.uuid4()
        cmd = (int(u.hex[:8], 16) & 0x7FFFFF) // 16 * 16
        if cmd and cmd not in used:
            return cmd, str(u)


def build_pack(discs: dict) -> None:
    """Rebuild the whole pack from tracks/ (cheap: oggs are stored, not recompressed)."""
    names = sorted(n for n in discs if os.path.isfile(os.path.join(TRACKS_DIR, f"{n}.ogg")))
    sounds = {f"customdisc.{n}": {"sounds": [{"stream": True, "name": f"custom/{n}"}], "category": "record"} for n in names}
    items = {"model": {
        "type": "range_dispatch",
        "property": "custom_model_data",
        "entries": [{"threshold": discs[n]["customModelData"],
                     "model": {"type": "model", "model": f"item/custom_music_disc_{n}"}}
                    for n in sorted(names, key=lambda n: discs[n]["customModelData"])],
        "fallback": {"type": "model", "model": "minecraft:item/music_disc_13"},
    }}
    model = json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": "item/record_custom"}})

    tmp = PACK_PATH + ".tmp"
    with zipfile.ZipFile(BASE_PACK) as base, zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as z:
        for item in base.infolist():  # pack.mcmeta, pack.png, disc texture
            if not item.is_dir() and item.filename not in ("assets/minecraft/sounds.json", "assets/minecraft/items/music_disc_13.json"):
                z.writestr(item.filename, base.read(item))
        z.writestr("assets/minecraft/sounds.json", json.dumps(sounds, indent=2))
        z.writestr("assets/minecraft/items/music_disc_13.json", json.dumps(items, indent=2))
        for n in names:
            z.writestr(f"assets/minecraft/models/item/custom_music_disc_{n}.json", model)
            z.write(os.path.join(TRACKS_DIR, f"{n}.ogg"), f"assets/minecraft/sounds/custom/{n}.ogg", zipfile.ZIP_STORED)
    with open(tmp, "rb") as f:
        sha1 = hashlib.sha1(f.read()).hexdigest()
    os.replace(tmp, PACK_PATH)
    _write_atomic(PACK_PATH + ".sha1", sha1.encode())  # the plugin reads this instead of hashing on the game thread
    log.info("Disc pack rebuilt: %d discs, %d KB, sha1 %s", len(names), os.path.getsize(PACK_PATH) // 1024, sha1)


def import_old_tracks() -> None:
    """One-time migration: discs made by the old plugin only exist inside the pack zip."""
    discs = load_discs()
    missing = [n for n in discs if not os.path.isfile(os.path.join(TRACKS_DIR, f"{n}.ogg"))]
    if not missing or not os.path.isfile(PACK_PATH):
        return
    os.makedirs(TRACKS_DIR, exist_ok=True)
    with zipfile.ZipFile(PACK_PATH) as z:
        for n in missing:
            try:
                _write_atomic(os.path.join(TRACKS_DIR, f"{n}.ogg"), z.read(f"assets/minecraft/sounds/custom/{n}.ogg"))
                log.info("Imported track %s from the old pack", n)
            except KeyError:
                log.warning("Disc %s has no audio in the old pack; it will be dropped from the new pack", n)
    build_pack(discs)


# ---------- tools ----------

async def _run(cmd: list, on_line=None) -> tuple:
    proc = await asyncio.create_subprocess_exec(*cmd, stdout=asyncio.subprocess.PIPE, stderr=asyncio.subprocess.STDOUT)
    lines = []
    async for raw in proc.stdout:
        line = raw.decode(errors="replace").rstrip()
        lines.append(line)
        if on_line:
            await on_line(line)
    return await proc.wait(), lines


async def update_ytdlp() -> str:
    """yt-dlp breaks whenever YouTube changes; keep it current in the bot's own user site-packages."""
    code, lines = await _run([sys.executable, "-m", "pip", "install", "-q", "-U", "--user", "--no-warn-script-location",
                              "yt-dlp[default]"])
    _, version = await _run([sys.executable, "-m", "yt_dlp", "--version"])
    v = version[-1] if version else "?"
    log.info("yt-dlp update %s, now %s", "ok" if code == 0 else "FAILED:\n" + "\n".join(lines[-5:]), v)
    return v


async def _download(url: str, dest_stem: str, emit) -> str:
    last_error, last_sent = "", 0.0

    async def on_line(line):
        nonlocal last_error, last_sent
        log.info("[yt-dlp] %s", line)
        if line.startswith("ERROR:"):
            last_error = line[6:].strip()
        elif line.startswith("[download]") and "%" in line and time.monotonic() - last_sent > 1:
            last_sent = time.monotonic()
            await emit({"stage": "download", "progress": line[len("[download]"):].strip()})

    cmd = NICE + [sys.executable, "-m", "yt_dlp", "--no-playlist", "--newline", "--no-mtime",
                  "-f", "bestaudio/best", "--max-filesize", "300M", "-o", dest_stem + ".%(ext)s", url]
    code, _ = await _run(cmd, on_line)
    if code != 0:
        await emit({"stage": "update", "message": f"Download failed ({last_error or 'unknown error'}). Updating yt-dlp and retrying once..."})
        await update_ytdlp()
        last_error = ""
        code, _ = await _run(cmd, on_line)
        if code != 0:
            raise DiscError(f"Download failed: {last_error or 'unknown error'}")
    out = [f for f in os.listdir(os.path.dirname(dest_stem)) if f.startswith(os.path.basename(dest_stem) + ".")]
    if not out:
        raise DiscError("Download finished but produced no file")
    return os.path.join(os.path.dirname(dest_stem), out[0])


async def _convert(src: str, dest: str, mono: bool, emit) -> None:
    duration = None

    async def on_line(line):
        nonlocal duration
        m = re.search(r"Duration: (\d+):(\d+):(\d+)", line)
        if m and duration is None:
            duration = int(m[1]) * 3600 + int(m[2]) * 60 + int(m[3])
            if duration > MAX_SECONDS:
                await emit({"stage": "convert", "message": f"Track is {duration // 60}:{duration % 60:02d} long, trimming to {MAX_SECONDS // 60}:{MAX_SECONDS % 60:02d}."})

    code, lines = await _run(NICE + ["ffmpeg", "-hide_banner", "-nostdin", "-y", "-i", src, "-vn", "-map_metadata", "-1",
                                     "-t", str(MAX_SECONDS), "-ac", "1" if mono else "2",
                                     "-c:a", "libvorbis", "-q:a", "3", "-f", "ogg", dest], on_line)
    if code != 0 or not os.path.isfile(dest):
        raise DiscError("Couldn't convert the audio: " + (lines[-1] if lines else "ffmpeg failed"))


# ---------- jobs ----------

async def create(source: str, name: str, mono: bool, emit) -> dict:
    if not NAME.match(name):
        raise DiscError("Disc names can only use a-z, 0-9, _ and - (max 64).")
    is_url = source.lower().startswith(("http://", "https://"))
    if not is_url:
        if not MP3_FILE.match(source):
            raise DiscError("Give a link, or a plain .mp3 file name from the drop folder.")
        if not os.path.isfile(os.path.join(AUDIO_DIR, source)):
            raise DiscError(f"No file '{source}' in the drop folder (each MP3 is used up once its disc is made).")

    if _job.locked():
        await emit({"stage": "queued", "message": "Another disc is being made; you're next."})
    async with _job:
        work = os.path.join(WORK_DIR, uuid.uuid4().hex)
        os.makedirs(work)
        try:
            if is_url:
                await emit({"stage": "download", "message": "Downloading audio..."})
                src = await _download(source, os.path.join(work, "src"), emit)
            else:
                src = os.path.join(AUDIO_DIR, source)
            await emit({"stage": "convert", "message": "Converting audio..."})
            ogg = os.path.join(work, "out.ogg")
            await _convert(src, ogg, mono, emit)

            await emit({"stage": "pack", "message": "Adding it to the disc pack..."})
            os.makedirs(TRACKS_DIR, exist_ok=True)
            shutil.move(ogg, os.path.join(TRACKS_DIR, f"{name}.ogg"))
            discs = load_discs()
            if name not in discs:
                cmd, uid = new_model_data(discs)
                discs[name] = {"customModelData": cmd, "displayName": name, "uuid": uid}
            discs[name]["source"] = source
            save_discs(discs)
            await asyncio.to_thread(build_pack, discs)
            if not is_url:
                os.remove(src)  # the MP3 is used up, as before
            return discs[name]
        finally:
            shutil.rmtree(work, ignore_errors=True)


async def delete(name: str) -> None:
    async with _job:
        discs = load_discs()
        if name not in discs:
            raise DiscError(f"No disc named '{name}'.")
        del discs[name]
        save_discs(discs)
        try:
            os.remove(os.path.join(TRACKS_DIR, f"{name}.ogg"))
        except FileNotFoundError:
            pass
        await asyncio.to_thread(build_pack, discs)
