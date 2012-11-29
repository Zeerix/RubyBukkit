package org.notbukkit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.apache.commons.lang.Validate;
import org.bukkit.Server;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.*;
import org.jruby.CompatVersion;
import org.jruby.RubyInstanceConfig.CompileMode;
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
    }

    @Override
    public Plugin loadPlugin(File file) throws InvalidPluginException, UnknownDependencyException {
        return loadPlugin(file, false);
    }

    public Plugin loadPlugin(File file, boolean ignoreSoftDependencies) throws InvalidPluginException, UnknownDependencyException {
        Validate.notNull(file, "File cannot be null");
        if (!file.exists()) {
            throw new InvalidPluginException(new FileNotFoundException(String.format("%s does not exist", file.getPath())));
        }

        // create a scripting container for every plugin to encapsulate it
        ScriptingContainer runtime = setupScriptingContainer(file);
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
        } catch (Exception e) {
            throw new InvalidPluginException(e);
        }
    }

    /**
     * create and setup ScriptingContainer
     */
    private ScriptingContainer setupScriptingContainer(File file) {
        ScriptingContainer runtime = new ScriptingContainer(LocalContextScope.SINGLETHREAD);

        runtime.setCompileMode(CompileMode.JIT);

        runtime.setClassLoader(RubyPluginLoader.class.getClassLoader());

        // Setup load paths, the "file:" thing is for internal stuff like rubygems
        String[] loadPaths = new String[] {
            file.getAbsoluteFile().getParent(),
            RubyBukkit.thisJar.getAbsolutePath(),
            "file:" + RubyBukkit.jrubyJar.getAbsoluteFile() + "!/META_INF/jruby.home/lib/ruby/site_ruby/" + RubyBukkit.rubyVersion,
            "file:" + RubyBukkit.jrubyJar.getAbsoluteFile() + "!/META_INF/jruby.home/lib/ruby/site_ruby/shared",
            "file:" + RubyBukkit.jrubyJar.getAbsoluteFile() + "!/META_INF/jruby.home/lib/ruby/" + RubyBukkit.rubyVersion,
        };
        runtime.setLoadPaths(Arrays.asList(loadPaths));

        if (RubyBukkit.rubyVersion.equals("1.9"))
            runtime.setCompatVersion(CompatVersion.RUBY1_9);

        return runtime;
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

    @Override
    public PluginDescriptionFile getPluginDescription(File file) throws InvalidDescriptionException {
        Validate.notNull(file, "File cannot be null");
        if (!file.exists()) {
            throw new InvalidDescriptionException(new FileNotFoundException(String.format("%s does not exist", file.getPath())));
        }

        ScriptingContainer runtime = setupScriptingContainer(file);
        try {
            // run init script
            runResourceScript(runtime, initScript);

            final EmbedEvalUnit eval = runtime.parse(PathType.RELATIVE, file.getPath());
            /*IRubyObject res =*/ eval.run();

            return getDescriptionFile(runtime);

        } catch (IOException e) {
            throw new InvalidDescriptionException(e);
        }
    }

    /**
     * extract description from Ruby script
     * "Plugin.getDescription" will return our map
     */
    private PluginDescriptionFile getDescriptionFile(ScriptingContainer runtime) throws InvalidDescriptionException {
        Object desc = convertFromRuby(runtime.runScriptlet("Plugin.getDescription"));
        if (!(desc instanceof Map)) {
            throw new InvalidDescriptionException("Plugin.getDescription must return a Map");
        }

        final Yaml yaml = new Yaml(new SafeConstructor());
        final StringReader reader = new StringReader( yaml.dump(desc) );
        return new PluginDescriptionFile(reader);
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

    @Override
    public Pattern[] getPluginFileFilters() {
        return fileFilters;
    }

    @Override
    public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(Listener listener, Plugin plugin) {
        return Collections.emptyMap();  // no annotation based registering for Ruby plugins yet
    }

    @Override
    public void enablePlugin(Plugin plugin) {
        Validate.isTrue(plugin instanceof RubyPlugin, "Plugin is not associated with this PluginLoader");

        if (!plugin.isEnabled()) {
            String message = String.format("[%s] Loading %s.", plugin.getDescription().getName(), plugin.getDescription().getFullName());
            server.getLogger().info(message);

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

    @Override
    public void disablePlugin(Plugin plugin) {
        Validate.isTrue(plugin instanceof RubyPlugin, "Plugin is not associated with this PluginLoader");

        if (plugin.isEnabled()) {
            String message = String.format("[%s] Unloading %s.", plugin.getDescription().getName(), plugin.getDescription().getFullName());
            server.getLogger().info(message);

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
