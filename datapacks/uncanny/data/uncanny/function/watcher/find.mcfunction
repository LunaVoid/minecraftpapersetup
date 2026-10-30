# Scan from 6 above to 6 below for a spot with two free blocks over solid ground.
scoreboard players set #dy un.tmp 13
execute align xyz positioned ~0.5 ~6 ~0.5 run function uncanny:watcher/scan
