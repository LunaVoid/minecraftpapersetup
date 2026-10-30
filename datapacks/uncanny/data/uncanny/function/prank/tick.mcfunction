scoreboard players remove #prank un.t 1
execute unless entity @a[tag=un.victim] run return run function uncanny:prank/end
# No evidence: anyone else coming near ends it on the spot.
execute at @a[tag=un.victim] if entity @a[distance=0.01..48] run return run function uncanny:prank/end
execute if score #prank un.t matches ..0 run return run function uncanny:prank/end
execute if score #prank un.kind matches 1 as @a[tag=un.victim] at @s run function uncanny:watcher/tick
execute if score #prank un.kind matches 3 as @a[tag=un.victim] at @s run function uncanny:party/tick
