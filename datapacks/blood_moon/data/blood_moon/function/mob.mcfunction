tag @s add bm.seen
execute unless entity @s[tag=bm.clone] unless data entity @s {"Paper.SpawnReason":"NATURAL"} run return fail
tag @s add bm.blood
attribute @s minecraft:max_health modifier add blood_moon:buff 0.5 add_multiplied_base
attribute @s minecraft:movement_speed modifier add blood_moon:buff 0.15 add_multiplied_base
# Creepers have no attack damage; this line just fails for them.
attribute @s minecraft:attack_damage modifier add blood_moon:buff 0.5 add_multiplied_base
data modify entity @s Health set value 1000f
execute unless entity @s[tag=bm.clone] if predicate blood_moon:half run function blood_moon:clone
