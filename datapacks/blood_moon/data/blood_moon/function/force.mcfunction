# /function blood_moon:force — jumps time forward to the next Blood Moon night (time only moves forward; nothing is rewound).
execute store result score #pos bm.tmp run time query blood_moon:blood_moon
function blood_moon:next_target
scoreboard players operation #target bm.tmp -= #pos bm.tmp
execute store result storage blood_moon:tmp skip int 1 run scoreboard players get #target bm.tmp
function blood_moon:skip with storage blood_moon:tmp
