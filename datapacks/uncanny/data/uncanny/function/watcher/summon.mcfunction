# Still running as the victim, so the head loot copies the victim's own skin onto the Watcher.
scoreboard players set #found un.tmp 1
summon mannequin ~ ~ ~ {Tags:["un.watcher","un.new"],Invulnerable:1b,immovable:1b,hide_description:1b,Silent:1b}
loot replace entity @e[type=mannequin,tag=un.new,limit=1] armor.head loot uncanny:victim_head
execute as @e[type=mannequin,tag=un.new,limit=1] run data modify entity @s profile set from entity @s equipment.head.components."minecraft:profile"
item replace entity @e[type=mannequin,tag=un.new,limit=1] armor.head with minecraft:air
tag @e[type=mannequin,tag=un.new] remove un.new
