# minecraftpapersetup

Our Paper 26.3 server (itzg/minecraft-server on Coolify, `mc.lunael.org`) and the datapacks we write for it.
Terms used here are defined in [CONTEXT.md](CONTEXT.md); design decisions are in [docs/adr](docs/adr).

## Layout
- `docker-compose.yml` — the Coolify service (env values live in Coolify).
- `plugins/urlcustomdiscs/` + `discord-bot/` — **Custom Discs**: music discs from YouTube links or MP3s, self-hosted. Setup: [docs/custom-discs.md](docs/custom-discs.md).
- `datapacks/ender_pet/` — **Ender Pet**: tame endermen, Stay/Follow, fetch trips. Replaces the laggy third-party Tameable Enderman pack.
- `scripts/test.py` — boots a throwaway Paper 26.3 server and checks the pack loads and works. Run it after every change.
- `scripts/build.sh` — builds `dist/install-ender_pet.sh`, a paste-able installer for the server terminal.

## Ender Pet in game
| Action | How |
|---|---|
| Make a Magic Ender Pearl | Drop 1 ender pearl + 1 poppy together |
| Tame | Drop (Q, don't throw) the Magic Pearl within 3 blocks of an enderman |
| Stay / Follow | Right-click your pet |
| Fetch | Sneak + right-click; hotbar slot 6 cobble, 7 wood, 8 food, 9 ores; 4s cooldown |
| Untame | Drop an Eye of Ender within 3 blocks of it (you get the Magic Pearl back) |

## Playtest locally
Needs Docker and a Minecraft **26.3** client.
1. `MC_USERNAME=YourName docker compose -f dev/docker-compose.yml up -d` (first start downloads Paper; about a minute).
2. In Minecraft: Multiplayer → Direct Connection → `localhost:25566`.
3. In chat: `/function devkit:kit` gives pearls, poppies, Eyes of Ender, Magic Pearls, golem eggs, and spawns 3 endermen in front of you.
4. Edit files under `datapacks/ender_pet/`, then `/reload` in game. No restart needed.
5. Stop with `docker compose -f dev/docker-compose.yml down`. The world lives in `dev/data/` (gitignored; delete it for a fresh world).

`dev/devkit/` is a test-only pack mounted just on this server; it never ships. The log line `ERROR: No key layers in MapLike[{}]` on first start is harmless (the image passes empty flat-world settings; Minecraft falls back to default superflat).

## Try it without cloning (players / testers)
Needs Docker and a Minecraft **26.3** client. One command runs a private test server with the latest released Ender Pet (repo must be public):
```sh
docker run -d --name enderpet-test -p 127.0.0.1:25566:25565 \
  -e EULA=TRUE -e TYPE=PAPER -e VERSION=26.3 -e PAPER_BUILD=227 \
  -e MODE=creative -e LEVEL_TYPE=FLAT -e OPS=YourMinecraftName \
  -e DATAPACKS=https://github.com/LunaVoid/minecraftpapersetup/releases/latest/download/ender_pet.zip \
  itzg/minecraft-server
```
Join `localhost:25566`. Remove it with `docker rm -f enderpet-test`.

## Performance
`python3 scripts/bench.py [old-pack-dir]` (dev server running, one player online) builds the same busy area (300 dropped items, 100 mobs, 5 pets) under each config and compares Paper's 10s average tick time. Result on 2026-10-28 (dev server on a Mac, 1 player): baseline 8.23 ms, Ender Pet 8.23 ms (no measurable cost), old Tameable Enderman 13.43 ms (+5.2 ms, and its cost multiplies with each online player).

## Workflow
1. Edit `datapacks/ender_pet/`.
2. `python3 scripts/test.py` — must print all PASS.
3. `scripts/build.sh`, paste `dist/install-ender_pet.sh` into the Coolify terminal, check the sha1 it prints.
4. Commit and tag (`git tag ender_pet-vX.Y.Z`).

## 26.3 format gotchas (each one cost us a failed load)
- Loot conditions / predicates: dispatch key is `"type"`, not `"condition"`.
- Item modifiers (inline and in loot tables): `"type"`, not `"function"`; loot entries take a single `"modifier"`, and the old `"functions"` key is **silently ignored**.
- Entity predicates use namespaced sub-predicate keys: `"minecraft:entity_type"`, `"minecraft:flags"`.
- Advancement `conditions.entity` must be an object (a list is rejected).
- `pack.mcmeta`: `min_format`/`max_format` = 121 for 26.3.
- `rcon-cli` drops your quotes: wrap commands with quoted args in single quotes, e.g. `rcon-cli 'datapack enable "file/ender_pet"'`.

## Credits
- Luna — server, design, testing
- Claude (Anthropic) — Ender Pet implementation, test harness, docs (co-author on commits)
