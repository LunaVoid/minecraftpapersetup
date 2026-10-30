# As/at the victim: ~20 blocks behind them, else to the left, else to the right.
scoreboard players set #found un.tmp 0
execute rotated ~ 0 positioned ^ ^ ^-20 run function uncanny:watcher/find
execute if score #found un.tmp matches 0 rotated ~ 0 positioned ^20 ^ ^ run function uncanny:watcher/find
execute if score #found un.tmp matches 0 rotated ~ 0 positioned ^-20 ^ ^ run function uncanny:watcher/find
execute if score #found un.tmp matches 0 run return fail
scoreboard players set #prank un.kind 1
playsound minecraft:entity.elder_guardian.curse ambient @s ~ ~ ~ 0.5 0.5
# Disc 11 at half speed; it starts quiet, so full volume.
playsound minecraft:music_disc.11 ambient @s ~ ~ ~ 1 0.5
function uncanny:watcher/whisper {msg:"turn around"}
# Gives up after 60s.
scoreboard players set #prank un.t 120
