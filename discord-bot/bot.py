import asyncio
import hmac
import json
import logging
import os
import re
import secrets

import discord
from aiohttp import web

import discs
from discs import AUDIO_DIR, PLUGIN_DIR

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("disc-bot")

PORT = int(os.environ.get("PORT", "8080"))
MAX_BYTES = 10 * 1024 * 1024
# Disc name becomes minecraft:customdisc.<name>, so it must be a lowercase resource location.
UNSAFE = re.compile(r"[^a-z0-9_-]")


def disc_name(filename: str) -> str:
    stem = filename.rsplit(".", 1)[0].lower().replace(" ", "_")
    return UNSAFE.sub("", stem) or "disc"


def pack_app(token: str, api_secret: str = "") -> web.Application:
    """Public: the disc pack at a secret path (ADR 0005). Private: the disc API, only if a secret is set (ADR 0006)."""
    async def pack(_request):
        if not os.path.isfile(discs.PACK_PATH):
            raise web.HTTPNotFound()
        return web.FileResponse(discs.PACK_PATH, headers={"Content-Type": "application/zip", "Cache-Control": "no-cache"})

    def authorized(request) -> bool:
        return hmac.compare_digest(request.headers.get("Authorization", ""), f"Bearer {api_secret}")

    async def create(request):
        if not authorized(request):
            raise web.HTTPUnauthorized()
        body = await request.json()
        resp = web.StreamResponse(headers={"Content-Type": "application/x-ndjson"})
        await resp.prepare(request)

        async def emit(event):  # one JSON object per line; the plugin relays them to the player
            await resp.write((json.dumps(event) + "\n").encode())

        try:
            disc = await discs.create(str(body.get("source", "")), str(body.get("name", "")), bool(body.get("mono")), emit)
            await emit({"stage": "done", "disc": disc})
        except discs.DiscError as exc:
            await emit({"stage": "error", "message": str(exc)})
        except Exception as exc:
            log.exception("create failed")
            await emit({"stage": "error", "message": f"Unexpected error: {exc}"})
        await resp.write_eof()
        return resp

    async def delete(request):
        if not authorized(request):
            raise web.HTTPUnauthorized()
        try:
            await discs.delete(str((await request.json()).get("name", "")))
        except discs.DiscError as exc:
            return web.json_response({"ok": False, "message": str(exc)})
        return web.json_response({"ok": True})

    async def update(request):
        if not authorized(request):
            raise web.HTTPUnauthorized()
        return web.json_response({"ok": True, "version": await discs.update_ytdlp()})

    app = web.Application()
    app.router.add_get(f"/{token}/discs.zip", pack)
    if api_secret:
        app.router.add_post("/api/create", create)
        app.router.add_post("/api/delete", delete)
        app.router.add_post("/api/update", update)
    return app


async def on_startup(app):
    """Bring old discs over and get the latest yt-dlp, without blocking the web server."""
    await asyncio.to_thread(discs.import_old_tracks)
    asyncio.get_running_loop().create_task(discs.update_ytdlp())


class DiscBot(discord.Client):
    def __init__(self, channel_id: int, token: str, api_secret: str):
        intents = discord.Intents.default()
        intents.message_content = True
        super().__init__(intents=intents)
        self.channel_id = channel_id
        self.pack_token = token
        self.api_secret = api_secret

    async def setup_hook(self):
        app = pack_app(self.pack_token, self.api_secret)
        app.on_startup.append(on_startup)
        runner = web.AppRunner(app)
        await runner.setup()
        await web.TCPSite(runner, "0.0.0.0", PORT).start()
        log.info("Serving disc pack on :%s", PORT)

    async def on_ready(self):
        log.info("Logged in as %s, watching channel %s", self.user, self.channel_id)

    async def on_message(self, message: discord.Message):
        if message.author.bot or message.channel.id != self.channel_id:
            return
        for attachment in message.attachments:
            if not attachment.filename.lower().endswith(".mp3"):
                continue
            name = disc_name(attachment.filename)
            dest = os.path.join(AUDIO_DIR, f"{name}.mp3")
            if os.path.exists(dest):
                await message.reply(f"⚠️ `{name}.mp3` is already waiting in the drop folder. Make its disc first, or rename and re-upload.")
                continue
            if attachment.size > MAX_BYTES:
                await message.reply(f"⚠️ `{attachment.filename}` is over 10MB, skipping.")
                continue
            part = dest + ".part"
            try:
                await attachment.save(part)
                os.replace(part, dest)  # the plugin never sees a half-written file
            except Exception as exc:
                log.exception("Failed to save %s", attachment.filename)
                await message.reply(f"❌ Failed to save `{attachment.filename}`: {exc}")
                if os.path.exists(part):
                    os.remove(part)
                continue
            log.info("Saved %s as %s", attachment.filename, dest)
            await message.reply(f"✅ Saved. In-game, run:\n`/disc create {name}.mp3`\n(long tracks are trimmed by the server)")


def self_check() -> None:
    """Log what this container can reach, so a bad mount shows up in the deploy log."""
    log.info("Plugin folder %s: %s", PLUGIN_DIR, "found" if os.path.isdir(PLUGIN_DIR) else "MISSING (check the /data bind mount)")
    log.info("Disc pack: %s", f"found ({os.path.getsize(discs.PACK_PATH) // 1024} KB)" if os.path.isfile(discs.PACK_PATH) else "not created yet (made with the first disc)")
    try:
        os.makedirs(AUDIO_DIR, exist_ok=True)
        probe = os.path.join(AUDIO_DIR, ".write-test")
        open(probe, "w").close()
        os.remove(probe)
        log.info("Drop folder %s: writable", AUDIO_DIR)
    except OSError as exc:
        log.error("Drop folder %s: NOT writable (%s); MP3 uploads will fail", AUDIO_DIR, exc)


SECRETS_FILE = os.path.join(PLUGIN_DIR, ".disc-secrets.json")


def load_secrets() -> tuple:
    """PACK_TOKEN / DISC_API_SECRET from env if set, else generated once and kept in the shared plugin folder.
    The plugin reads the same file, so nothing has to be copied between the two (ADR 0006)."""
    try:
        with open(SECRETS_FILE) as f:
            saved = json.load(f)
    except (FileNotFoundError, ValueError):
        saved = {}
    pack = os.environ.get("PACK_TOKEN") or saved.get("pack_token") or secrets.token_hex(16)
    api = os.environ.get("DISC_API_SECRET") or saved.get("api_secret") or secrets.token_hex(16)
    for name, value in (("PACK_TOKEN", pack), ("DISC_API_SECRET", api)):
        if len(value) < 24 or not value.isalnum():
            raise SystemExit(f"{name} must be at least 24 letters/digits")
    if pack == api:
        raise SystemExit("PACK_TOKEN and DISC_API_SECRET must differ")
    if saved != {"pack_token": pack, "api_secret": api}:
        tmp = SECRETS_FILE + ".tmp"
        with open(os.open(tmp, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600), "w") as f:  # owner (uid 1000, shared with the server) only
            json.dump({"pack_token": pack, "api_secret": api}, f)
        os.replace(tmp, SECRETS_FILE)
        log.info("Saved secrets to %s (the plugin reads them from there)", SECRETS_FILE)
    return pack, api


if __name__ == "__main__":
    self_check()
    pack_token, api_secret = load_secrets()
    log.info("Disc API enabled")
    if not os.environ.get("DISCORD_BOT_TOKEN"):
        # No Discord configured: pack + disc API only.
        log.info("DISCORD_BOT_TOKEN not set: no Discord, web only on :%s", PORT)
        app = pack_app(pack_token, api_secret)
        app.on_startup.append(on_startup)
        web.run_app(app, port=PORT, print=None)
    else:
        DiscBot(int(os.environ["DISCORD_CHANNEL_ID"]), pack_token, api_secret).run(os.environ["DISCORD_BOT_TOKEN"])
