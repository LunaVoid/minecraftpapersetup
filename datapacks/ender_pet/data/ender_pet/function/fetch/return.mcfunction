effect clear @s minecraft:invisibility
particle minecraft:portal ~ ~1.5 ~ 0.3 0.8 0.3 1 60
playsound minecraft:entity.enderman.teleport neutral @a[distance=..16]
execute if score @s ep.kind matches 5 run loot spawn ~ ~1 ~ loot ender_pet:fetch/cobblestone
execute if score @s ep.kind matches 6 run loot spawn ~ ~1 ~ loot ender_pet:fetch/wood
execute if score @s ep.kind matches 7 run loot spawn ~ ~1 ~ loot ender_pet:fetch/food
execute if score @s ep.kind matches 8 run loot spawn ~ ~1 ~ loot ender_pet:fetch/ores
# 60 steps of 0.5s = 30s cooldown.
scoreboard players set @s ep.cd 60
