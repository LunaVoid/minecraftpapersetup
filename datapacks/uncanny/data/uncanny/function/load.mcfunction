scoreboard objectives add un.t dummy
scoreboard objectives add un.kind dummy
scoreboard objectives add un.cd dummy
scoreboard objectives add un.color dummy
scoreboard objectives add un.tmp dummy
scoreboard players set #2 un.tmp 2
scoreboard players set #8 un.tmp 8
# The Blood Moon moved to its own pack; drop the bossbar older versions made.
bossbar remove uncanny:blood_moon
# Same single 0.5s clock as Ender Pet (ADR 0002).
schedule function uncanny:loop 10t replace
