# As an enderman tamed by the old pack: clear its leftovers, then tame it for the nearest player.
tag @s remove tamed_enderman
effect clear @s
tag @p[distance=..32] add ep.newowner
function ender_pet:tame
tag @a remove ep.newowner
