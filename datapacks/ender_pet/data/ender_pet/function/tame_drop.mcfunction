# As a Magic Ender Pearl item next to an untamed enderman.
tag @p[distance=..16] add ep.newowner
execute as @e[type=enderman,tag=!ep.pet,distance=..3,limit=1,sort=nearest] at @s run function ender_pet:tame
tag @a remove ep.newowner
item modify entity @s contents {type:"minecraft:set_count",count:-1,add:true}
