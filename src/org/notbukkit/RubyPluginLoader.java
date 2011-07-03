package org.notbukkit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.jruby.RubySymbol;
import org.jruby.embed.EmbedEvalUnit;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Loader for Ruby plugins; will be registered in PluginManager.
 *
 * Info about JRuby:
 *  - Embedding: http://kenai.com/projects/jruby/pages/DirectJRubyEmbedding
 *  - YAML & co: http://kenai.com/projects/jruby/pages/AccessingJRubyObjectInJava
 *
 * @author Zeerix
 */
public final class RubyPluginLoader implements PluginLoader {
    
    // *** referenced classes ***
    
    private final JavaPluginLoader javaPluginLoader;    // for createExecutor
    private final Server server;

    // *** data ***
    
    private final Pattern[] fileFilters = new Pattern[] {
        Pattern.compile("\\.rb$"),
    };

    // script files
    private final String initScript = "/rubybukkit/init-plugin.rb"; 
    private final String createScript = "/rubybukkit/new-plugin.rb"; 
    
    // *** interface ***
    
    public RubyPluginLoader(Server server) {
        this.server = server;
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
        ScriptingContainer runtime = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
        runtime.setClassLoader(runtime.getClass().getClassLoader());
        runtime.setHomeDirectory( RubyBukkit.jrubyFile.getAbsoluteFile().getParent() );
        String[] loadPaths = new String[] { file.getAbsoluteFile().getParent(), RubyBukkit.thisJar.getAbsolutePath() };
        runtime.setLoadPaths(Arrays.asList(loadPaths));
        
        if (RubyBukkit.rubyVersion.equals("1.9"))
            runtime.setCompatVersion(CompatVersion.RUBY1_9);
        
        try {
            // run init script
            runResourceScript(runtime, initScript);
            
            final EmbedEvalUnit eval = runtime.parse(PathType.RELATIVE, file.getPath());
            /*IRubyObject res =*/ eval.run();
    
            // create plugin description
            final PluginDescriptionFile description = getDescriptionFile(runtime);
            if (description == null)
                return null;    // silent fail if the script doesn't contain a plugin description
            
            final File dataFolder = new File(file.getParentFile(), description.getName());
            
            // create instance of main class
            RubyPlugin plugin = (RubyPlugin)runResourceScript(runtime, createScript);
            plugin.initialize(this, server, description, dataFolder, file, runtime);
            return plugin;
        } catch (InvalidDescriptionException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidPluginException(e);
        }
    }
    
    /**
     * execute Ruby script from resource (embedded in .jar) 
     */
    private Object runResourceScript(ScriptingContainer runtime, String filename) throws IOException {
        InputStream script = getClass().getResourceAsStream(filename);
        if (script == null)
            throw new FileNotFoundException(filename);
        try {
            return runtime.runScriptlet(script, filename);
        } finally {
            script.close();
        }        
    }
    
    /**
     * extract description from Ruby script
     * "Plugin.getDescription" will return our map 
     */
    private PluginDescriptionFile getDescriptionFile(ScriptingContainer runtime) throws InvalidDescriptionException {
        Object desc = convertFromRuby(runtime.runScriptlet("Plugin.getDescription"));
        if (desc instanceof Map) {
            final Yaml yaml = new Yaml(new SafeConstructor());
            final StringReader reader = new StringReader( yaml.dump(desc) );
            return new PluginDescriptionFile(reader);
        }
        return null;
    }
    
    private static Object convertFromRuby(Object object) throws InvalidDescriptionException {
        if (object == null || object instanceof String)
            return object;
        if (object instanceof Boolean)
            return (boolean)(Boolean)object;
        if (object instanceof Long)
            return (long)(Long)object;
        if (object instanceof Double)
            return (double)(Double)object;
        if (object instanceof RubySymbol)
            return convertFromRuby( ((RubySymbol)object).asJavaString() );
        if (object instanceof List)
            return convertFromRuby( (List<Object>)object );
        if (object instanceof Map)
            return convertFromRuby( (Map<Object, Object>)object );
        
        throw new InvalidDescriptionException("Unknown Ruby object: " + object.getClass().getName());
    }

    private static Object convertFromRuby(Map<Object, Object> map) throws InvalidDescriptionException {
        Map<Object, Object> result = new HashMap<Object, Object>();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            Object key = convertFromRuby(entry.getKey());
            Object value = convertFromRuby(entry.getValue());
            result.put(key, value);
        }        
        return result;
    }  
    
    private static Object convertFromRuby(List<Object> list) throws InvalidDescriptionException {
        List<Object> result = new ArrayList<Object>();
        for (Object entry : list) {
            result.add(convertFromRuby(entry));
        }        
        return result;
    }   
    
    public Pattern[] getPluginFileFilters() {
        return fileFilters;
    }    

    public EventExecutor createExecutor(Event.Type type, Listener listener) {
        return javaPluginLoader.createExecutor(type, listener);     // delegate
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
