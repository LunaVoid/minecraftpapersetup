tag @s add un.seen
execute unless entity @s[tag=un.clone] unless data entity @s {"Paper.SpawnReason":"NATURAL"} run return fail
tag @s add un.blood
attribute @s minecraft:max_health modifier add uncanny:blood 0.5 add_multiplied_base
attribute @s minecraft:movement_speed modifier add uncanny:blood 0.15 add_multiplied_base
# Creepers have no attack damage; this line just fails for them.
attribute @s minecraft:attack_damage modifier add uncanny:blood 0.5 add_multiplied_base
data modify entity @s Health set value 1000f
execute unless entity @s[tag=un.clone] if predicate uncanny:half run function uncanny:blood/clone
