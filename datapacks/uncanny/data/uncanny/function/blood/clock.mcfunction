# Dusk (12500-12999 on the day timeline) rolls once per night.
execute store result score #day un.tmp run time query day
execute if score #day un.tmp matches 12500..12999 unless score #rolled un.tmp matches 1 run function uncanny:blood/roll
execute if score #day un.tmp matches ..12499 run scoreboard players set #rolled un.tmp 0
execute if score #blood un.t matches 1 run function uncanny:blood/tick
