#!/usr/bin/env python3
"""Boot a throwaway Paper server, load every pack in datapacks/, and assert they work.

Usage: python3 scripts/test.py   (needs Java 25+; downloads Paper into .cache/ once)
Covers what can run without a player: clean load, crafting, loot amounts,
taming, hitbox, guard duty, fetch trip; Uncanny's Watcher/Animal party cleanup and Blood Moon.
Clicks/follow/untame, the Whisper, the Watcher's skin and the red sky need a real player.
"""
import hashlib, os, re, shutil, socket, struct, subprocess, sys, time, urllib.request

PAPER_URL = "https://fill-data.papermc.io/v1/objects/416b7d105b1d0192df4afa4b29d2800d9dbeda092a82fbc5b511248a54adde15/paper-26.3-227.jar"
PAPER_SHA256 = "416b7d105b1d0192df4afa4b29d2800d9dbeda092a82fbc5b511248a54adde15"
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, ".cache")
SRV = os.path.join(CACHE, "server")
PORT = 25599


def rcon(*cmds):
    def pkt(i, t, body):
        b = body.encode() + b"\0\0"
        return struct.pack("<iii", len(b) + 8, i, t) + b

    def recv(s):
        n = struct.unpack("<i", s.recv(4))[0]
        d = b""
        while len(d) < n:
            d += s.recv(n - len(d))
        return d[8:-2].decode(errors="replace")

    with socket.create_connection(("127.0.0.1", PORT)) as s:
        s.sendall(pkt(1, 3, "test"))
        recv(s)
        out = []
        for c in cmds:
            s.sendall(pkt(2, 2, c))
            out.append(recv(s))
        return out if len(out) > 1 else out[0]


def until(cond, timeout=5):
    """Poll cond() until true; the pack's clock is 0.5s and a fresh server can tick slowly."""
    end = time.time() + timeout
    while time.time() < end:
        if cond():
            return True
        time.sleep(0.25)
    return False


def check(name, cond, detail=""):
    print(("PASS " if cond else "FAIL ") + name + ("" if cond else f"  -> {detail}"))
    return bool(cond)


def main():
    os.makedirs(SRV, exist_ok=True)
    jar = os.path.join(CACHE, "paper.jar")
    if not os.path.exists(jar):
        urllib.request.urlretrieve(PAPER_URL, jar)
    assert hashlib.sha256(open(jar, "rb").read()).hexdigest() == PAPER_SHA256, "paper.jar checksum mismatch"
    open(os.path.join(SRV, "eula.txt"), "w").write("eula=true\n")
    open(os.path.join(SRV, "server.properties"), "w").write(
        f"online-mode=false\nenable-rcon=true\nrcon.password=test\nrcon.port={PORT}\nserver-port=25598\n"
        "level-type=minecraft\\:flat\nview-distance=4\nsimulation-distance=4\ngenerate-structures=false\n")
    # Fresh world every run: entities saved by a previous run (e.g. a dead golem's poppy)
    # load in after our kill commands and corrupt the checks.
    for w in ("world", "world_nether", "world_the_end"):
        shutil.rmtree(os.path.join(SRV, w), ignore_errors=True)
    for pack in os.listdir(os.path.join(ROOT, "datapacks")):
        shutil.copytree(os.path.join(ROOT, "datapacks", pack), os.path.join(SRV, "world", "datapacks", pack))

    log = open(os.path.join(SRV, "server.out"), "w+")
    proc = subprocess.Popen(["java", "-Xmx2G", "-jar", jar, "--nogui"], cwd=SRV, stdout=log, stderr=subprocess.STDOUT)
    ok = True
    try:
        for _ in range(180):
            log.seek(0)
            if "Done (" in log.read():
                break
            time.sleep(1)
        else:
            sys.exit("server did not start; see .cache/server/server.out")

        rcon('datapack enable "file/ender_pet"', 'datapack enable "file/uncanny"')
        time.sleep(1)
        log.seek(0)
        errors = [l for l in log.read().splitlines() if re.search(r"ender_pet|uncanny", l) and re.search(r"ERROR|Failed|Reload failed", l)]
        ok &= check("packs load with no errors", not errors, errors[:3])
        enabled = rcon("datapack list").split("available")[0]
        ok &= check("packs are enabled", "file/ender_pet" in enabled and "file/uncanny" in enabled, enabled)

        rcon("forceload add 0 0")
        # forceload returns before the chunk is loaded; summoning too early silently does nothing.
        assert until(lambda: "passed" in rcon("execute if loaded 0 -60 0"), 30), "chunk 0,0 never loaded"
        rcon("kill @e[type=!player]",
             'summon item 0 -60 0 {Item:{id:"minecraft:ender_pearl",count:1}}',
             'summon item 0.3 -60 0 {Item:{id:"minecraft:poppy",count:1}}')
        ok &= check("pearl + poppy -> Magic Ender Pearl", until(lambda:
                    "Count: 1" in rcon("execute if entity @e[type=item]") and
                    "passed" in rcon('execute as @e[type=item] if items entity @s contents minecraft:ender_pearl[minecraft:custom_data~{ep_magic:1b}]')),
                    rcon("execute as @e[type=item] run data get entity @s Item", "execute as @e[type=item] run data get entity @s Pos"))

        rcon("kill @e[type=item]")
        drops = rcon(*[f"loot spawn 5 -60 5 loot ender_pet:fetch/{t}" for t in ("cobblestone", "wood", "food", "ores")])
        ok &= check("loot amounts applied (cobble 16, wood 8)",
                    drops[0].startswith("Dropped 16") and drops[1].startswith("Dropped 8"), drops)

        rcon("kill @e[type=!player]", 'summon enderman 0 -60 0 {Tags:["t"]}', "execute as @e[tag=t] at @s run function ender_pet:tame")
        time.sleep(1.2)
        ok &= check("tame sets NoAI + Invulnerable",
                    rcon("data get entity @e[tag=t,limit=1] NoAI").endswith("1b") and
                    rcon("data get entity @e[tag=t,limit=1] Invulnerable").endswith("1b"))
        ok &= check("exactly one hitbox", "Count: 1" in rcon("execute if entity @e[type=interaction,tag=ep.hit]"))

        rcon("execute at @e[tag=t] run summon iron_golem ~4 ~ ~ {NoAI:1b}")
        ok &= check("guard duty kills golem", until(lambda: "failed" in rcon("execute if entity @e[type=iron_golem]")))

        rcon("kill @e[type=item]", "scoreboard players set @e[tag=t] ep.kind 8", "scoreboard players set @e[tag=t] ep.trip 2")
        ok &= check("fetch trip drops loot and starts cooldown", until(lambda:
                    "passed" in rcon("execute if entity @e[type=item]") and
                    re.search(r"has [1-8] \[ep.cd\]", rcon("scoreboard players get @e[tag=t,limit=1] ep.cd"))))

        # Uncanny. A marker stands in for the victim; with no real player tagged, the next loop
        # ends the prank, which is exactly the cleanup path we need to prove restores everything.
        rcon("kill @e[type=!player]", 'summon marker 0 -60 0 {Tags:["v"]}',
             *[f'summon sheep {x} -60 3 {{Color:3b}}' for x in (0, 1, 2)])
        rcon("execute as @e[tag=v] at @s run function uncanny:party/start")
        ok &= check("animal party freezes animals", "Count: 3" in rcon("execute if entity @e[type=sheep,tag=un.party,nbt={NoAI:1b}]"))
        ok &= check("animal party restores AI and colour", until(lambda:
                    "Count: 3" in rcon("execute if entity @e[type=sheep,tag=!un.party,nbt=!{NoAI:1b},nbt={Color:3b}]"))
                    , rcon("execute as @e[type=sheep] run data get entity @s Color"))

        rcon("execute as @e[tag=v] at @s run function uncanny:watcher/start")
        ok &= check("watcher finds a spot", "passed" in rcon("execute if entity @e[type=mannequin,tag=un.watcher]"))
        ok &= check("watcher removed when the prank ends", until(lambda: "failed" in rcon("execute if entity @e[type=mannequin]")))

        rcon("kill @e[type=!player]", "function uncanny:force/blood_moon")
        ok &= check("blood moon starts on a blood night", until(lambda: "currently visible" in rcon("bossbar get uncanny:blood_moon visible")), rcon("time query uncanny:blood_moon"))
        rcon('summon zombie 0 -60 0 {Tags:["un.clone"],NoAI:1b}')
        ok &= check("blood mob is buffed", until(lambda: "is 30.0" in rcon("attribute @e[type=zombie,limit=1] minecraft:max_health get")))
        rcon("time set day")
        ok &= check("blood moon ends at day", until(lambda: "hidden" in rcon("bossbar get uncanny:blood_moon visible")))
    finally:
        try:
            rcon("stop")
        except OSError:
            proc.terminate()
        proc.wait(60)
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
