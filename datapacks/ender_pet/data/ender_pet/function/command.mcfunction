# As the clicked pet; the clicking player is tagged ep.clicker.
function ender_pet:owner_tag
execute unless entity @a[tag=ep.clicker,tag=ep.o] run return run title @a[tag=ep.clicker] actionbar {text:"This isn't your enderman",color:"gray"}
execute if score @s ep.trip matches 1.. run return fail
execute if entity @a[tag=ep.clicker,predicate=ender_pet:sneaking] run return run function ender_pet:fetch/start
execute if entity @s[tag=ep.stay] run return run function ender_pet:follow_on
tag @s add ep.stay
title @a[tag=ep.clicker] actionbar {text:"Staying here",color:"light_purple"}
