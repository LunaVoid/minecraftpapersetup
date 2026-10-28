#!/usr/bin/env python3
"""A/B tick-time benchmark on the running dev server (dev/docker-compose.yml).

Usage: python3 scripts/bench.py [path/to/old/pack/dir]
Needs one player online: the old Tameable Enderman pack only runs per online player.
Builds the same busy area (300 dropped items, 100 mobs, 5 pets) under each config and
reads Paper's 10s average tick time. Without an old-pack path it measures baseline vs Ender Pet.
"""
import re, shutil, subprocess, sys, time, os

SETTLE, SAMPLES = 15, 3
OLD_DIR = "dev/data/world/datapacks/tameable-enderman"


def r(cmd):
    out = subprocess.run(["docker", "exec", "mc-dev", "rcon-cli", cmd], capture_output=True, text=True).stdout
    return re.sub(r"\x1b\[[0-9;]*m", "", out)


def mspt():
    """Average of SAMPLES readings of Paper's 10s-window average tick time."""
    time.sleep(SETTLE)
    vals = []
    for _ in range(SAMPLES):
        m = re.findall(r"([\d.]+)/([\d.]+)/([\d.]+)", r("mspt"))
        vals.append(float(m[1][0]))  # second triple = last 10s: avg/min/max
        time.sleep(5)
    return sum(vals) / len(vals)


def run(name, player, *setup):
    r("function devkit:bench/setup")
    for s in setup:
        r(f"execute as {player} at @s run {s}")
    v = mspt()
    print(f"{name:<28} {v:6.2f} ms/tick")
    r("function devkit:bench/clear")
    return v


def main():
    os.chdir(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    players = re.findall(r"online: (.+)", r("list"))
    if not players or not players[0].strip():
        sys.exit("need one player online on the dev server")
    player = players[0].split(",")[0].strip()
    old_src = sys.argv[1] if len(sys.argv) > 1 else None

    r('datapack disable "file/ender_pet"')
    results = {"baseline (no pet pack)": run("baseline (no pet pack)", player)}

    r('datapack enable "file/ender_pet"')
    results["Ender Pet"] = run("Ender Pet", player, "function devkit:bench/pets_new")
    r('datapack disable "file/ender_pet"')

    if old_src:
        shutil.copytree(old_src, OLD_DIR, dirs_exist_ok=True)
        r("reload")
        r('datapack enable "file/tameable-enderman"')
        try:
            results["old Tameable Enderman"] = run("old Tameable Enderman", player, "function devkit:bench/pets_old")
        finally:
            r('datapack disable "file/tameable-enderman"')
            shutil.rmtree(OLD_DIR, ignore_errors=True)
            r("reload")

    r('datapack enable "file/ender_pet"')
    base = results["baseline (no pet pack)"]
    for k, v in results.items():
        if k != "baseline (no pet pack)":
            print(f"{k}: +{v - base:.2f} ms/tick over baseline (1 player online)")


if __name__ == "__main__":
    main()
