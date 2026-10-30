scoreboard objectives add bm.t dummy
scoreboard objectives add bm.tmp dummy
scoreboard players set #24000 bm.tmp 24000
# Same single 0.5s clock idea as Ender Pet (ADR 0002).
schedule function blood_moon:loop 10t replace
