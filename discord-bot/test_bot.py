"""Runs during `docker build`. Exercises the real pipeline with the image's ffmpeg (no network)."""
import asyncio
import json
import os
import subprocess
import tempfile
import zipfile

import bot
import discs
from aiohttp.test_utils import TestClient, TestServer

assert bot.disc_name("Purple Rain.mp3") == "purple_rain"
assert bot.disc_name("../../etc/passwd.mp3") == "etcpasswd"
assert bot.disc_name("!!!.mp3") == "disc"

TOKEN, SECRET = "t" * 24, "s" * 24
AUTH = {"Authorization": f"Bearer {SECRET}"}


def use_dir(d):
    discs.PLUGIN_DIR = d
    discs.AUDIO_DIR = os.path.join(d, "audio_to_send")
    discs.TRACKS_DIR = os.path.join(d, "tracks")
    discs.DISCS_JSON = os.path.join(d, "discs.json")
    discs.PACK_PATH = os.path.join(d, "URLCustomDiscsPack.zip")
    discs.WORK_DIR = os.path.join(d, "work")
    os.makedirs(discs.AUDIO_DIR)


async def events(resp):
    return [json.loads(line) for line in (await resp.text()).splitlines()]


async def main():
    with tempfile.TemporaryDirectory() as d:
        use_dir(d)
        discs.MAX_SECONDS = 3
        subprocess.run(["ffmpeg", "-v", "error", "-f", "lavfi", "-i", "sine=duration=5", os.path.join(discs.AUDIO_DIR, "tone.mp3")], check=True)

        async with TestClient(TestServer(bot.pack_app(TOKEN, SECRET))) as c:
            assert (await c.get(f"/{TOKEN}/discs.zip")).status == 404  # no pack yet
            assert (await c.post("/api/create", json={"source": "tone.mp3", "name": "tone"})).status == 401  # no secret
            assert (await c.post("/api/create", json={}, headers={"Authorization": "Bearer wrong"})).status == 401

            ev = await events(await c.post("/api/create", json={"source": "../tone.mp3", "name": "x"}, headers=AUTH))
            assert ev[-1]["stage"] == "error", ev  # paths are rejected
            ev = await events(await c.post("/api/create", json={"source": "tone.mp3", "name": "Bad Name"}, headers=AUTH))
            assert ev[-1]["stage"] == "error", ev

            ev = await events(await c.post("/api/create", json={"source": "tone.mp3", "name": "tone"}, headers=AUTH))
            assert ev[-1]["stage"] == "done", ev
            assert any("trimming" in e.get("message", "") for e in ev), ev  # 5s > MAX_SECONDS
            assert not os.path.exists(os.path.join(discs.AUDIO_DIR, "tone.mp3"))  # MP3 used up

            r = await c.get(f"/{TOKEN}/discs.zip")
            assert r.status == 200
            pack = os.path.join(d, "dl.zip")
            open(pack, "wb").write(await r.read())
            with zipfile.ZipFile(pack) as z:
                names = z.namelist()
                assert "assets/minecraft/sounds/custom/tone.ogg" in names and "pack.mcmeta" in names
                assert "customdisc.tone" in json.loads(z.read("assets/minecraft/sounds.json"))
                cmd = json.load(open(discs.DISCS_JSON))["tone"]["customModelData"]
                entries = json.loads(z.read("assets/minecraft/items/music_disc_13.json"))["model"]["entries"]
                assert entries[0]["threshold"] == cmd
            assert len(open(discs.PACK_PATH + ".sha1").read()) == 40

            assert (await (await c.post("/api/delete", json={"name": "tone"}, headers=AUTH)).json())["ok"]
            assert "tone" not in json.load(open(discs.DISCS_JSON))
            assert (await c.get("/wrong/discs.zip")).status == 404

        # Pack-only mode: no API routes at all.
        async with TestClient(TestServer(bot.pack_app(TOKEN))) as c:
            assert (await c.post("/api/create", json={}, headers=AUTH)).status in (404, 405)


def secrets_persist():
    with tempfile.TemporaryDirectory() as d:
        bot.SECRETS_FILE = os.path.join(d, ".disc-secrets.json")
        os.environ.pop("PACK_TOKEN", None)
        os.environ.pop("DISC_API_SECRET", None)
        first = bot.load_secrets()
        assert len(first[0]) == 32 and first[0] != first[1]
        assert oct(os.stat(bot.SECRETS_FILE).st_mode & 0o777) == "0o600"
        assert bot.load_secrets() == first  # same on the next start (redeploys keep the pack URL)
        os.environ["PACK_TOKEN"] = "p" * 24
        assert bot.load_secrets() == ("p" * 24, first[1])  # env wins, and is saved for the plugin
        assert json.load(open(bot.SECRETS_FILE))["pack_token"] == "p" * 24
        del os.environ["PACK_TOKEN"]


asyncio.run(main())
secrets_persist()
print("ok")
