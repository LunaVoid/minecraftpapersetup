import asyncio
import os
import tempfile

import bot
from aiohttp.test_utils import TestClient, TestServer

assert bot.disc_name("Purple Rain.mp3") == "purple_rain"
assert bot.disc_name("PurpleRain.MP3") == "purplerain"
assert bot.disc_name("../../etc/passwd.mp3") == "etcpasswd"
assert bot.disc_name("song.v2.mp3") == "songv2"
assert bot.disc_name("!!!.mp3") == "disc"


async def pack_route():
    with tempfile.TemporaryDirectory() as d:
        bot.PACK_PATH = os.path.join(d, "URLCustomDiscsPack.zip")
        async with TestClient(TestServer(bot.pack_app("t" * 24))) as c:
            assert (await c.get(f"/{'t' * 24}/discs.zip")).status == 404  # no pack yet
            open(bot.PACK_PATH, "wb").write(b"PK")
            r = await c.get(f"/{'t' * 24}/discs.zip")
            assert r.status == 200 and await r.read() == b"PK"
            assert (await c.get("/wrong/discs.zip")).status == 404
            assert (await c.get("/")).status == 404
            assert (await c.post(f"/{'t' * 24}/discs.zip")).status == 405


asyncio.run(pack_route())
print("ok")
