# /execute as <player> at @s run function uncanny:force/watcher
function uncanny:prank/end
tag @s add un.victim
function uncanny:watcher/start
execute unless score #prank un.t matches 1.. run tellraw @s {text:"No spot for the Watcher: needs two free blocks over ground ~20 blocks behind or beside you.",color:"gray"}
