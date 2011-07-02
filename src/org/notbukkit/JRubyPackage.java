package org.notbukkit;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.bukkit.Server;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.plugin.java.PluginClassLoader;

/**
 * This is an artificial plugin that represents the jruby.jar package
 * 
 * @author Zeerix
 */
public final class JRubyPackage extends JavaPlugin {
    protected JRubyPackage(JavaPluginLoader loader, File jrubyFile, Server server, ClassLoader systemLoader) throws MalformedURLException {
        URL[] urls = new URL[]{ jrubyFile.toURI().toURL() };
        PluginClassLoader jrubyClassLoader = new PluginClassLoader(loader, urls, systemLoader);
        
        PluginDescriptionFile description = new PluginDescriptionFile("JRubyPackage", "1.0", "JRubyPackage");
        initialize(loader, server, description, null, null, jrubyClassLoader);
    }
    
    public void onEnable() {}

    public void onDisable() {}
}
