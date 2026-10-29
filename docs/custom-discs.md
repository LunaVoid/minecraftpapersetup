# Custom Discs: setup guide

Custom music discs from YouTube links or MP3s, all on our own server. Three pieces:

| Piece | Where | What it does |
|---|---|---|
| **Custom Discs** plugin | `plugins/urlcustomdiscs/` → runs inside the Minecraft server | `/disc create …` downloads (yt-dlp) or picks up an MP3, converts it (FFmpeg), adds it to the disc pack, pushes the pack to everyone online |
| **Disc bot** | `discord-bot/` → its own Coolify app | Saves MP3s posted in the disc channel into the drop folder; serves the disc pack to players at a secret URL |
| **Disc pack** | `plugins/URLCustomDiscs/URLCustomDiscsPack.zip` on the server's data volume | The resource pack holding every disc's audio. Generated on the server, never committed (it's copyrighted music) |

Why it's built this way: [ADR 0003](adr/0003-fork-urlcustomdiscs-self-hosted-only.md) (fork), [0004](adr/0004-disc-bot-separate-coolify-app.md) (bot), [0005](adr/0005-disc-pack-secret-url-and-hash.md) (secret URL + hash).

## Secrets: none of these go in the repo

| Secret | Lives in |
|---|---|
| `DISCORD_BOT_TOKEN` | Coolify env of the disc bot |
| `PACK_TOKEN` | Coolify env of the disc bot, and inside `downloadResourcePackURL` in the server's `config.yml` (on the server, not in git) |

The repo is public. If either leaks: reset the bot token in the Discord Developer Portal; for `PACK_TOKEN`, generate a new one and update both places.

## 1. Release the plugin

```sh
cd plugins/urlcustomdiscs && mvn package          # → target/URLCustomDiscs-v2.7.2-fork.1.jar
gh release create discs-v2.7.2-fork.1 target/URLCustomDiscs-v2.7.2-fork.1.jar \
  --title "Custom Discs 2.7.2-fork.1" --notes "Self-hosted fork of URLCustomDiscs"
```

Bump the version in `pom.xml` (`finalName`) and `src/main/resources/plugin.yml` for each new release.

## 2. Discord bot application

1. https://discord.com/developers/applications → **New Application** → **Bot** tab → **Reset Token**, copy it (`DISCORD_BOT_TOKEN`).
2. Same tab: enable **Message Content Intent** (the bot can't see attachments without it).
3. **OAuth2 → URL Generator**: scope `bot`; permissions View Channels, Send Messages, Read Message History. Open the URL and add the bot to your server.
4. Discord: User Settings → Advanced → Developer Mode on. Right-click the disc channel → **Copy Channel ID** (`DISCORD_CHANNEL_ID`).
5. Lock down who can post in that channel: anyone who can post there can submit songs.

## 3. Disc bot on Coolify

1. **New Resource → Application** → this repo, build pack **Docker Compose**, base directory **`/discord-bot`** (so it uses `discord-bot/docker-compose.yml`). Never the repo root: that compose file is the Minecraft server.
2. **Storage** is declared in `discord-bot/docker-compose.yml`: only the plugin folder, `/data/coolify/services/<minecraft service id>/minecraft-data/plugins/URLCustomDiscs` → `/data/plugins/URLCustomDiscs`, so the bot can't see worlds or other plugins. If the Minecraft service ID changes, set `MC_PLUGIN_DIR` in Coolify's env to the new host path (find it with `docker inspect <mc container> --format '{{json .Mounts}}'`). The folder must already exist (start the server with the plugin once first); otherwise Docker creates it owned by root and the plugin can't write its pack.
3. **Environment**:
   - `DISCORD_BOT_TOKEN`, `DISCORD_CHANNEL_ID` (from step 2). Optional: leave both unset to run as the pack server only
   - `PACK_TOKEN`: run `openssl rand -hex 16` and paste the output
4. **Network**: give it a domain (e.g. `https://packs.<your domain>`) and set the exposed port to `8080`.
5. Deploy. The startup log checks the mount for you:
   ```
   Plugin folder /data/plugins/URLCustomDiscs: found        ← MISSING = wrong bind mount
   Disc pack: found (… KB)                                  ← "not created yet" until the plugin's first start
   Drop folder /data/plugins/URLCustomDiscs/audio_to_send: writable
   ```
   then `Logged in as …` (with Discord) or `serving the disc pack only` (without).
6. Check: `curl -I https://packs.<your domain>/<PACK_TOKEN>/discs.zip` → `404` until the server has created the pack (step 4), `200` after. Any other path is always `404`.

## 4. Minecraft server

1. In Coolify's env for the Minecraft service:
   - set `PLUGINS` to `https://github.com/LunaVoid/minecraftpapersetup/releases/download/discs-v2.7.2-fork.1/URLCustomDiscs-v2.7.2-fork.1.jar`
   - remove `url-custom-discs` from `MODRINTH_PROJECTS` if it's there, or the upstream plugin comes back
2. In the server terminal, remove the upstream plugin and its old config (the fork writes a fresh one):
   ```sh
   rm /data/plugins/URLCustomDiscs-v2.7.2-1.21-1.21.8.jar /data/plugins/URLCustomDiscs/config.yml
   ```
   Discs made in the old API mode lived in the author's pack. Delete `/data/plugins/URLCustomDiscs/discs.json` too and make them again.
3. Redeploy. The log should show `Created empty disc pack at …` and a warning that `downloadResourcePackURL is empty`.
4. Edit `/data/plugins/URLCustomDiscs/config.yml`:
   ```yaml
   downloadResourcePackURL: "https://packs.<your domain>/<PACK_TOKEN>/discs.zip"
   maxSeconds: 600   # longer tracks are trimmed
   ```
   Leave `resource-pack=` in `server.properties` empty: the plugin sends the pack (with its hash) on join.
5. Restart the Minecraft server.

## 5. Use it

| Command | What |
|---|---|
| `/disc` | Clickable help |
| `/disc create <YouTube link> <name>` | Download → disc. Progress shows above your hotbar |
| `/disc create song.mp3` | MP3 from the drop folder (what the bot replies with) |
| `/disc list` · `/disc give <name>` · `/disc delete <name>` | Manage discs |
| add `mono` at the end of `create` | Jukebox-style sound that fades with distance (default is stereo) |

Commands are op-only. Everyone online gets a resource pack prompt after each create/delete and must accept it to hear new discs.

**First thing to try:** one YouTube disc. If it fails with "Sign in to confirm you're not a bot", YouTube is blocking the server's IP: MP3s through the bot still work. `/disc updatedep` updates yt-dlp when downloads start failing for other reasons.

## Local testing

`MC_USERNAME=YourName docker compose -f dev/docker-compose.yml up -d --build` runs the plugin (from `plugins/urlcustomdiscs/target/`, so `mvn package` first) and the bot's pack server. Then set in `dev/data/plugins/URLCustomDiscs/config.yml`:

```yaml
downloadResourcePackURL: "http://localhost:8765/devpacktoken000000000000/discs.zip"
```

restart, and drop MP3s into `dev/data/plugins/URLCustomDiscs/audio_to_send/` yourself (no Discord login locally). The bot's tests run on every `docker build` of `discord-bot/`.

## License

The plugin is a fork of [TheoDgb/URLCustomDiscs](https://github.com/TheoDgb/URLCustomDiscs) (forked at `49a7270`), licensed CC-BY-NC 4.0 (`plugins/urlcustomdiscs/LICENSE.md`): non-commercial use only, credit kept. Our changes are marked with `// fork:` comments in the source.
