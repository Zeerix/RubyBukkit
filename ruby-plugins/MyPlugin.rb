
import 'org.bukkit.event.Event'

class BlockListener < org.bukkit.event.block.BlockListener
    def onBlockBreak(event)
        block = event.getBlock
        x,y,z = block.getX, block.getY, block.getZ
    	item = event.getPlayer.getInventory.getItemInHand.getTypeId

	    printf "onBlockBreak: #{x},#{y},#{z}, item = #{item}"
    end
end

class MyPlugin < RubyPlugin
    def onEnable
        pm = getServer.getPluginManager
        pm.registerEvent(Event::Type::BLOCK_BREAK, BlockListener.new, Event::Priority::Normal, self)
    
        printf "MyPlugin enabled! yay!"
    end
    def onDisable
        printf "MyPlugin disabled! yay!"
    end
end

Name = "MyPlugin"
Version = "0.1"
Main = Name

