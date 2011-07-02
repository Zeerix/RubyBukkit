package org.notbukkit;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.util.config.Configuration;

/**
 * Loads Ruby plugins for Bukkit
 *
 * @author Zeerix
 */
public class RubyBukkit extends JavaPlugin {
    
    // *** data ***
    
    private Logger log;
    private String logPrefix = "";

    private boolean loaderRegistered;
    
    // *** plugin configuration ***
    
    protected static File thisJar;
    protected static File jrubyFile;
    protected static boolean debugInfo;
    protected static String rubyVersion;

    // *** public interface ***
    
    public void onLoad() {
        log = Logger.getLogger(getDescription().getName());
        logPrefix = "[" + getDescription().getName() + "] ";
    }
    
    public void onEnable() {
        logInfo(getDescription().getFullName() + " enabled.");
        
        // store jar path for plugin loader
        thisJar = this.getFile();

        // load configuration
        Configuration config = getConfiguration(); 
        config.load();
        jrubyFile = new File(config.getString("runtime.jruby-path", getDataFolder() + File.separator + "jruby.jar"));
        rubyVersion = config.getString("runtime.ruby-version", "1.8");
        
        File pluginsFolder = new File(config.getString("settings.plugins-path", getFile().getParent()));
        debugInfo = config.getBoolean("settings.debug", true);
        config.save();
        
        // sanity checks
        if (!jrubyFile.exists()) {
            logSevere("JRuby runtime not found: " + jrubyFile.getPath());
            return;
        }        
        if (!rubyVersion.equals("1.8") && !rubyVersion.equals("1.9")) {
            logSevere("Invalid Ruby version \"" + rubyVersion + "\". Possible values are \"1.8\" and \"1.9\".");
            return;
        }
        
        if (debugInfo) {        
            logInfo("Using JRuby runtime " + jrubyFile.getPath());
            logInfo("Ruby version set to " + rubyVersion);
        }
        
        // register loader for Ruby plugins
        registerPluginLoader(jrubyFile);
        
        // enumerate Ruby plugin files
        final File[] rubyFiles = getPluginFiles(pluginsFolder);   // get *.rb in plugins/ folder
        
        // load & initialize plugins
        /*final Plugin[] plugins =*/ loadPlugins(rubyFiles);   
    }
    
    public void onDisable() {
        logInfo(getDescription().getFullName() + " disabled.");
    }
    
    // *** internals ***

    private void doLog(Level level, String msg, Throwable ex) {
        log.log(level, logPrefix + msg, ex);
    }
    private void doLog(Level level, String msg) {
        log.log(level, logPrefix + msg);
    }
    
    private void logInfo(String msg) { doLog(Level.INFO, msg); }    
    private void logSevere(String msg) { doLog(Level.SEVERE, msg); }    
    private void logSevere(String msg, Throwable e) { doLog(Level.SEVERE, msg, e); }    
    
    private void registerPluginLoader(File jrubyFile) {
        if (!loaderRegistered) {
            try {
                // load jruby.jar as pseudo-plugin, so all plugins and ruby-script can access its classes
                JavaPluginLoader pluginLoader = (JavaPluginLoader)getPluginLoader();
                
                JRubyPackage jrubyPackage = new JRubyPackage(pluginLoader, jrubyFile, getServer(), getClassLoader().getParent());
                pluginLoader.enablePlugin(jrubyPackage);
                
                // register RubyPluginLoader in Bukkit
                getServer().getPluginManager().registerInterface(RubyPluginLoader.class);
                loaderRegistered = true;
            } catch (Exception e) {
                logSevere(e.getMessage() + " registering RubyPluginLoader", e);
            }
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
            logInfo(files.length == 0 ? "No Ruby plugins found." : "Loading Ruby plugins...");
        }
        
        ArrayList<Plugin> plugins = new ArrayList<Plugin>();
        for (File file : files) {
            try {
                if (debugInfo)
                    logInfo(" - " + file.getName());
                Plugin plugin = getServer().getPluginManager().loadPlugin(file);
                if (plugin != null)
                    plugins.add(plugin);
                else if (debugInfo)
                    logInfo("   ! Could not load " + file.getName());
            } catch (Exception e) {
                logSevere("Error loading " + file.getName(), e); 
            }
        }
        
        for (Plugin plugin : plugins) {
            try {
                plugin.onLoad();
            } catch (Exception e) {
                logSevere("Error initializing " + plugin.getDescription().getFullName(), e); 
            }
        }
        
        return plugins.toArray(new Plugin[0]);
    }
}