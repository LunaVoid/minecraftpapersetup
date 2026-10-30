# As/at the victim. First they hop over to the victim...
execute if score #prank un.t matches 25.. as @e[tag=un.party] at @s run return run function uncanny:party/step
execute if score #prank un.t matches 25.. run return 0
# ...then dance. Odd beats: hop up and spin; even beats: land facing the victim.
scoreboard players operation #beat un.tmp = #prank un.t
scoreboard players operation #beat un.tmp %= #2 un.tmp
execute if score #beat un.tmp matches 1 as @e[tag=un.party,tag=!un.up] at @s run tp @s ~ ~0.6 ~ ~180 ~
execute if score #beat un.tmp matches 1 run tag @e[tag=un.party] add un.up
execute if score #beat un.tmp matches 0 as @e[tag=un.party,tag=un.up] at @s run function uncanny:party/land
execute as @e[type=sheep,tag=un.party] store result entity @s Color byte 1 run random value 0..15
execute at @e[tag=un.party] run particle minecraft:note ~ ~2 ~ 0.3 0.2 0.3 1 1 normal @s
# Four-note riff over a kick drum.
scoreboard players operation #step un.tmp = #prank un.t
scoreboard players operation #step un.tmp %= #8 un.tmp
execute if score #beat un.tmp matches 0 run playsound minecraft:block.note_block.basedrum record @s ~ ~ ~ 1 1
execute if score #step un.tmp matches 7 run playsound minecraft:block.note_block.bit record @s ~ ~ ~ 1 0.75
execute if score #step un.tmp matches 5 run playsound minecraft:block.note_block.bit record @s ~ ~ ~ 1 1
execute if score #step un.tmp matches 3 run playsound minecraft:block.note_block.bit record @s ~ ~ ~ 1 1.26
execute if score #step un.tmp matches 1 run playsound minecraft:block.note_block.bit record @s ~ ~ ~ 1 1.5
