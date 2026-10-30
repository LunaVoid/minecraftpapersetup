# As a solo player. ~1 prank per 2h of solo play: 14400 half-second loops.
execute store result score #r un.tmp run random value 1..14400
execute unless score #r un.tmp matches 1 run return fail
# Another solo player won this same loop.
execute if score #prank un.t matches 1.. run return fail
function uncanny:prank/start
