package org.notbukkit;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.bukkit.Server;
import org.bukkit.event.CustomEventListener;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.painting.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.server.*;
import org.bukkit.event.vehicle.*;
import org.bukkit.event.world.*;
import org.bukkit.event.weather.*;
import org.bukkit.event.inventory.*;
import org.bukkit.plugin.*;
import org.jruby.RubyInstanceConfig.CompileMode;
import org.jruby.embed.EmbedEvalUnit;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

public final class RubyPluginLoader implements PluginLoader {
    
    // *** referenced classes ***
    
    private final Server server;

    // *** data ***
    // Embedding: http://kenai.com/projects/jruby/pages/DirectJRubyEmbedding
    // YAML & co: http://kenai.com/projects/jruby/pages/AccessingJRubyObjectInJava
    
    //private final String jarFile = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
    
    private final Pattern[] fileFilters = new Pattern[] {
        Pattern.compile("\\.rb$"),
    };

    private final String packageName = getClass().getPackage().getName();

    // pre-script to make it easier for plugin authors
    private final String preScript =
        "require 'java'\n" +
        "import '" + packageName + ".RubyPlugin'\n";
    
    // Ruby script to check and create an instance of the main plugin class 
    private final String instanceScript =
        "if defined?(className) == 'constant' and className.class == Class then\n" +
        "    className.new\n" +
        "else\n" +
        "    raise 'main class not defined: className'\n" +
        "end\n";
    
    // *** interface ***
    
    public RubyPluginLoader(Server instance) {
        server = instance;
    }
    
    public Plugin loadPlugin(File file) throws InvalidPluginException, InvalidDescriptionException, UnknownDependencyException {
        return loadPlugin(file, false);
    }
    
    public Plugin loadPlugin(File file, boolean ignoreSoftDependencies) throws InvalidPluginException, InvalidDescriptionException, UnknownDependencyException {
        if (!file.exists()) {
            throw new InvalidPluginException(new FileNotFoundException(String.format("%s does not exist", file.getPath())));
        }     
        
        // create a scripting container for every plugin to encapsulate it
        ScriptingContainer runtime = new ScriptingContainer(LocalContextScope.CONCURRENT);
        runtime.setClassLoader(runtime.getClass().getClassLoader());
        runtime.setCompileMode(CompileMode.JIT);
        //runtime.setCompatVersion(CompatVersion.RUBY1_9);
        //runtime.setHomeDirectory( "/path/to/home/" );
        
        try {
            // parse and run script
            runtime.runScriptlet(preScript);
            
            final EmbedEvalUnit eval = runtime.parse(PathType.RELATIVE, file.getPath());
            /*IRubyObject res =*/ eval.run();
    
            // create plugin description
            final PluginDescriptionFile description = getDescriptionFile(runtime);
            final File dataFolder = new File(file.getParentFile(), description.getName());
        
            // create main plugin class
            final String script = instanceScript.replaceAll("className", description.getMain());
            
            final RubyPlugin plugin = (RubyPlugin)runtime.runScriptlet(script);     // create instance of main class
            plugin.initialize(this, server, description, dataFolder, file, runtime);
            return plugin;
        } catch (Throwable ex) {
            throw new InvalidPluginException(ex);
        }
    }
    
    /**
     * extract description from Ruby script 
     */
    private PluginDescriptionFile getDescriptionFile(ScriptingContainer runtime) throws InvalidDescriptionException {
        String name;
        try {
            name = runtime.get("Name").toString();
        } catch (Exception e) {
            throw new InvalidDescriptionException(e, "name is not defined");
        }
        if (!name.matches("^[A-Za-z0-9 _.-]+$")) {
            throw new InvalidDescriptionException("name '" + name + "' contains invalid characters.");        
        }
        
        String version;
        try {
            version = runtime.get("Version").toString();
        } catch (Exception e) {
            throw new InvalidDescriptionException(e, "version is not defined");
        }            

        String main;
        try {
            main = runtime.get("Main").toString();
        } catch (Exception e) {
            throw new InvalidDescriptionException(e, "main is not defined");
        }
        
        PluginDescriptionFile description = new PluginDescriptionFile(name, version, main);
        return description;
    }

    public Pattern[] getPluginFileFilters() {
        return fileFilters;
    }    
    
    public EventExecutor createExecutor(Event.Type type, Listener listener) {
        
        switch (type) {
            // Player Events
    
        case PLAYER_JOIN:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PlayerListener) listener).onPlayerJoin((PlayerJoinEvent) event);
                }
            };
    
        case PLAYER_QUIT:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PlayerListener) listener).onPlayerQuit((PlayerQuitEvent) event);
                }
            };
    
        case PLAYER_RESPAWN:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PlayerListener) listener).onPlayerRespawn((PlayerRespawnEvent) event);
                }
            };
    
        case PLAYER_KICK:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PlayerListener) listener).onPlayerKick((PlayerKickEvent) event);
                }
            };
    
        case PLAYER_COMMAND_PREPROCESS:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PlayerListener) listener).onPlayerCommandPreprocess((PlayerCommandPreprocessEvent) event);
                }
            };
    
        case PLAYER_CHAT:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PlayerListener) listener).onPlayerChat((PlayerChatEvent) event);
                }
            };
    
        case PLAYER_MOVE:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PlayerListener) listener).onPlayerMove((PlayerMoveEvent) event);
                }
            };
    
        case PLAYER_TELEPORT:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PlayerListener) listener).onPlayerTeleport((PlayerTeleportEvent) event);
                }
            };
    
        case PLAYER_PORTAL:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PlayerListener) listener).onPlayerPortal((PlayerPortalEvent) event);
                }
            };
    
        case PLAYER_INTERACT:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PlayerListener) listener).onPlayerInteract((PlayerInteractEvent) event);
                }
            };
    
        case PLAYER_INTERACT_ENTITY:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PlayerListener) listener).onPlayerInteractEntity((PlayerInteractEntityEvent) event);
                }
            };
    
        case PLAYER_LOGIN:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PlayerListener) listener).onPlayerLogin((PlayerLoginEvent) event);
                }
            };
    
        case PLAYER_PRELOGIN:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PlayerListener) listener).onPlayerPreLogin((PlayerPreLoginEvent) event);
                }
            };
    
        case PLAYER_EGG_THROW:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PlayerListener) listener).onPlayerEggThrow((PlayerEggThrowEvent) event);
                }
            };
    
        case PLAYER_ANIMATION:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PlayerListener) listener).onPlayerAnimation((PlayerAnimationEvent) event);
                }
            };
    
        case INVENTORY_OPEN:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PlayerListener) listener).onInventoryOpen((PlayerInventoryEvent) event);
                }
            };
    
        case PLAYER_ITEM_HELD:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PlayerListener) listener).onItemHeldChange((PlayerItemHeldEvent) event);
                }
            };
    
        case PLAYER_DROP_ITEM:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PlayerListener) listener).onPlayerDropItem((PlayerDropItemEvent) event);
                }
            };
    
        case PLAYER_PICKUP_ITEM:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PlayerListener) listener).onPlayerPickupItem((PlayerPickupItemEvent) event);
                }
            };
    
        case PLAYER_TOGGLE_SNEAK:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PlayerListener) listener).onPlayerToggleSneak((PlayerToggleSneakEvent) event);
                }
            };
    
        case PLAYER_BUCKET_EMPTY:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PlayerListener) listener).onPlayerBucketEmpty((PlayerBucketEmptyEvent) event);
                }
            };
    
        case PLAYER_BUCKET_FILL:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PlayerListener) listener).onPlayerBucketFill((PlayerBucketFillEvent) event);
                }
            };
    
        case PLAYER_BED_ENTER:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PlayerListener) listener).onPlayerBedEnter((PlayerBedEnterEvent) event);
                }
            };
    
        case PLAYER_BED_LEAVE:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((PlayerListener) listener).onPlayerBedLeave((PlayerBedLeaveEvent) event);
                }
            };
    
        // Block Events
        case BLOCK_PHYSICS:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((BlockListener) listener).onBlockPhysics((BlockPhysicsEvent) event);
                }
            };
    
        case BLOCK_CANBUILD:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((BlockListener) listener).onBlockCanBuild((BlockCanBuildEvent) event);
                }
            };
    
        case BLOCK_PLACE:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((BlockListener) listener).onBlockPlace((BlockPlaceEvent) event);
                }
            };
    
        case BLOCK_DAMAGE:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((BlockListener) listener).onBlockDamage((BlockDamageEvent) event);
                }
            };
    
        case BLOCK_FROMTO:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((BlockListener) listener).onBlockFromTo((BlockFromToEvent) event);
                }
            };
    
        case LEAVES_DECAY:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((BlockListener) listener).onLeavesDecay((LeavesDecayEvent) event);
                }
            };
    
        case SIGN_CHANGE:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((BlockListener) listener).onSignChange((SignChangeEvent) event);
                }
            };
    
        case BLOCK_IGNITE:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((BlockListener) listener).onBlockIgnite((BlockIgniteEvent) event);
                }
            };
    
        case REDSTONE_CHANGE:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((BlockListener) listener).onBlockRedstoneChange((BlockRedstoneEvent) event);
                }
            };
    
        case BLOCK_BURN:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((BlockListener) listener).onBlockBurn((BlockBurnEvent) event);
                }
            };
    
        case BLOCK_BREAK:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((BlockListener) listener).onBlockBreak((BlockBreakEvent) event);
                }
            };
    
        case SNOW_FORM:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((BlockListener) listener).onSnowForm((SnowFormEvent) event);
                }
            };
    
        case BLOCK_FORM:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((BlockListener) listener).onBlockForm((BlockFormEvent) event);
                }
            };
    
        case BLOCK_SPREAD:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((BlockListener) listener).onBlockSpread((BlockSpreadEvent) event);
                }
            };
    
    
        case BLOCK_FADE:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((BlockListener) listener).onBlockFade((BlockFadeEvent) event);
                }
            };
    
        case BLOCK_DISPENSE:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((BlockListener) listener).onBlockDispense((BlockDispenseEvent) event);
                }
            };
    
        // Server Events
        case PLUGIN_ENABLE:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((ServerListener) listener).onPluginEnable((PluginEnableEvent) event);
                }
            };
    
        case PLUGIN_DISABLE:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((ServerListener) listener).onPluginDisable((PluginDisableEvent) event);
                }
            };
    
        case SERVER_COMMAND:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((ServerListener) listener).onServerCommand((ServerCommandEvent) event);
                }
            };
    
        // World Events
        case CHUNK_LOAD:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((WorldListener) listener).onChunkLoad((ChunkLoadEvent) event);
                }
            };
    
        case CHUNK_POPULATED:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((WorldListener) listener).onChunkPopulate((ChunkPopulateEvent) event);
                }
            };
    
        case CHUNK_UNLOAD:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((WorldListener) listener).onChunkUnload((ChunkUnloadEvent) event);
                }
            };
    
        case SPAWN_CHANGE:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((WorldListener) listener).onSpawnChange((SpawnChangeEvent) event);
                }
            };
    
        case WORLD_SAVE:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((WorldListener) listener).onWorldSave((WorldSaveEvent) event);
                }
            };
    
        case WORLD_INIT:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((WorldListener) listener).onWorldInit((WorldInitEvent) event);
                }
            };
    
        case WORLD_LOAD:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((WorldListener) listener).onWorldLoad((WorldLoadEvent) event);
                }
            };
    
        case WORLD_UNLOAD:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((WorldListener) listener).onWorldUnload((WorldUnloadEvent) event);
                }
            };
    
        case PORTAL_CREATE:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((WorldListener) listener).onPortalCreate((PortalCreateEvent) event);
                }
            };
    
        // Painting Events
        case PAINTING_PLACE:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((EntityListener) listener).onPaintingPlace((PaintingPlaceEvent) event);
                }
            };
    
        case PAINTING_BREAK:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((EntityListener) listener).onPaintingBreak((PaintingBreakEvent) event);
                }
            };
    
        // Entity Events
        case ENTITY_DAMAGE:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((EntityListener) listener).onEntityDamage((EntityDamageEvent) event);
                }
            };
    
        case ENTITY_DEATH:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((EntityListener) listener).onEntityDeath((EntityDeathEvent) event);
                }
            };
    
        case ENTITY_COMBUST:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((EntityListener) listener).onEntityCombust((EntityCombustEvent) event);
                }
            };
    
        case ENTITY_EXPLODE:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((EntityListener) listener).onEntityExplode((EntityExplodeEvent) event);
                }
            };
    
        case EXPLOSION_PRIME:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((EntityListener) listener).onExplosionPrime((ExplosionPrimeEvent) event);
                }
            };
    
        case ENTITY_TARGET:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((EntityListener) listener).onEntityTarget((EntityTargetEvent) event);
                }
            };
    
        case ENTITY_INTERACT:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((EntityListener) listener).onEntityInteract((EntityInteractEvent) event);
                }
            };
    
        case ENTITY_PORTAL_ENTER:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((EntityListener) listener).onEntityPortalEnter((EntityPortalEnterEvent) event);
                }
            };
    
        case CREATURE_SPAWN:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((EntityListener) listener).onCreatureSpawn((CreatureSpawnEvent) event);
                }
            };
    
        case ITEM_SPAWN:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((EntityListener) listener).onItemSpawn((ItemSpawnEvent) event);
                }
            };
    
        case PIG_ZAP:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((EntityListener) listener).onPigZap((PigZapEvent) event);
                }
            };
    
        case CREEPER_POWER:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((EntityListener) listener).onCreeperPower((CreeperPowerEvent) event);
                }
            };
    
        case ENTITY_TAME:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((EntityListener) listener).onEntityTame((EntityTameEvent) event);
                }
            };
    
        case ENTITY_REGAIN_HEALTH:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((EntityListener) listener).onEntityRegainHealth((EntityRegainHealthEvent) event);
                }
            };
    
        case PROJECTILE_HIT:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((EntityListener) listener).onProjectileHit((ProjectileHitEvent) event);
                }
            };
    
        // Vehicle Events
        case VEHICLE_CREATE:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((VehicleListener) listener).onVehicleCreate((VehicleCreateEvent) event);
                }
            };
    
        case VEHICLE_DAMAGE:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((VehicleListener) listener).onVehicleDamage((VehicleDamageEvent) event);
                }
            };
    
        case VEHICLE_DESTROY:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((VehicleListener) listener).onVehicleDestroy((VehicleDestroyEvent) event);
                }
            };
    
        case VEHICLE_COLLISION_BLOCK:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((VehicleListener) listener).onVehicleBlockCollision((VehicleBlockCollisionEvent) event);
                }
            };
    
        case VEHICLE_COLLISION_ENTITY:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((VehicleListener) listener).onVehicleEntityCollision((VehicleEntityCollisionEvent) event);
                }
            };
    
        case VEHICLE_ENTER:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((VehicleListener) listener).onVehicleEnter((VehicleEnterEvent) event);
                }
            };
    
        case VEHICLE_EXIT:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((VehicleListener) listener).onVehicleExit((VehicleExitEvent) event);
                }
            };
    
        case VEHICLE_MOVE:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((VehicleListener) listener).onVehicleMove((VehicleMoveEvent) event);
                }
            };
    
        case VEHICLE_UPDATE:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((VehicleListener) listener).onVehicleUpdate((VehicleUpdateEvent) event);
                }
            };
    
        // Weather Events
        case WEATHER_CHANGE:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((WeatherListener) listener).onWeatherChange((WeatherChangeEvent) event);
                }
            };
    
        case THUNDER_CHANGE:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((WeatherListener) listener).onThunderChange((ThunderChangeEvent) event);
                }
            };
    
        case LIGHTNING_STRIKE:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((WeatherListener) listener).onLightningStrike((LightningStrikeEvent) event);
                }
            };
            
            // Inventory Events
        case FURNACE_SMELT:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((InventoryListener) listener).onFurnaceSmelt((FurnaceSmeltEvent) event);
                }
            };
        case FURNACE_BURN:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((InventoryListener) listener).onFurnaceBurn((FurnaceBurnEvent) event);
                }
            };
    
        // Custom Events
        case CUSTOM_EVENT:
            return new EventExecutor() {
                public void execute(Listener listener, Event event) {
                    ((CustomEventListener) listener).onCustomEvent(event);
                }
            };
        }
    
        throw new IllegalArgumentException("Event " + type + " is not supported");
    }

    public void enablePlugin(Plugin plugin) {
        if (!(plugin instanceof RubyPlugin)) {
            throw new IllegalArgumentException("Plugin is not associated with this PluginLoader");
        }
        
        if (!plugin.isEnabled()) {
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
    
    public void disablePlugin(Plugin plugin) {
        if (!(plugin instanceof RubyPlugin)) {
            throw new IllegalArgumentException("Plugin is not associated with this PluginLoader");
        }
        
        if (plugin.isEnabled()) {
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
