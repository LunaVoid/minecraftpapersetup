schedule function uncanny:loop 10t replace
# ponytail: one prank at a time server-wide; a second solo player just waits. Per-victim ids would lift this.
execute if score #prank un.t matches 1.. run return run function uncanny:prank/tick
function uncanny:prank/idle
