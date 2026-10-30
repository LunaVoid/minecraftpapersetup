execute if entity @s[tag=un.up] at @s run tp @s ~ ~-0.6 ~
execute unless entity @s[tag=un.keepai] run data merge entity @s {NoAI:0b}
execute if entity @s[type=sheep] store result entity @s Color byte 1 run scoreboard players get @s un.color
scoreboard players reset @s un.color
tag @s remove un.party
tag @s remove un.keepai
tag @s remove un.up
