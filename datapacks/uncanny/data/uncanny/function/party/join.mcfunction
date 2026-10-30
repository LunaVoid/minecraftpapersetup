tag @s add un.party
# Already AI-less (Ender Pets, someone's display mob): leave it that way afterwards.
execute if data entity @s {NoAI:1b} run tag @s add un.keepai
data merge entity @s {NoAI:1b}
execute if entity @s[type=sheep] store result score @s un.color run data get entity @s Color
