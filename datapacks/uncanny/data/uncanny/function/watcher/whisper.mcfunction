# As the victim. Looks exactly like a real /msg, sent by their own name. Call with {msg:"..."}.
$tellraw @s {translate:"commands.message.display.incoming",with:[{selector:"@s"},{text:"$(msg)"}],color:"gray",italic:true}
