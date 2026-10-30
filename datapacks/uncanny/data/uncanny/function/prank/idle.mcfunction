# Leftovers from a prank cut short by a logout or an unloaded chunk.
execute as @e[tag=un.party] run function uncanny:party/restore
execute as @e[type=mannequin,tag=un.watcher] run function uncanny:watcher/remove
tag @a remove un.victim
scoreboard players remove @a[scores={un.cd=1..}] un.cd 1
# Target mode (/function uncanny:target {name:"..."}): only that player gets pranks, even while they're offline.
execute if data storage uncanny:cfg target run return run function uncanny:prank/roll_target with storage uncanny:cfg
execute as @a[gamemode=!creative,gamemode=!spectator] unless score @s un.cd matches 1.. at @s unless entity @a[distance=0.01..48] run function uncanny:prank/roll
