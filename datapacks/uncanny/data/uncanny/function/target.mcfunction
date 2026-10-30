# /function uncanny:target {name:"PlayerName"} — only this player gets random pranks (works before they log in).
$data modify storage uncanny:cfg target set value "$(name)"
$tellraw @s {text:"Uncanny: only $(name) gets pranks now. /function uncanny:everyone to undo.",color:"gray"}
