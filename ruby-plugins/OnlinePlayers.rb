# plugin.yml constants
Name = "OnlinePlayers"
Version = "0.1"
Author = "Zeerix"
Description = "Displays list of online players on login and by a command"
Main = Name
Commands = {
    :list => {
        :usage => "/list - displays list of online players",
        :aliases => [ :online ]
    }
}

# imports
import 'org.bukkit.event.Event'

# listeners
class PlayerListener < org.bukkit.event.player.PlayerListener
    def plugin=(plugin)
        @plugin = plugin
    end    
    def onPlayerLogin(event)
        task = ListPlayers.new @plugin, event.getPlayer
        @plugin.getServer.getScheduler.scheduleSyncDelayedTask @plugin, task 
    end
end

# scheduler task
class ListPlayers
    include java.lang.Runnable
    def initialize(plugin, player)
        @plugin, @player = plugin, player
    end
    def run()
        players = @plugin.getServer.getOnlinePlayers
        @player.sendMessage "Players: " + players.map{|player| player.name }.join(", ")
    end     
end

# actual plugin class
class OnlinePlayers < RubyPlugin
    def onEnable
        listener = PlayerListener.new
        listener.plugin = self
        pm = getServer.getPluginManager
        pm.registerEvent(Event::Type::PLAYER_LOGIN, listener, Event::Priority::Normal, self)
    
        printf "OnlinePlayers enabled!"
    end
    def onDisable
        printf "OnlinePlayers disabled!"
    end
    
    def onCommand(sender, command, label, args)
        ListPlayers.new(self, sender).run
        true
    end
end
