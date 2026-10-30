# As/at the victim. It stares; it's gone the moment they look at it or walk up to it. Then the whisper.
execute if score #prank un.t matches 1 unless entity @e[type=mannequin,tag=un.watcher] run return run function uncanny:watcher/whisper {msg:"no one else will believe you"}
execute unless entity @e[type=mannequin,tag=un.watcher] run return fail
execute as @e[type=mannequin,tag=un.watcher] at @s run rotate @s facing entity @a[tag=un.victim,limit=1] eyes
execute if entity @e[type=mannequin,tag=un.watcher,distance=..10] run return run function uncanny:watcher/vanish
execute anchored eyes positioned ^ ^ ^8 if entity @e[type=mannequin,tag=un.watcher,distance=..2.5] run return run function uncanny:watcher/vanish
execute anchored eyes positioned ^ ^ ^16 if entity @e[type=mannequin,tag=un.watcher,distance=..3.5] run return run function uncanny:watcher/vanish
execute anchored eyes positioned ^ ^ ^24 if entity @e[type=mannequin,tag=un.watcher,distance=..4.5] run return run function uncanny:watcher/vanish
execute anchored eyes positioned ^ ^ ^32 if entity @e[type=mannequin,tag=un.watcher,distance=..5.5] run return run function uncanny:watcher/vanish
