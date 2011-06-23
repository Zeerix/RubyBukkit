package org.notbukkit;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Ruby plugins for Bukkit
 *
 * @author Zeerix
 */
public class RubyBukkit extends JavaPlugin {
    
    // *** referenced classes ***
    
    final PluginManager pluginManager = Bukkit.getServer().getPluginManager();
    
    // *** data ***
    
    private PluginDescriptionFile pdfFile;
    private String logPrefix = "";

    private boolean loaderRegistered;
    
	// *** public interface ***
    
    public void onLoad() {
        pdfFile = this.getDescription();
        logPrefix = "[" + pdfFile.getName() + "] ";
    }
    
    public void onEnable() {
        infoLog("Version " + pdfFile.getVersion() + " enabled.");

        // register loader for Ruby plugins
        if (!loaderRegistered) {
            try {
                String packageName = getClass().getPackage().getName();
                File jrubyFile = new File(this.getDataFolder(), "jruby.jar");
                URL[] urls = new URL[]{ getFile().toURI().toURL(), jrubyFile.toURI().toURL() };
                ClassLoader jarLoader = new URLClassLoader(urls, PluginLoader.class.getClassLoader());
                Class<?> loaderClass = Class.forName(packageName+".RubyPluginLoader", true, jarLoader);
                Class<? extends PluginLoader> loader = loaderClass.asSubclass(PluginLoader.class);            
                pluginManager.registerInterface(loader);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }            
            loaderRegistered = true;
        }
        
        // load Ruby plugins        
        final File pluginsFolder = this.getFile().getParentFile();
        final File[] rubyFiles = pluginsFolder.listFiles( new FilenameFilter() {
           public boolean accept(File dir, String name) {
               return name.endsWith(".rb");
           }
        });
        
        if (rubyFiles.length > 0)
            infoLog("Loading Ruby plugins...");
        
        ArrayList<Plugin> plugins = new ArrayList<Plugin>();
        for (File rubyPlugin : rubyFiles) {
            try {
                infoLog(" - " + rubyPlugin.getName());
                Plugin plugin = pluginManager.loadPlugin(rubyPlugin);
                plugin.onLoad();
                plugins.add(plugin);
            } catch (Exception e) {
                e.printStackTrace(); 
            }
        }
        
        for (Plugin plugin : plugins) {
            try {
                plugin.getPluginLoader().enablePlugin(plugin);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public void onDisable() {
        infoLog("Version " + pdfFile.getVersion() + " disabled.");
    }
    
    private void infoLog(String msg) {
        System.out.println(logPrefix + msg);
    }
}