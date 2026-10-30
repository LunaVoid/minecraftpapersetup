execute if block ~ ~ ~ #uncanny:passable if block ~ ~1 ~ #uncanny:passable unless block ~ ~-1 ~ #uncanny:passable run return run function uncanny:watcher/summon
scoreboard players remove #dy un.tmp 1
execute if score #dy un.tmp matches 1.. positioned ~ ~-1 ~ run function uncanny:watcher/scan
