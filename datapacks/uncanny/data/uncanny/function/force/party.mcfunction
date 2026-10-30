# /execute as <player> at @s run function uncanny:force/party
function uncanny:prank/end
tag @s add un.victim
function uncanny:party/start
execute unless score #prank un.t matches 1.. run tellraw @s {text:"Animal party needs 2+ animals within 24 blocks of you.",color:"gray"}
