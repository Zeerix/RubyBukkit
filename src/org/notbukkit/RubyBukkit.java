package org.notbukkit;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * This is the Bukkit plugin that adds support for Ruby plugins
 *
 * @author Zeerix
 */
public class RubyBukkit extends JavaPlugin {
    
    // *** logging ***

    private final Level INFO = Level.INFO;
    private final Level SEVERE = Level.SEVERE;

    private Logger log;
    private String logPrefix = "";

    private void log(Level level, String msg, Throwable ex) {
        log.log(level, logPrefix + msg, ex);
    }
    private void log(Level level, String msg) {
        log.log(level, logPrefix + msg);
    }
    
    // *** plugin configuration ***
    
    protected static File thisJar;
    protected static File jrubyJar;
    protected static boolean debugInfo;
    protected static String rubyVersion;

    private File pluginsFolder;     // where to load plugins from
    
    // *** public interface ***
    
    public void onLoad() {
        // initialize logging
        log = Logger.getLogger(getDescription().getName());
        logPrefix = "[" + getDescription().getName() + "] ";

        // store jar path for plugin loader
        thisJar = this.getFile();
    }
    
    public void onEnable() {
        log(INFO, getDescription().getFullName() + " enabled.");
        
        loadConfig();
        
        // debug output
        if (debugInfo) {        
            log(INFO, "Ruby version set to " + rubyVersion);
        }

        // register runtime and loader for Ruby plugins
        if (!registerJRubyJar(jrubyJar)) {
            disableSelf();
            return;
        }
        
        registerPluginLoader();
        
        // enumerate Ruby plugin files
        final File[] rubyFiles = getPluginFiles(pluginsFolder);   // get *.rb in plugins/ folder
        
        // load & initialize plugins
        /*final Plugin[] plugins =*/ loadPlugins(rubyFiles);   
    }
    
    public void onDisable() {
        log(INFO, getDescription().getFullName() + " disabled.");
    }

    // *** configuration ***
    
    private void loadConfig() {
        // load configuration
        FileConfiguration config = getConfig();
        
        // set defaults
        config.options().copyDefaults(true);
        config.addDefault("runtime.ruby-version", "1.8");
        config.addDefault("runtime.jruby-path", getDataFolder() + File.separator + "jruby.jar");
        config.addDefault("settings.plugins-path", getDataFolder().getPath());
        config.addDefault("settings.debug", true);

        // Ruby version
        rubyVersion = config.getString("runtime.ruby-version");
        if (!rubyVersion.equals("1.8") && !rubyVersion.equals("1.9")) {
            log(SEVERE, "Invalid Ruby version \"" + rubyVersion + "\". Possible values are \"1.8\" and \"1.9\".");
            config.set("runtime.ruby-version", rubyVersion = "1.8");
        }
        
        // JRuby runtime
        jrubyJar = new File(config.getString("runtime.jruby-path"));
        
        // plugin search path        
        pluginsFolder = new File(config.getString("settings.plugins-path"));
        debugInfo = config.getBoolean("settings.debug");
        saveConfig();
    }
    
    // *** internals ***
    
    private void disableSelf() {
        getServer().getPluginManager().disablePlugin(this);
    }    
    
    private boolean registerJRubyJar(File jrubyFile) {
        try {
            // sanity checks
            if (!jrubyFile.exists()) {
                log(SEVERE, "JRuby runtime not found: " + jrubyFile.getPath());
                return false;
            }              
            
            URL jrubyURL = jrubyFile.toURI().toURL();
            
            URLClassLoader syscl = (URLClassLoader)ClassLoader.getSystemClassLoader();
            URL[] urls = syscl.getURLs();
            for (URL url : urls)
                if (url.sameFile(jrubyURL)) {
                    log(INFO, "Using present JRuby.jar from the classpath.");
                    return true;
                }

            // URLClassLoader.addUrl is protected
            Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{ URL.class });
            addURL.setAccessible(true);

            // add jruby.jar to Bukkit's class path
            addURL.invoke(syscl, new Object[]{ jrubyURL });
            
            log(INFO, "Using JRuby runtime " + jrubyFile.getPath());
            return true;            
        } catch (Exception e) {
            log(SEVERE, e.getMessage() + " while adding JRuby.jar to the classpath", e);
            return false;
        }
    }
    
    private void registerPluginLoader() {
        try {
            // register RubyPluginLoader in Bukkit
            getServer().getPluginManager().registerInterface(RubyPluginLoader.class);
        } catch (Exception e) {
            log(SEVERE, e.getMessage() + " while registering RubyPluginLoader", e);
        }
    }
    
    private File[] getPluginFiles(File folder) {
        FilenameFilter rbExtFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".rb");
            }
        };
        return folder.listFiles(rbExtFilter);
    }
    
    private Plugin[] loadPlugins(File[] files) {
        if (debugInfo) {        
            log(INFO, files.length == 0 ? "No Ruby plugins found." : "Loading Ruby plugins...");
        }
        
        ArrayList<Plugin> plugins = new ArrayList<Plugin>();
        for (File file : files) {
            try {
                if (debugInfo)
                    log(INFO, " - " + file.getName());
                Plugin plugin = getServer().getPluginManager().loadPlugin(file);
                if (plugin != null)
                    plugins.add(plugin);
                else if (debugInfo)
                    log(INFO, "   ! Could not load " + file.getName());
            } catch (Exception e) {
                log(SEVERE, "Error loading " + file.getName(), e); 
            }
        }
        
        for (Plugin plugin : plugins) {
            try {
                plugin.onLoad();
            } catch (Exception e) {
                log(SEVERE, "Error initializing " + plugin.getDescription().getFullName(), e); 
            }
        }
        
        return plugins.toArray(new Plugin[0]);
    }
}