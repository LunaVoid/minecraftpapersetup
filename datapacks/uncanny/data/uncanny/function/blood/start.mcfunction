scoreboard players set #blood un.t 1
bossbar set uncanny:blood_moon players @a
bossbar set uncanny:blood_moon visible true
title @a times 10 70 20
title @a subtitle {text:"The Blood Moon rises",color:"dark_red"}
title @a title ""
title @a actionbar {text:"Monsters grow stronger tonight",color:"red"}
execute as @a at @s run playsound minecraft:event.raid.horn ambient @s ~ ~ ~ 64 0.7
