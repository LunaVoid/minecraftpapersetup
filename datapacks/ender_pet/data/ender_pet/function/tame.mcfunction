# As the enderman being tamed, at it. Owner is the player tagged ep.newowner.
execute as @a[tag=ep.newowner,limit=1] unless score @s ep.id matches 1.. store result score @s ep.id run scoreboard players add #next ep.id 1
scoreboard players operation @s ep.owner = @a[tag=ep.newowner,limit=1] ep.id
tag @s add ep.pet
data merge entity @s {NoAI:1b,Invulnerable:1b,PersistenceRequired:1b,CustomName:{text:"Tamed Enderman",color:"light_purple"}}
summon minecraft:interaction ~ ~ ~ {Tags:["ep.hit"],width:0.8f,height:3.0f,response:1b}
particle minecraft:portal ~ ~1.5 ~ 0.3 0.8 0.3 1 60
playsound minecraft:entity.enderman.ambient neutral @a[distance=..16]
title @a[tag=ep.newowner] actionbar {text:"Enderman tamed! Right-click: stay/follow. Sneak + right-click: fetch (slots 6-9)",color:"light_purple"}
