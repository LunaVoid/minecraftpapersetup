# /function devkit:kit  -- everything needed to test Ender Pet, plus 3 endermen in front of you.
weather clear 1000000
give @s minecraft:ender_pearl 16
give @s minecraft:poppy 16
give @s minecraft:ender_eye 4
give @s minecraft:ender_pearl[minecraft:custom_data={ep_magic:1b},minecraft:enchantment_glint_override=true,minecraft:rarity="epic",minecraft:item_name={text:"Magic Ender Pearl",color:"light_purple"}] 4
give @s minecraft:iron_golem_spawn_egg 2
function devkit:endermen
tellraw @s [{text:"Dev kit: ",color:"light_purple"},{text:"drop (Q) a Magic Pearl next to an enderman to tame it. Right-click = stay/follow, sneak + right-click with hotbar slot 6-9 selected = fetch. ",color:"gray"},{text:"/function devkit:endermen",color:"aqua"},{text:" for more, ",color:"gray"},{text:"/function devkit:reset",color:"aqua"},{text:" to clear.",color:"gray"}]
