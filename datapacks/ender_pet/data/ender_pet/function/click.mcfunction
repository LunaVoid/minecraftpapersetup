# Advancement reward: as the player who right-clicked a pet's hitbox.
advancement revoke @s only ender_pet:click
tag @s add ep.clicker
execute as @e[type=interaction,tag=ep.hit,distance=..8] if data entity @s interaction at @s run function ender_pet:clicked
tag @s remove ep.clicker
