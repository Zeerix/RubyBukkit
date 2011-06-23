package org.notbukkit;

import java.io.File;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.util.config.Configuration;
import org.jruby.embed.ScriptingContainer;

import com.avaje.ebean.EbeanServer;

public /*abstract*/ class RubyPlugin implements Plugin {
    private boolean isEnabled = false;
    private boolean initialized = false;
    private PluginLoader loader;
    private Server server;
    PluginDescriptionFile description;
    private File pluginFile;
    private File dataFolder;
    private ScriptingContainer runtime;
    private Configuration config;
    private boolean naggable = true;
    
    public RubyPlugin() {}
    
    protected void initialize(PluginLoader loader, Server server, PluginDescriptionFile description,
            File dataFolder, File file, ScriptingContainer runtime) {
        if (!initialized) {
            this.initialized = true;
            this.loader = loader;
            this.server = server;
            this.description = description;
            this.dataFolder = dataFolder;
            this.pluginFile = file;
            this.runtime = runtime;
        }
    }
    
    protected ScriptingContainer getRuntime() {
        return runtime;
    }    
    
    public Configuration getConfiguration() {
        return config;
    }

    public File getDataFolder() {
        return dataFolder;
    }

    public EbeanServer getDatabase() {
        return null;
    }

    public PluginDescriptionFile getDescription() {
        return description;
    }

    public PluginLoader getPluginLoader() {
        return loader;
    }

    public Server getServer() {
        return server;
    }
    
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        getServer().getLogger().severe("Plugin " + getDescription().getFullName() + " does not contain any generators that may be used in the default world!");
        return null;
    }
    
    public boolean isNaggable() {
        return naggable;
    }
    
    public void setNaggable(boolean canNag) {
        naggable = canNag;
    }

    protected void setEnabled(final boolean enabled) {
        if (isEnabled != enabled) {
            isEnabled = enabled;

            if (isEnabled) {
                onEnable();
            } else {
                onDisable();
            }
        }
    }
    
    public boolean isEnabled() {
        return isEnabled;
    }

    public void onDisable() {
    }

    public void onEnable() {
    }

    public void onLoad() {
    }
    
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }

}
