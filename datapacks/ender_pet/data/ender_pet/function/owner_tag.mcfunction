# As a pet: tag its owner (if online) with ep.o.
tag @a remove ep.o
scoreboard players operation #o ep.tmp = @s ep.owner
execute as @a if score @s ep.id = #o ep.tmp run tag @s add ep.o
