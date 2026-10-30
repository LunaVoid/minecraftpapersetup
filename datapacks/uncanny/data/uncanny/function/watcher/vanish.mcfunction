execute at @e[type=mannequin,tag=un.watcher] run particle minecraft:large_smoke ~ ~1 ~ 0.2 0.6 0.2 0.01 12 normal @s
playsound minecraft:ambient.cave ambient @s ~ ~ ~ 1 0.6
execute as @e[type=mannequin,tag=un.watcher] run function uncanny:watcher/remove
scoreboard players set #prank un.t 1
