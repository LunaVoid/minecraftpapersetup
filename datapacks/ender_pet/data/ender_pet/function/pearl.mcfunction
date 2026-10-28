# As a dropped ender pearl item, at it.
execute if items entity @s contents minecraft:ender_pearl[minecraft:custom_data~{ep_magic:1b}] if entity @e[type=enderman,tag=!ep.pet,distance=..2] if entity @p[distance=..16] run return run function ender_pet:tame_drop
execute unless items entity @s contents minecraft:ender_pearl[minecraft:custom_data] as @e[type=item,distance=..1.5] if items entity @s contents minecraft:poppy run tag @s add ep.poppy
execute if entity @e[type=item,tag=ep.poppy,distance=..1.5] run function ender_pet:craft
tag @e[type=item,tag=ep.poppy] remove ep.poppy
