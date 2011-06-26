# plugin.yml constants
Name = "OnlinePlayers"
Version = "0.2"
Author = "Zeerix"
Description = "Displays list of online players on login and by a command"
Main = Name
Commands = {
    :list => {
        :usage => "/<command> - displays list of online players",
        :aliases => [ :players, :online ]
    }
}

# imports
import 'org.bukkit.event.Event'

# plugin class
class OnlinePlayers < RubyPlugin
    def onEnable
        registerEvents
        printf "OnlinePlayers enabled."
    end
    def onDisable
        printf "OnlinePlayers disabled."
    end
    
    def registerEvents
        registerEvent(Event::Type::PLAYER_LOGIN, Event::Priority::Normal) {
            |loginEvent|
            player = loginEvent.getPlayer    
            scheduleSyncTask { listPlayersTo player }
        }
    end
    
    def onCommand(sender, command, label, args)
        listPlayersTo sender
        true
    end
    
    def listPlayersTo(player)
        players = getServer.getOnlinePlayers
        if players.size == 0 then
            msg = "No players online."
        else
            msg = "Players: " + players.map{|player| player.name }.join(", ")
        end
        player.sendMessage msg
    end
end
