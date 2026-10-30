# Blood Moon nights are fixed by timeline/blood_moon.json (days 3, 12, 23 of a 31-day cycle): the client
# reads the same timeline to colour the sky, so gameplay follows it here instead of rolling dice.
execute store result score #pos un.tmp run time query uncanny:blood_moon
scoreboard players operation #day un.tmp = #pos un.tmp
scoreboard players operation #day un.tmp %= #24000 un.tmp
scoreboard players operation #pos un.tmp /= #24000 un.tmp
scoreboard players set #night un.tmp 0
execute if score #day un.tmp matches 12500..22999 if score #pos un.tmp matches 3 run scoreboard players set #night un.tmp 1
execute if score #day un.tmp matches 12500..22999 if score #pos un.tmp matches 12 run scoreboard players set #night un.tmp 1
execute if score #day un.tmp matches 12500..22999 if score #pos un.tmp matches 23 run scoreboard players set #night un.tmp 1
execute if score #night un.tmp matches 1 unless score #blood un.t matches 1 run function uncanny:blood/start
execute if score #night un.tmp matches 0 if score #blood un.t matches 1 run function uncanny:blood/end
execute if score #blood un.t matches 1 run function uncanny:blood/tick
