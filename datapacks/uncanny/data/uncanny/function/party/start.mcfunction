# As/at the victim. Needs 3+ animals (their Ender Pet joins in too).
execute store result score #n un.tmp if entity @e[type=#uncanny:party,distance=..12]
execute if score #n un.tmp matches ..2 run return fail
execute as @e[type=#uncanny:party,distance=..12] run function uncanny:party/join
execute as @e[type=enderman,tag=ep.pet,distance=..12] run function uncanny:party/join
scoreboard players set #prank un.kind 3
# 12s
scoreboard players set #prank un.t 24
