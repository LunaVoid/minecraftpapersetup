scoreboard objectives add ep.id dummy
scoreboard objectives add ep.owner dummy
scoreboard objectives add ep.trip dummy
scoreboard objectives add ep.cd dummy
scoreboard objectives add ep.kind dummy
scoreboard objectives add ep.tmp dummy
# One loop every 10 ticks (0.5s) instead of per-player-per-tick work.
schedule function ender_pet:loop 10t replace
