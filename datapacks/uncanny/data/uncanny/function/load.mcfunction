scoreboard objectives add un.t dummy
scoreboard objectives add un.kind dummy
scoreboard objectives add un.cd dummy
scoreboard objectives add un.color dummy
scoreboard objectives add un.tmp dummy
scoreboard players set #2 un.tmp 2
scoreboard players set #8 un.tmp 8
scoreboard players set #24000 un.tmp 24000
bossbar add uncanny:blood_moon {text:"Blood Moon",color:"dark_red"}
bossbar set uncanny:blood_moon color red
bossbar set uncanny:blood_moon max 10500
# Same single 0.5s clock as Ender Pet (ADR 0002).
schedule function uncanny:loop 10t replace
