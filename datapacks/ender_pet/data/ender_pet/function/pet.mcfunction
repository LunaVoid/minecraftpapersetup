# As each pet, at it, every 0.5s.
execute unless entity @e[type=interaction,tag=ep.hit,distance=..1] run summon minecraft:interaction ~ ~ ~ {Tags:["ep.hit"],width:0.8f,height:3.0f,response:1b}
execute if score @s ep.cd matches 1.. run scoreboard players remove @s ep.cd 1
execute if score @s ep.trip matches 1.. run return run function ender_pet:fetch/step
# Guard duty: golems/Wardens within 5 blocks, the Wither within 10.
execute as @e[type=minecraft:iron_golem,distance=..5] at @s run function ender_pet:smite
execute as @e[type=minecraft:warden,distance=..5] at @s run function ender_pet:smite
execute as @e[type=minecraft:wither,distance=..10] at @s run function ender_pet:smite
kill @e[type=minecraft:wither_skull,distance=..10]
particle minecraft:portal ~ ~1.5 ~ 0.3 0.8 0.3 0.2 2
execute unless entity @s[tag=ep.stay] run function ender_pet:follow
