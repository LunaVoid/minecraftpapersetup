# minecraftpapersetup — Domain Glossary

**Server** — the itzg/minecraft-server container on Coolify running Paper 26.3; `/data` inside it is the `minecraft-data` volume.

**Ender Pet** — our datapack (`datapacks/ender_pet`) that lets players tame endermen. _Avoid:_ "the mod", "Tameable Enderman" (that's the old third-party pack it replaced).

**Pet** — an enderman tamed with Ender Pet (tag `ep.pet`): no AI, invulnerable, owned by one player. _Avoid:_ "tamed enderman" when you mean ours specifically.

**Owner** — the player who tamed a pet; matched by `ep.id` (player) = `ep.owner` (pet). Only the owner can command or untame it.

**Magic Ender Pearl** — an ender pearl with `custom_data {ep_magic:1b}`; made by dropping 1 ender pearl + 1 poppy together, dropped (not thrown) next to an enderman to tame it.

**Hitbox** — the invisible `interaction` entity (tag `ep.hit`) at each pet's feet; it's what makes right-clicks on a pet detectable. _Avoid:_ "interaction" alone (ambiguous with the NBT field).

**Stay / Follow** — a pet's mode, toggled by right-click. Follow = blinks to the owner when they're >8 blocks away and on the ground.

**Fetch trip** — sneak + right-click: the pet vanishes for 3s and returns with loot chosen by the owner's selected hotbar slot (6 cobble, 7 wood, 8 food, 9 ores), then rests 4s. _Avoid:_ "find", "teleport" (the old pack's terms).

**Guard duty** — pets kill iron golems/Wardens within 2 blocks and the Wither/wither skulls within 10.

**Loop** — `ender_pet:loop`, the pack's only clock; runs every 10 ticks (0.5s). Nothing runs per tick or per player.
