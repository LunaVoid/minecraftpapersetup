scoreboard players reset #prank un.t
scoreboard players reset #prank un.kind
execute as @e[tag=un.party] run function uncanny:party/restore
execute as @e[type=mannequin,tag=un.watcher] run function uncanny:watcher/remove
tag @a remove un.victim
