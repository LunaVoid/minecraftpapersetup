scoreboard players set #blood un.t 1
scoreboard players set #rolled un.tmp 1
bossbar set uncanny:blood_moon players @a
bossbar set uncanny:blood_moon visible true
title @a times 10 70 20
title @a subtitle {text:"Monsters grow stronger tonight",color:"red"}
title @a title {text:"The Blood Moon rises",color:"dark_red",bold:true}
execute as @a at @s run playsound minecraft:event.raid.horn ambient @s ~ ~ ~ 64 0.7
