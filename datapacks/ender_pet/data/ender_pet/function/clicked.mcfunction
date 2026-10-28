# As the clicked hitbox.
data remove entity @s interaction
execute as @e[type=enderman,tag=ep.pet,distance=..1,limit=1,sort=nearest] at @s run function ender_pet:command
