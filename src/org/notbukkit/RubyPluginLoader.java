package org.notbukkit;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.jruby.CompatVersion;
import org.jruby.RubyInstanceConfig.CompileMode;
import org.jruby.embed.EmbedEvalUnit;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

public final class RubyPluginLoader implements PluginLoader {
    
    // *** referenced classes ***
    
    private final JavaPluginLoader javaPluginLoader;    // for createExecutor
    private final Server server;

    // *** data ***
    // Embedding: http://kenai.com/projects/jruby/pages/DirectJRubyEmbedding
    // YAML & co: http://kenai.com/projects/jruby/pages/AccessingJRubyObjectInJava
    
    private final Pattern[] fileFilters = new Pattern[] {
        Pattern.compile("\\.rb$"),
    };

    private final String packageName = getClass().getPackage().getName();

    // pre-script to make it easier for plugin authors
    private final String preScript =
        "require 'java'\n" +
        "import '" + packageName + ".RubyPlugin'\n";
    
    // Ruby script to check and create an instance of the main plugin class 
    private final String instanceScript =
        "if defined?(className) == 'constant' and className.class == Class then\n" +
        "    className.new\n" +
        "else\n" +
        "    raise 'main class not defined: className'\n" +
        "end\n";
    
    // *** interface ***
    
    public RubyPluginLoader(Server instance) {
        server = instance;
        javaPluginLoader = new JavaPluginLoader(server);
    }
    
    public Plugin loadPlugin(File file) throws InvalidPluginException, InvalidDescriptionException, UnknownDependencyException {
        return loadPlugin(file, false);
    }
    
    public Plugin loadPlugin(File file, boolean ignoreSoftDependencies) throws InvalidPluginException, InvalidDescriptionException, UnknownDependencyException {
        if (!file.exists()) {
            throw new InvalidPluginException(new FileNotFoundException(String.format("%s does not exist", file.getPath())));
        }     
        
        // create a scripting container for every plugin to encapsulate it
        ScriptingContainer runtime = new ScriptingContainer(LocalContextScope.CONCURRENT);
        runtime.setClassLoader(runtime.getClass().getClassLoader());
        runtime.setCompileMode(CompileMode.JIT);
        //runtime.setHomeDirectory( "/path/to/home/" );
        
        if (RubyBukkit.rubyVersion.equals("1.9"))
            runtime.setCompatVersion(CompatVersion.RUBY1_9);
        
        try {
            // parse and run script
            runtime.runScriptlet(preScript);
            
            final EmbedEvalUnit eval = runtime.parse(PathType.RELATIVE, file.getPath());
            /*IRubyObject res =*/ eval.run();
    
            // create plugin description
            final PluginDescriptionFile description = getDescriptionFile(runtime);
            final File dataFolder = new File(file.getParentFile(), description.getName());
        
            // create main plugin class
            final String script = instanceScript.replaceAll("className", description.getMain());
            
            final RubyPlugin plugin = (RubyPlugin)runtime.runScriptlet(script);     // create instance of main class
            plugin.initialize(this, server, description, dataFolder, file, runtime);
            return plugin;
        } catch (Throwable ex) {
            throw new InvalidPluginException(ex);
        }
    }
    
    /**
     * extract description from Ruby script 
     */
    private PluginDescriptionFile getDescriptionFile(ScriptingContainer runtime) throws InvalidDescriptionException {
        String name;
        try {
            name = runtime.get("Name").toString();
        } catch (Exception e) {
            throw new InvalidDescriptionException(e, "name is not defined");
        }
        if (!name.matches("^[A-Za-z0-9 _.-]+$")) {
            throw new InvalidDescriptionException("name '" + name + "' contains invalid characters.");        
        }
        
        String version;
        try {
            version = runtime.get("Version").toString();
        } catch (Exception e) {
            throw new InvalidDescriptionException(e, "version is not defined");
        }            

        String main;
        try {
            main = runtime.get("Main").toString();
        } catch (Exception e) {
            throw new InvalidDescriptionException(e, "main is not defined");
        }
        
        PluginDescriptionFile description = new PluginDescriptionFile(name, version, main);
        return description;
    }

    public Pattern[] getPluginFileFilters() {
        return fileFilters;
    }    

    public EventExecutor createExecutor(Event.Type type, Listener listener) {
        return javaPluginLoader.createExecutor(type, listener);
    }

    public void enablePlugin(Plugin plugin) {
        if (!(plugin instanceof RubyPlugin)) {
            throw new IllegalArgumentException("Plugin is not associated with this PluginLoader");
        }
        
        if (!plugin.isEnabled()) {
            try {
                RubyPlugin rPlugin = (RubyPlugin)plugin;
                rPlugin.setEnabled(true);
            } catch (Throwable ex) {
                server.getLogger().log(Level.SEVERE, "Error occurred while enabling " + plugin.getDescription().getFullName() + " (Is it up to date?): " + ex.getMessage(), ex);
            }
            
            // Perhaps abort here, rather than continue going, but as it stands,
            // an abort is not possible the way it's currently written
            server.getPluginManager().callEvent(new PluginEnableEvent(plugin));
        }
    }
    
    public void disablePlugin(Plugin plugin) {
        if (!(plugin instanceof RubyPlugin)) {
            throw new IllegalArgumentException("Plugin is not associated with this PluginLoader");
        }
        
        if (plugin.isEnabled()) {
            try {
                RubyPlugin rPlugin = (RubyPlugin)plugin;
                rPlugin.setEnabled(false);
            } catch (Throwable ex) {
                server.getLogger().log(Level.SEVERE, "Error occurred while disabling " + plugin.getDescription().getFullName() + ": " + ex.getMessage(), ex);
            }
            
            // Perhaps abort here, rather than continue going, but as it stands,
            // an abort is not possible the way it's currently written
            server.getPluginManager().callEvent(new PluginDisableEvent(plugin));
        }
    }    
}
