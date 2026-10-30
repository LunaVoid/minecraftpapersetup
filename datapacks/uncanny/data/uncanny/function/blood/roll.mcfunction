scoreboard players set #rolled un.tmp 1
execute store result score #r un.tmp run random value 1..10
execute if score #r un.tmp matches 1 run function uncanny:blood/start
