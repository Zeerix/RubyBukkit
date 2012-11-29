package org.notbukkit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginLogger;
import org.jruby.RubyClass;
import org.jruby.embed.ScriptingContainer;

import com.avaje.ebean.EbeanServer;
import java.util.ArrayList;
import java.util.List;

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
    private boolean naggable = true;
    private FileConfiguration newConfig;
    private File configFile;
    private PluginLogger logger = null;

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

    @Override
    public FileConfiguration getConfig() {
        if (newConfig == null) {
            reloadConfig();
        }
        return newConfig;
    }

    @Override
    public void reloadConfig() {
        newConfig = YamlConfiguration.loadConfiguration(configFile);
    }

    @Override
    public void saveConfig() {
        try {
            newConfig.save(configFile);
        } catch (IOException ex) {
            Logger.getLogger(RubyPlugin.class.getName()).log(Level.SEVERE, "Could not save config to " + configFile, ex);
        }
    }

    @Override
    public void saveDefaultConfig() {
        try {
            getConfig().save(configFile);
        } catch (IOException ex) {
            Logger.getLogger(RubyPlugin.class.getName()).log(Level.SEVERE, "Could not save config to " + configFile, ex);
        }
    }

    @Override
    public void saveResource(String resourcePath, boolean replace) {
        throw new java.lang.AbstractMethodError(RubyPlugin.class.getName() + ".saveResource is not implemented.");

    }

    @Override
    public InputStream getResource(String filename) {
        throw new java.lang.AbstractMethodError(RubyPlugin.class.getName() + ".getResource is not implemented.");
    }

    @Override
    public File getDataFolder() {
        return dataFolder;
    }

    @Override
    public EbeanServer getDatabase() {
        return null;
    }

    protected File getFile() {
        return pluginFile;
    }

    @Override
    public PluginDescriptionFile getDescription() {
        return description;
    }

    @Override
    public final PluginLoader getPluginLoader() {
        return loader;
    }

    @Override
    public final Server getServer() {
        return server;
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        getServer().getLogger().severe("Plugin " + getDescription().getFullName() + " does not contain any generators that may be used in the default world!");
        return null;
    }

    @Override
    public final boolean isNaggable() {
        return naggable;
    }

    @Override
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

    @Override
    public final boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public Logger getLogger() {
        if (logger == null) {
            logger = new PluginLogger(this);
        }
        return logger;
    }

    @Override
    public String getName() {
        return getDescription().getName();
    }

    @Override
    public String toString() {
        return getDescription().getFullName();
    }

    @Override
    public final int hashCode() {
        return getName().hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        return (obj instanceof Plugin) && getName().equals(((Plugin) obj).getName());
    }

    // *** callback methods for the implementation plugin ***

    @Override
    public void onLoad() {
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }

    public List<String> onTabComplete(CommandSender cs, Command cmnd, String string, String[] strings) {
        return new ArrayList<String>();
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

    protected void registerRubyBlock(Class<? extends Event> type, EventPriority priority, RubyListener listener) {
        Bukkit.getPluginManager().registerEvent(type, listener, priority, new RubyExecutor(), this);
    }

    @SuppressWarnings("unchecked")
    protected void registerRubyBlock(RubyClass rubyClass, EventPriority priority, RubyListener listener) {
        registerRubyBlock(rubyClass.getReifiedClass(), priority, listener);
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
        return Bukkit.getScheduler().scheduleSyncDelayedTask(this, task);
    }
    public int scheduleSyncDelayedTask(long delay, Runnable task) {
        return Bukkit.getScheduler().scheduleSyncDelayedTask(this, task, delay);
    }
    public int scheduleSyncRepeatingTask(long delay, long period, Runnable task) {
        return Bukkit.getScheduler().scheduleSyncRepeatingTask(this, task, delay, period);
    }

    public int scheduleAsyncTask(Runnable task) {
        return Bukkit.getScheduler().scheduleAsyncDelayedTask(this, task);
    }
    public int scheduleAsyncDelayedTask(long delay, Runnable task) {
        return Bukkit.getScheduler().scheduleAsyncDelayedTask(this, task, delay);
    }
    public int scheduleAsyncRepeatingTask(long delay, long period, Runnable task) {
        return Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, task, delay, period);
    }

    public void cancelTask(int taskId) {
        Bukkit.getScheduler().cancelTask(taskId);
    }
    public void cancelTasks() {
        Bukkit.getScheduler().cancelTasks(this);
    }
}
