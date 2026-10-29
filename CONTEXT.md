# minecraftpapersetup — Domain Glossary

**Server** — the itzg/minecraft-server container on Coolify running Paper 26.3; `/data` inside it is the `minecraft-data` volume.

**Ender Pet** — our datapack (`datapacks/ender_pet`) that lets players tame endermen. _Avoid:_ "the mod", "Tameable Enderman" (that's the old third-party pack it replaced).

**Pet** — an enderman tamed with Ender Pet (tag `ep.pet`): no AI, invulnerable, owned by one player. _Avoid:_ "tamed enderman" when you mean ours specifically.

**Owner** — the player who tamed a pet; matched by `ep.id` (player) = `ep.owner` (pet). Only the owner can command or untame it.

**Magic Ender Pearl** — an ender pearl with `custom_data {ep_magic:1b}`; made by dropping 1 ender pearl + 1 poppy together, dropped (not thrown) next to an enderman to tame it.

**Hitbox** — the invisible `interaction` entity (tag `ep.hit`) at each pet's feet; it's what makes right-clicks on a pet detectable. _Avoid:_ "interaction" alone (ambiguous with the NBT field).

**Stay / Follow** — a pet's mode, toggled by right-click. Follow = blinks to the owner when they're >8 blocks away and on the ground.

**Fetch trip** — sneak + right-click: the pet vanishes for 3s and returns with loot chosen by the owner's selected hotbar slot (6 cobble, 7 wood, 8 food, 9 ores), then rests 4s. _Avoid:_ "find", "teleport" (the old pack's terms).

**Guard duty** — pets kill iron golems/Wardens within 5 blocks and the Wither/wither skulls within 10.

**Loop** — `ender_pet:loop`, the pack's only clock; runs every 10 ticks (0.5s). Nothing runs per tick or per player.

**Custom Discs** — our fork of the URLCustomDiscs Paper plugin (`plugins/urlcustomdiscs`), self-hosted only: it never calls the original author's API. _Avoid:_ "the mod", "URLCustomDiscs" when you mean our build.

**Disc** — a music disc made in game with `/customdisc create <url or file.mp3> <disc_name> <mono|stereo>`; plays from a jukebox or `/playsound minecraft:customdisc.<disc_name>`.

**Disc pack** — `URLCustomDiscsPack.zip`, the resource pack holding every disc's `.ogg`; the plugin rewrites it on each create/delete and pushes it to online players. _Avoid:_ "the resource pack" (ambiguous once we have others).

**Drop folder** — `plugins/URLCustomDiscs/audio_to_send/` on the server's data volume; MP3s placed here (by hand or by the Discord bot) can be turned into discs by filename.

**Disc bot** — `discord-bot/`, a separate Coolify app that saves MP3s posted in the disc channel into the drop folder and serves the disc pack to players. _Avoid:_ "the Discord thing", "the API".

**Disc channel** — the one Discord channel the disc bot watches for MP3 attachments; who can post there is who can submit songs.

**Pack token** — the random secret in the disc pack's URL path (`/<PACK_TOKEN>/discs.zip`); the only thing keeping the URL unguessable. _Avoid:_ "password".
