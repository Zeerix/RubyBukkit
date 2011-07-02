# Example plugin to show event handling and commands

Plugin.is {
    name "OnlinePlayers"
    version "0.3"
    author "Zeerix"
    description "Displays list of online players on login and by a command"
    commands :list => {
        :description => "Show online players.",
        :usage => "/list",
        :aliases => [ :players, :online ]
    }
}

# imports
import 'org.bukkit.event.Event'

# plugin class
class OnlinePlayers < RubyPlugin
    def onEnable
        registerEvents
        print getDescription.getFullName + " enabled."
    end
    def onDisable
        print getDescription.getFullName + " disabled."
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
    
    def listPlayersTo(sender)
        players = getServer.getOnlinePlayers
        if players.size == 0 then
            msg = "No players online."
        else
            msg = "Players: " + players.map{|player| player.name }.join(", ")
        end
        sender.sendMessage msg
    end
end
