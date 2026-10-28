# As a pet with an Eye of Ender dropped next to it. Only its owner can untame it.
function ender_pet:owner_tag
execute unless entity @a[tag=ep.o,distance=..16] run return fail
give @a[tag=ep.o,limit=1] minecraft:ender_pearl[minecraft:custom_data={ep_magic:1b},minecraft:enchantment_glint_override=true,minecraft:rarity="epic",minecraft:item_name={text:"Magic Ender Pearl",color:"light_purple"}]
kill @e[type=interaction,tag=ep.hit,distance=..1,limit=1,sort=nearest]
tag @s remove ep.pet
tag @s remove ep.stay
effect clear @s minecraft:invisibility
data merge entity @s {NoAI:0b,Invulnerable:0b,PersistenceRequired:0b}
data remove entity @s CustomName
scoreboard players reset @s
particle minecraft:portal ~ ~1.5 ~ 0.3 0.8 0.3 1 60
title @a[tag=ep.o] actionbar {text:"Your enderman is free again",color:"gray"}
