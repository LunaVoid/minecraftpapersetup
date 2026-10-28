# Benchmark world at ~2000,2000 (away from spawn): 10 cells x (30 dropped items + 10 mobs).
forceload add 1984 1984 2031 2031
function devkit:bench/clear
execute positioned 2000 -60 2000 run summon marker ~ ~ ~ {Tags:["bm"]}
execute positioned 2000 -60 2000 run summon marker ~4 ~ ~ {Tags:["bm"]}
execute positioned 2000 -60 2000 run summon marker ~8 ~ ~ {Tags:["bm"]}
execute positioned 2000 -60 2000 run summon marker ~12 ~ ~ {Tags:["bm"]}
execute positioned 2000 -60 2000 run summon marker ~16 ~ ~ {Tags:["bm"]}
execute positioned 2000 -60 2000 run summon marker ~ ~ ~8 {Tags:["bm"]}
execute positioned 2000 -60 2000 run summon marker ~4 ~ ~8 {Tags:["bm"]}
execute positioned 2000 -60 2000 run summon marker ~8 ~ ~8 {Tags:["bm"]}
execute positioned 2000 -60 2000 run summon marker ~12 ~ ~8 {Tags:["bm"]}
execute positioned 2000 -60 2000 run summon marker ~16 ~ ~8 {Tags:["bm"]}
execute at @e[type=marker,tag=bm] run function devkit:bench/cell
