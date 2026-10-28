# As the pet; owner sneak-clicked it. Hotbar slots 6-9 (index 5-8) pick what to fetch.
execute if score @s ep.cd matches 1.. run return run function ender_pet:fetch/resting
execute store result score #slot ep.tmp run data get entity @a[tag=ep.clicker,limit=1] SelectedItemSlot
execute unless score #slot ep.tmp matches 5..8 run return run title @a[tag=ep.clicker] actionbar {text:"Select hotbar slot 6 (cobble), 7 (wood), 8 (food) or 9 (ores)",color:"gray"}
scoreboard players operation @s ep.kind = #slot ep.tmp
# 6 steps of 0.5s = 3s away.
scoreboard players set @s ep.trip 6
effect give @s minecraft:invisibility 5 0 true
particle minecraft:portal ~ ~1.5 ~ 0.3 0.8 0.3 1 60
playsound minecraft:entity.enderman.teleport neutral @a[distance=..16]
title @a[tag=ep.clicker] actionbar {text:"Off to fetch...",color:"light_purple"}
