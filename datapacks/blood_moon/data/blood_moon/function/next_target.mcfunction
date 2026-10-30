# The next 13000-tick mark of night 3, 12 or 23 after #pos (wrapping into the next 31-day cycle).
scoreboard players set #target bm.tmp 85000
execute if score #pos bm.tmp matches 85000.. run scoreboard players set #target bm.tmp 301000
execute if score #pos bm.tmp matches 301000.. run scoreboard players set #target bm.tmp 565000
execute if score #pos bm.tmp matches 565000.. run scoreboard players set #target bm.tmp 829000
