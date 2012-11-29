# RubyBukkit plugin instance script.
# Executed to create an instance of the plugin class
#
# This is loaded directly from the RubyBukkit.jar
#
# @author Zeerix

# raise error if :main is not defined
if not Plugin.getDescription.key?(:main) then
    raise StandardError, "Plugin.main is not defined", caller
end     

begin
    PluginMain = Plugin.getDescription[:main]
    eval(PluginMain).new
rescue
    raise StandardError, "Could not instantiate Plugin class: #{PluginMain.inspect}", caller
end
