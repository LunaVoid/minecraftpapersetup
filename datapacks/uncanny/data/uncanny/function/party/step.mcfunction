# As/at a party animal: one hop (1.2 blocks) toward the victim, stepping up or down one block to follow the ground.
execute if entity @a[tag=un.victim,distance=..3] run return fail
execute facing entity @a[tag=un.victim,limit=1] feet rotated ~ 0 positioned ^ ^ ^1.2 run function uncanny:party/move
