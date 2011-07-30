# RubyBukkit's Plugin module
# Defines the Plugin.is{} block as replacement for plugin.yml file
#
# @author Zeerix

module Plugin
    def self.yml(&block)
        @desc = Description.new
        @desc.instance_eval &block
    end
    def self.is(&block); yml(&block); end
    def self.getDescription; @desc.data if @desc != nil; end    
    
    class Description
        attr_reader :data
        def initialize; @data = {}; end
        
        def name(value)
            @data[:name] = value
            @data[:main] = value if not @data.key?(:main)
        end
        
        def method_missing(name, *args)
            @data[name] = (args.size == 1 ? args[0] : args)
        end
    end
end
