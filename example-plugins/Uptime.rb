# Display time since server start or reload 

Plugin.is {
    name "Uptime"
    version "0.3"
    author "Zeerix"
    commands :uptime => {
        :usage => "/uptime - display time since server start or reload"
    }
    permissions "rubybukkit.uptime" => {
        :default => :op,
        :description => "Allows use of /uptime command"
    }
}

# plugin class
class Uptime < RubyPlugin
    def onEnable
        @serverStart = time
        print "[" + description.name + "] " + description.fullName + " enabled."
    end
    
    def onDisable; print uptimeString; end
    
    def onCommand(sender, command, label, args)
        if sender.hasPermission("rubybukkit.uptime") then
            sender.sendMessage uptimeString
        else
            sender.sendMessage "You don't have the permissions"
        end
        true
    end

    def time; java.lang.System.currentTimeMillis / 1000; end     
    def uptimeString
        diff = time - @serverStart
        "Uptime: #{diff / 86400}d #{diff / 3600 % 24}h #{diff / 60 % 60}m #{diff % 60}s"
    end     
end
