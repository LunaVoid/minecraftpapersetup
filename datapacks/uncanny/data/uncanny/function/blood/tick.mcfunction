bossbar set uncanny:blood_moon players @a
scoreboard players set #bar un.tmp 23000
scoreboard players operation #bar un.tmp -= #day un.tmp
execute store result bossbar uncanny:blood_moon value run scoreboard players get #bar un.tmp
execute as @a at @s if dimension minecraft:overworld run particle minecraft:dust{color:[0.6,0.0,0.0],scale:1.5} ~ ~6 ~ 10 4 10 0 4 normal @s
# Each new Overworld hostile is looked at once (ADR 0007).
execute in minecraft:overworld as @e[type=#uncanny:hostile,tag=!un.seen,distance=0..] at @s run function uncanny:blood/mob
