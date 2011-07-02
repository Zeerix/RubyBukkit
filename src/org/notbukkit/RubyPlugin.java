package org.notbukkit;

import java.io.File;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.EventExecutor;
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
    
    public void registerEvent(Event.Type type, Event.Priority priority, RubyListener listener) {
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
