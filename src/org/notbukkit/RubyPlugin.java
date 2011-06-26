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

public class RubyPlugin implements Plugin {
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
    
    protected final void initialize(PluginLoader loader, Server server, PluginDescriptionFile description,
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
    
    protected final ScriptingContainer getRuntime() {
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
    
    protected File getFile() {
        return pluginFile;
    }

    public PluginDescriptionFile getDescription() {
        return description;
    }

    public final PluginLoader getPluginLoader() {
        return loader;
    }

    public final Server getServer() {
        return server;
    }
    
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        getServer().getLogger().severe("Plugin " + getDescription().getFullName() + " does not contain any generators that may be used in the default world!");
        return null;
    }
    
    public final boolean isNaggable() {
        return naggable;
    }
    
    public final void setNaggable(boolean canNag) {
        naggable = canNag;
    }

    protected final void setEnabled(final boolean enabled) {
        if (isEnabled != enabled) {
            isEnabled = enabled;

            if (isEnabled) {
                onEnable();
            } else {
                onDisable();
            }
        }
    }
    
    public final boolean isEnabled() {
        return isEnabled;
    }

    public void onLoad() {
    }

    public void onEnable() {
    }
        
    public void onDisable() {
    }
    
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }

}
