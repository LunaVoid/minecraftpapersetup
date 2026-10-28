# As a plain pearl item next to a poppy item: use one of each, make a Magic Ender Pearl.
item modify entity @s contents {type:"minecraft:set_count",count:-1,add:true}
item modify entity @e[type=item,tag=ep.poppy,distance=..1.5,limit=1,sort=nearest] contents {type:"minecraft:set_count",count:-1,add:true}
summon item ~ ~0.5 ~ {Item:{id:"minecraft:ender_pearl",count:1,components:{"minecraft:custom_data":{ep_magic:1b},"minecraft:enchantment_glint_override":true,"minecraft:rarity":"epic","minecraft:item_name":{text:"Magic Ender Pearl",color:"light_purple"}}}}
particle minecraft:portal ~ ~0.5 ~ 0.2 0.2 0.2 0.5 30
playsound minecraft:block.amethyst_block.chime neutral @a[distance=..16]
