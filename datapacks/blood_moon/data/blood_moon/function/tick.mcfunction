execute as @a at @s if dimension minecraft:overworld run particle minecraft:dust{color:[0.6,0.0,0.0],scale:1.5} ~ ~6 ~ 10 4 10 0 4 normal @s
# Each new Overworld hostile is looked at once (ADR 0007).
execute in minecraft:overworld as @e[type=#blood_moon:hostile,tag=!bm.seen,distance=0..] at @s run function blood_moon:mob
