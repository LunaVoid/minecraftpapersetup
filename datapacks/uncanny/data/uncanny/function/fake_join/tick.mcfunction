# As/at the victim; only they see or hear any of it. 70 loops = 35s.
execute if score #prank un.t matches 68 run tellraw @s {translate:"multiplayer.player.joined",with:["Herobrine"],color:"yellow"}
execute if score #prank un.t matches 47..50 rotated ~ 0 positioned ^ ^ ^-7 run playsound minecraft:block.stone.step player @s ~ ~ ~ 0.5
execute if score #prank un.t matches 43..46 rotated ~ 0 positioned ^ ^ ^-4 run playsound minecraft:block.stone.step player @s ~ ~ ~ 0.6
execute if score #prank un.t matches 40..42 rotated ~ 0 positioned ^ ^ ^-2 run playsound minecraft:block.stone.step player @s ~ ~ ~ 0.7
execute if score #prank un.t matches 2 run tellraw @s {translate:"multiplayer.player.left",with:["Herobrine"],color:"yellow"}
