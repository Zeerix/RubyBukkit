# RubyBukkit's class RubyPlugin
# Imports and extends the Java RubyPlugin class
#
# @author Zeerix

import 'org.notbukkit.RubyPlugin'

class RubyPlugin
    # Ruby convenience methods: Registering events
    # Example:
    #    registerEvent(:PlayerLogin, :Normal) {
    #        |loginEvent|
    #        player = loginEvent.getPlayer    
    #    }
    
    def registerEvent(type, prio, &listener)
        type = type.to_s if type.is_a?(Symbol)
        prio = prio.to_s if prio.is_a?(Symbol)
        
        #type = matchToEnum(type, org.bukkit.event.Event::Type) if type.is_a?(String)
        prio = matchToEnum(prio, org.bukkit.event.EventPriority) if prio.is_a?(String)

        #type = type.getEventClass if type.respond_to?(:getEventClass)
        prio = prio.getNewPriority if prio.respond_to?(:getNewPriority)
        
        registerRubyBlock(type, prio, &listener)
    end
    
    # Helper methods
    
    def matchToEnum(str, enum)
        str = str.gsub('_', '').upcase
        enum.values.each { |elem|
            return elem if str == elem.name.gsub('_', '').upcase
        }
        raise StandardError, "Could not match '#{str}' to Enum #{enum}", caller
    end
end
