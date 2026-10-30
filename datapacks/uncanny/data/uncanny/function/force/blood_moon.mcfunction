# Jumps time forward to the next Blood Moon night (time only moves forward; nothing is rewound).
execute store result score #pos un.tmp run time query uncanny:blood_moon
function uncanny:force/next_target
scoreboard players operation #target un.tmp -= #pos un.tmp
execute store result storage uncanny:tmp skip int 1 run scoreboard players get #target un.tmp
function uncanny:force/skip with storage uncanny:tmp
