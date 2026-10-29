import logging
import os
import re

import discord
from aiohttp import web

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("disc-bot")

PLUGIN_DIR = os.environ.get("PLUGIN_DIR", "/data/plugins/URLCustomDiscs")
AUDIO_DIR = os.path.join(PLUGIN_DIR, "audio_to_send")
PACK_PATH = os.path.join(PLUGIN_DIR, "URLCustomDiscsPack.zip")
PORT = int(os.environ.get("PORT", "8080"))
MAX_BYTES = 10 * 1024 * 1024
# Disc name becomes minecraft:customdisc.<name>, so it must be a lowercase resource location.
UNSAFE = re.compile(r"[^a-z0-9_-]")


def disc_name(filename: str) -> str:
    stem = filename.rsplit(".", 1)[0].lower().replace(" ", "_")
    return UNSAFE.sub("", stem) or "disc"


def pack_app(token: str) -> web.Application:
    """One read-only route: the disc pack at a secret path (ADR 0005). Everything else 404s."""
    async def pack(_request):
        if not os.path.isfile(PACK_PATH):
            raise web.HTTPNotFound()
        return web.FileResponse(PACK_PATH, headers={"Content-Type": "application/zip", "Cache-Control": "no-cache"})

    app = web.Application()
    app.router.add_get(f"/{token}/discs.zip", pack)
    return app


class DiscBot(discord.Client):
    def __init__(self, channel_id: int, token: str):
        intents = discord.Intents.default()
        intents.message_content = True
        super().__init__(intents=intents)
        self.channel_id = channel_id
        self.pack_token = token

    async def setup_hook(self):
        runner = web.AppRunner(pack_app(self.pack_token))
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
    log.info("Disc pack: %s", f"found ({os.path.getsize(PACK_PATH) // 1024} KB)" if os.path.isfile(PACK_PATH) else "not created yet (the plugin makes it on first start)")
    try:
        os.makedirs(AUDIO_DIR, exist_ok=True)
        probe = os.path.join(AUDIO_DIR, ".write-test")
        open(probe, "w").close()
        os.remove(probe)
        log.info("Drop folder %s: writable", AUDIO_DIR)
    except OSError as exc:
        log.error("Drop folder %s: NOT writable (%s); MP3 uploads will fail", AUDIO_DIR, exc)


if __name__ == "__main__":
    pack_token = os.environ["PACK_TOKEN"]
    if len(pack_token) < 24 or not pack_token.isalnum():
        raise SystemExit("PACK_TOKEN must be at least 24 letters/digits (e.g. `openssl rand -hex 16`)")
    self_check()
    if not os.environ.get("DISCORD_BOT_TOKEN"):
        # No Discord configured: just serve the disc pack.
        log.info("DISCORD_BOT_TOKEN not set: serving the disc pack only on :%s", PORT)
        web.run_app(pack_app(pack_token), port=PORT, print=None)
    else:
        DiscBot(int(os.environ["DISCORD_CHANNEL_ID"]), pack_token).run(os.environ["DISCORD_BOT_TOKEN"])
