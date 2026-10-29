# Custom Discs: setup guide

Custom music discs from YouTube links or MP3s, all on our own server. Two programs, and the Minecraft server only ever holds files:

| Piece | Where | What it does |
|---|---|---|
| **Custom Discs** plugin | `plugins/urlcustomdiscs/` → inside the Minecraft server | The in-game half: `/disc` commands, jukebox, giving discs, pushing the pack to players. Runs no downloads or conversions |
| **Disc bot** | `discord-bot/` → its own Coolify app | Does all the heavy work: yt-dlp, FFmpeg, building the disc pack. Serves the pack to players. Optionally saves MP3s posted in a Discord channel |
| Files | `plugins/URLCustomDiscs/` on the server's data volume (shared) | `tracks/*.ogg`, `discs.json`, `URLCustomDiscsPack.zip` + `.sha1` (written by the bot), `audio_to_send/` (MP3 drop folder), `config.yml` (plugin) |

```
/disc create <link> <name>  →  plugin ──POST /api/create (secret)──→  disc bot: yt-dlp → FFmpeg → tracks/ → pack
                               plugin ←── streamed progress ─────────┘
players' games ──GET /<PACK_TOKEN>/discs.zip──→ disc bot
```

Why: [ADR 0003](adr/0003-fork-urlcustomdiscs-self-hosted-only.md) (fork), [0004](adr/0004-disc-bot-separate-coolify-app.md) (bot as its own app), [0005](adr/0005-disc-pack-secret-url-and-hash.md) (secret pack URL + hash), [0006](adr/0006-disc-pipeline-in-bot-container.md) (heavy work in the bot).

## Secrets: none of these go in the repo

| Secret | What it does | Who sees it |
|---|---|---|
| `DISC_API_SECRET` | Lets the plugin create/delete discs through the bot | Nobody else |
| `PACK_TOKEN` | The unguessable part of the pack URL | Every player (it's in the URL). Can only download the pack |
| `DISCORD_BOT_TOKEN` | Optional Discord login | Nobody else |

You don't make the first two: on first start the bot generates them and saves them to `plugins/URLCustomDiscs/.disc-secrets.json` (owner-only, on the server, never in git); the plugin reads them from there. To rotate one, delete that file (or set the env var in Coolify, which wins and gets saved) and restart the bot, then the server.

## 1. Release the plugin

Bump the version in `pom.xml` (`finalName`) and `src/main/resources/plugin.yml`, then:

```sh
cd plugins/urlcustomdiscs && mvn package
gh release create discs-v<version> target/URLCustomDiscs-v<version>.jar --title "Custom Discs <version>" --notes "..."
```

## 2. Disc bot on Coolify

1. **New Resource → Application** → this repo, branch `main`, build pack **Docker Compose**, base directory **`/discord-bot`** (uses `discord-bot/docker-compose.yml`). Never the repo root: that compose file is the Minecraft server.
2. **Storage** is declared in `discord-bot/docker-compose.yml`: only the plugin folder, `/data/coolify/services/<minecraft service id>/minecraft-data/plugins/URLCustomDiscs` → `/data/plugins/URLCustomDiscs`. If the Minecraft service ID changes, edit that path in `discord-bot/docker-compose.yml` (Coolify can't parse `${VAR:-default}` in a volume) (`docker inspect <mc container> --format '{{json .Mounts}}'`). The folder must already exist, or Docker creates it owned by root.
3. **Environment**: nothing required. Optional: `MAX_SECONDS` (default 600; longer tracks are trimmed), `DISCORD_BOT_TOKEN` + `DISCORD_CHANNEL_ID`, and `PACK_TOKEN` / `DISC_API_SECRET` if you want to choose them yourself.
4. **Domain**: e.g. `https://packs.<your domain>` on the `disc-bot` service, port `8080`.
5. Deploy. The startup log checks everything:
   ```
   Plugin folder /data/plugins/URLCustomDiscs: found        ← MISSING = wrong mount
   Drop folder /data/plugins/URLCustomDiscs/audio_to_send: writable
   Saved secrets to /data/plugins/URLCustomDiscs/.disc-secrets.json   ← first start only
   Disc API enabled
   yt-dlp update ok, now 2026.x.y
   ```
   CPU is capped at 1 core and 768 MB in the compose file, jobs run one at a time at low priority, so the game keeps its CPU.
6. Check from anywhere: `curl -I https://packs.<your domain>/<PACK_TOKEN>/discs.zip` → `200` once a disc exists; `curl -X POST https://packs.<your domain>/api/create` → `401`.

## 3. Minecraft server

The server isn't deployed from this repo, so the jar goes in by hand. On the host:

```sh
cd /data/coolify/services/<minecraft service id>/minecraft-data/plugins
curl -fL -o URLCustomDiscs.jar https://github.com/LunaVoid/minecraftpapersetup/releases/download/discs-v<version>/URLCustomDiscs-v<version>.jar
sha1sum URLCustomDiscs.jar                     # compare with the release notes
rm -f URLCustomDiscs-v2.7.2-1.21-1.21.8.jar    # upstream plugin, if still there
rm -rf URLCustomDiscs/bin URLCustomDiscs/edit_resource_pack URLCustomDiscs/config.yml   # leftovers of fork.1 / upstream
chown 1000:1000 URLCustomDiscs.jar
```

Remove `url-custom-discs` from the Minecraft service's `MODRINTH_PROJECTS` if it's there. Restart; the plugin writes a fresh `URLCustomDiscs/config.yml`. Set the one line and restart again:

```yaml
botURL: "https://packs.<your domain>"
```

`botSecret` and `downloadResourcePackURL` stay empty: they come from the bot's `.disc-secrets.json`.

Leave `resource-pack=` in `server.properties` empty: the plugin sends the pack (with its hash) on join. Discs made with fork.1 are kept: the bot moves their audio from the old pack into `tracks/` on its first start.

## 4. Use it

| Command | What |
|---|---|
| `/disc` | Clickable help |
| `/disc create <YouTube link> <name>` | Download → disc. Progress shows above your hotbar |
| `/disc create song.mp3` | From an MP3 in the drop folder (e.g. via the Discord bot) |
| `/disc list` | Every disc; click to get it, hover for where it came from |
| `/disc get <name>` · `/disc delete <name>` | Get a copy back / remove for everyone |
| `/disc updatedep` | Update yt-dlp in the bot when YouTube downloads fail |
| add `stereo` at the end of `create` | Full volume everywhere, no fading (default is mono, which fades with distance like a real jukebox) |

Commands are op-only. Everyone online gets a resource pack prompt after each create/delete and must accept it to hear new discs. If a YouTube download fails with "Sign in to confirm you're not a bot", YouTube is blocking the server's IP: MP3s via the drop folder still work.

## Local testing

```sh
(cd plugins/urlcustomdiscs && mvn package)
MC_USERNAME=YourName docker compose -f dev/docker-compose.yml up -d --build
```

runs Minecraft and the disc bot (no Discord). Put this in `dev/data/plugins/URLCustomDiscs/config.yml`, then restart `mc-dev`:

```yaml
botURL: "http://disc-bot:8765"
downloadResourcePackURL: "http://localhost:8765/devpacktoken000000000000/discs.zip"
```

The bot's tests (`discord-bot/test_bot.py`) run the real pipeline with FFmpeg on every `docker build`.

## License

The plugin is a fork of [TheoDgb/URLCustomDiscs](https://github.com/TheoDgb/URLCustomDiscs) (forked at `49a7270`), licensed CC-BY-NC 4.0 (`plugins/urlcustomdiscs/LICENSE.md`): non-commercial use only, credit kept. Our changes are marked with `// fork:` comments in the source.
