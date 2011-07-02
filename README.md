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

The Ruby script must call 'Plugin.is' with a block that calls the methods
'name' and 'version' with appropriate string values, similar to how plugin.yml
works for Java plugins. The value 'main', which defaults to the same as 'name',
must be the name of the main plugin class.

The main class must inherit from RubyPlugin.

Example:

    Plugin.is {
        name "ThePlugin"
        version "0.1"
        author "TheAuthor"
    }
    
    class ThePlugin < RubyPlugin
        def onEnable; print "ThePlugin enabled."; end
        def onDisable; print "ThePlugin disabled."; end
    end
