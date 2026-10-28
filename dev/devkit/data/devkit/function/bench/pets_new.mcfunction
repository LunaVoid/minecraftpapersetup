# 5 Ender Pet pets owned by the runner, set to Stay so they remain in the bench area.
tag @s add ep.newowner
execute positioned 2000 -60 2004 run summon minecraft:enderman ~ ~ ~ {Tags:["bm","bm_pet"]}
execute positioned 2000 -60 2004 run summon minecraft:enderman ~3 ~ ~ {Tags:["bm","bm_pet"]}
execute positioned 2000 -60 2004 run summon minecraft:enderman ~6 ~ ~ {Tags:["bm","bm_pet"]}
execute positioned 2000 -60 2004 run summon minecraft:enderman ~9 ~ ~ {Tags:["bm","bm_pet"]}
execute positioned 2000 -60 2004 run summon minecraft:enderman ~12 ~ ~ {Tags:["bm","bm_pet"]}
execute as @e[tag=bm_pet] at @s run function ender_pet:tame
tag @e[tag=bm_pet] add ep.stay
tag @s remove ep.newowner
