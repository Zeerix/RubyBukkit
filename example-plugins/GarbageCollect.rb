# Simple utility plugin to force a garbage collection cycle

Plugin.is {
    name "GarbageCollect"
    version "0.2"
    author "Zeerix"
    commands :gc => {
        :description => "garbage collect!",
        :usage => "/gc"
    }
}

# plugin class
class GarbageCollect < RubyPlugin
    def onEnable
        print getDescription.getFullName + " enabled."
    end
    
    def onCommand(sender, command, label, args)
        if sender.isPlayer then
            sender.sendMessage "Only console!"
            return true
        end
        
        sender.sendMessage "Collect!"
        java.lang.System::gc
        true
    end 
end
