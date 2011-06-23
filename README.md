RubyBukkit
==========

A Bukkit binding for JRuby plugins

Compilation
-----------

Compile against bukkit.jar and jruby.jar

Installation
------------

Put the RubyBukkit.jar file into the server's plugins/ folder.
Put jruby.jar into plugins/RubyBukkit/.

You get jruby.jar from http://jruby.org/ in the downloads section.
Download the release package and extract lib/jruby.jar.

Plugins
-------

Any files in plugins/ with the extension .rb will be loaded as Ruby plugin.
The Ruby script must define the three constants Name, Main and Version.
'Name' is the name of the plugin, while 'Main' is the name of the main plugin class.
The main class must inherit from RubyPlugin.

Example:
    class ThePlugin < RubyPlugin
        def onEnable; printf "ThePlugin enabled."; end
        def onDisable; printf "ThePlugin disabled."; end
    end
    
    Name = "ThePlugin"
    Main = "ThePlugin"
    Version = "0.1"
