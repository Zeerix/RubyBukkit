package org.notbukkit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Type;
import org.bukkit.event.Listener;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.util.config.Configuration;
import org.jruby.embed.ScriptingContainer;

import com.avaje.ebean.EbeanServer;

/*
 * Ruby plugins inherit from that class.
 */
public class RubyPlugin implements Plugin {
    private boolean isEnabled = false;
    private boolean initialized = false;
    private PluginLoader loader;
    private Server server;
    private File pluginFile;
    PluginDescriptionFile description;
    private File dataFolder;
    private Configuration config;
    private boolean naggable = true;
    private FileConfiguration newConfig;
    private File configFile;
    private long[] timings = new long[Event.Type.values().length];

    private ScriptingContainer runtime;

    public RubyPlugin() {}

    protected final void initialize(PluginLoader loader, Server server, PluginDescriptionFile description,
            File dataFolder, File pluginFile, ScriptingContainer runtime) {
        if (!initialized) {
            this.initialized = true;
            this.loader = loader;
            this.server = server;
            this.pluginFile = pluginFile;
            this.description = description;
            this.dataFolder = dataFolder;
            this.configFile = new File(dataFolder, "config.yml");

            this.runtime = runtime;
        }
    }

    protected final ScriptingContainer getRuntime() {
        return runtime;
    }

    public Configuration getConfiguration() {
        if (config == null) {
            config = new Configuration(configFile);
            config.load();
        }
        return config;
    }

    public FileConfiguration getConfig() {
        if (newConfig == null) {
            reloadConfig();
        }
        return newConfig;
    }

    public void reloadConfig() {
        newConfig = YamlConfiguration.loadConfiguration(configFile);
    }

    public void saveConfig() {
        try {
            newConfig.save(configFile);
        } catch (IOException ex) {
            Logger.getLogger(RubyPlugin.class.getName()).log(Level.SEVERE, "Could not save config to " + configFile, ex);
        }
    }

    public void saveDefaultConfig() {
        try {
            getConfig().save(configFile);
        } catch (IOException ex) {
            Logger.getLogger(RubyPlugin.class.getName()).log(Level.SEVERE, "Could not save config to " + configFile, ex);
        }
    }

    public void saveResource(String resourcePath, boolean replace) {
        throw new java.lang.AbstractMethodError(RubyPlugin.class.getName() + ".saveResource is not implemented.");

    }

    public InputStream getResource(String filename) {
        throw new java.lang.AbstractMethodError(RubyPlugin.class.getName() + ".getResource is not implemented.");
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

    // *** new timing methods ***

    public long getTiming(Event.Type type) {
        return timings[type.ordinal()];
    }

    public void incTiming(Event.Type type, long delta) {
        timings[type.ordinal()] += delta;
    }

    public void resetTimings() {
        timings = new long[timings.length];
    }

    // *** callback methods for the implementation plugin ***

    public void onLoad() {
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }

    /*
     * Ruby convenience methods: Registering events
     *
     * Ruby example:
     *    registerEvent(Event::Type::PLAYER_LOGIN, Event::Priority::Normal) {
     *       |loginEvent|
     *       player = loginEvent.getPlayer
     *       scheduleSyncTask { listPlayersTo player }
     *    }
     *
     * @listener: An instance of RubyListener or a Ruby Proc
     */

    public interface RubyListener extends Listener {
        void onEvent(Event event);
    }
    private final class RubyExecutor implements EventExecutor {
        public void execute(Listener listener, Event event) {
            ((RubyListener) listener).onEvent(event);
        }
    }

    protected void registerRubyBlock(Event.Type type, Event.Priority priority, RubyListener listener) {
        getServer().getPluginManager().registerEvent(type, listener, new RubyExecutor(), priority, this);
    }

    /*
     * Ruby convenience methods: Scheduler
     * These methods call the corresponding methods in the Bukkit scheduler
     *
     * Ruby example:
     *    plugin.scheduleSyncRepeatingTask(delay, period) { print "Hallo Welt!" }
     *
     * @delay, period: Time units are server ticks
     */

    public int scheduleSyncTask(Runnable task) {
        return getServer().getScheduler().scheduleSyncDelayedTask(this, task);
    }
    public int scheduleSyncDelayedTask(long delay, Runnable task) {
        return getServer().getScheduler().scheduleSyncDelayedTask(this, task, delay);
    }
    public int scheduleSyncRepeatingTask(long delay, long period, Runnable task) {
        return getServer().getScheduler().scheduleSyncRepeatingTask(this, task, delay, period);
    }

    public int scheduleAsyncTask(Runnable task) {
        return getServer().getScheduler().scheduleAsyncDelayedTask(this, task);
    }
    public int scheduleAsyncDelayedTask(long delay, Runnable task) {
        return getServer().getScheduler().scheduleAsyncDelayedTask(this, task, delay);
    }
    public int scheduleAsyncRepeatingTask(long delay, long period, Runnable task) {
        return getServer().getScheduler().scheduleAsyncRepeatingTask(this, task, delay, period);
    }

    public void cancelTask(int taskId) {
        getServer().getScheduler().cancelTask(taskId);
    }
    public void cancelTasks() {
        getServer().getScheduler().cancelTasks(this);
    }
}
