# Frozen (NoAI) so they don't wander or teleport during tests; taming sets NoAI anyway.
execute at @s run summon minecraft:enderman ^-2 ^ ^4 {NoAI:1b}
execute at @s run summon minecraft:enderman ^ ^ ^4 {NoAI:1b}
execute at @s run summon minecraft:enderman ^2 ^ ^4 {NoAI:1b}
