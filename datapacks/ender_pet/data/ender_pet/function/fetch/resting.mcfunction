scoreboard players operation #s ep.tmp = @s ep.cd
scoreboard players set #2 ep.tmp 2
scoreboard players operation #s ep.tmp /= #2 ep.tmp
scoreboard players add #s ep.tmp 1
title @a[tag=ep.clicker] actionbar [{text:"Resting... ready in ",color:"gray"},{score:{name:"#s",objective:"ep.tmp"}},"s"]
