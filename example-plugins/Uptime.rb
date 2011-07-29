# Display time since server start or reload 

Plugin.is {
    name "Uptime"
    version "0.2"
    author "Zeerix"
    commands :uptime => {
        :usage => "/uptime - display time since server start or reload"
    }
}

# use permission provider
require 'lib/permissions'

# plugin class
class Uptime < RubyPlugin
    def onEnable
        @serverStart = time
        print "[" + description.name + "] " + description.fullName + " enabled."
    end
    
    def onDisable; print uptimeString; end
    
    def onCommand(sender, command, label, args)
        if !sender.isPlayer || sender.has("rubybukkit.uptime") then
            sender.sendMessage uptimeString
        end
        true
    end

    def time; java.lang.System.currentTimeMillis / 1000; end     
    def uptimeString
		diff = time - @serverStart
		"Uptime: #{diff / 86400}d #{diff / 3600 % 24}h #{diff / 60 % 60}m #{diff % 60}s"
    end     
end
