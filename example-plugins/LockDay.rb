# Locks all player's vision to mid-day 

Plugin.is {
    name "LockDay"
    version "0.2"
    author "Zeerix"
}

import 'org.bukkit.event.player.PlayerLoginEvent'

class LockDay < RubyPlugin
    def onEnable
        server.onlinePlayers.each {
            |player|
            lockTime player
        }

        registerEvent(PlayerLoginEvent, :Normal) {
            |login|
            lockTime login.player    
        }
        
        print "[" + description.name + "] " + description.fullName + " enabled."
    end
    
    def onDisable
        server.onlinePlayers.each {
            |player|
            player.resetPlayerTime
        }
    
        print "[" + description.name + "] " + description.fullName + " disabled."    
    end
    
    def lockTime(player)
        player.setPlayerTime(6000, false)    
    end
end
