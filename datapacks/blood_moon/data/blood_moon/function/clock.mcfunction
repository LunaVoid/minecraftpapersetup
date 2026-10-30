# Blood Moon nights are fixed by timeline/blood_moon.json (days 3, 12, 23 of a 31-day cycle): the client
# reads the same timeline to colour the sky, so gameplay follows it here instead of rolling dice.
execute store result score #pos bm.tmp run time query blood_moon:blood_moon
scoreboard players operation #day bm.tmp = #pos bm.tmp
scoreboard players operation #day bm.tmp %= #24000 bm.tmp
scoreboard players operation #pos bm.tmp /= #24000 bm.tmp
scoreboard players set #night bm.tmp 0
execute if score #day bm.tmp matches 12500..22999 if score #pos bm.tmp matches 3 run scoreboard players set #night bm.tmp 1
execute if score #day bm.tmp matches 12500..22999 if score #pos bm.tmp matches 12 run scoreboard players set #night bm.tmp 1
execute if score #day bm.tmp matches 12500..22999 if score #pos bm.tmp matches 23 run scoreboard players set #night bm.tmp 1
execute if score #night bm.tmp matches 1 unless score #blood bm.t matches 1 run function blood_moon:start
execute if score #night bm.tmp matches 0 if score #blood bm.t matches 1 run function blood_moon:end
execute if score #blood bm.t matches 1 run function blood_moon:tick
