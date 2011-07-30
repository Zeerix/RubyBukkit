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
        
        type = matchToEnum(type, Event::Type) if type.is_a?(String)
        prio = matchToEnum(prio, Event::Priority) if prio.is_a?(String)

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
