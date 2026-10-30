# As/at the victim. Needs 2+ animals within 24 blocks; Ender Pets count and join in.
execute store result score #n un.tmp if entity @e[type=#uncanny:party,distance=..24]
execute store result score #p un.tmp if entity @e[type=enderman,tag=ep.pet,distance=..24]
scoreboard players operation #n un.tmp += #p un.tmp
execute if score #n un.tmp matches ..1 run return fail
execute as @e[type=#uncanny:party,distance=..24] run function uncanny:party/join
execute as @e[type=enderman,tag=ep.pet,distance=..24] run function uncanny:party/join
scoreboard players set #prank un.kind 3
# 5s gathering, then 12s dancing.
scoreboard players set #prank un.t 34
