schedule function ender_pet:loop 10t replace
# Dropped items: pearl + poppy crafting, taming, untaming. Type-filtered, no NBT reads.
execute as @e[type=item] if items entity @s contents minecraft:ender_pearl at @s run function ender_pet:pearl
execute as @e[type=item] if items entity @s contents minecraft:ender_eye at @s as @e[type=enderman,tag=ep.pet,distance=..2,limit=1,sort=nearest] at @s run function ender_pet:untame
# Endermen tamed with the old Tameable Enderman pack become pets of the nearest player.
execute as @e[type=enderman,tag=tamed_enderman,tag=!ep.pet] at @s if entity @p[distance=..32] run function ender_pet:adopt
execute as @e[type=enderman,tag=ep.pet] at @s run function ender_pet:pet
# Hitboxes whose pet is gone.
execute as @e[type=interaction,tag=ep.hit] at @s unless entity @e[type=enderman,tag=ep.pet,distance=..1] run kill @s
