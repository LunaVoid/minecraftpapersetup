execute unless block ~ ~ ~ #uncanny:passable positioned ~ ~1 ~ if block ~ ~ ~ #uncanny:passable run return run tp @s ~ ~ ~ facing entity @a[tag=un.victim,limit=1] feet
execute unless block ~ ~ ~ #uncanny:passable run return fail
execute if block ~ ~-1 ~ #uncanny:passable positioned ~ ~-1 ~ run return run tp @s ~ ~ ~ facing entity @a[tag=un.victim,limit=1] feet
tp @s ~ ~ ~ facing entity @a[tag=un.victim,limit=1] feet
