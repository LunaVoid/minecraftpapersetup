# As a following pet, at it.
function ender_pet:owner_tag
execute unless entity @a[tag=ep.o] run return fail
execute if entity @a[tag=ep.o,distance=..8] run return run rotate @s facing entity @a[tag=ep.o,limit=1] eyes
# Too far or another dimension: blink to the owner once they're standing on something.
execute unless entity @a[tag=ep.o,predicate=ender_pet:on_ground] run return fail
tp @e[type=interaction,tag=ep.hit,distance=..1,limit=1,sort=nearest] @a[tag=ep.o,limit=1]
tp @s @a[tag=ep.o,limit=1]
execute at @s run particle minecraft:portal ~ ~1.5 ~ 0.3 0.8 0.3 1 40
execute at @s run playsound minecraft:entity.enderman.teleport neutral @a[distance=..16]
