# As/at the victim: pick a prank; if it can't start here (no spot, too few animals) try the other one.
tag @s add un.victim
scoreboard players set @s un.cd 14400
execute store result score #pick un.tmp run random value 1..2
execute if score #pick un.tmp matches 1 run function uncanny:watcher/start
execute unless score #prank un.t matches 1.. run function uncanny:party/start
execute unless score #prank un.t matches 1.. run function uncanny:watcher/start
