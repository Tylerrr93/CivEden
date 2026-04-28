package world.edenmc.shortwave;

import world.edenmc.shortwave.listeners.InteractionListener;
import world.edenmc.shortwave.listeners.OxidationListener;
import world.edenmc.shortwave.managers.ConfigManager;
import world.edenmc.shortwave.managers.GUIManager;
import world.edenmc.shortwave.managers.SpeakerManager;
import world.edenmc.shortwave.managers.TowerManager;
import world.edenmc.shortwave.tasks.BroadcastTask;
import world.edenmc.shortwave.tasks.ParticleTask;
import world.edenmc.shortwave.voice.VoiceManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import vg.civcraft.mc.civmodcore.inventory.gui.ClickableInventoryListener;
import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.permission.PermissionType;

import java.util.Arrays;
import java.util.List;

public class ShortwavePlugin extends JavaPlugin {
    
    private static ShortwavePlugin instance;
    private ConfigManager configManager;
    private TowerManager towerManager;
    private GUIManager guiManager;
    private SpeakerManager speakerManager;
    private BroadcastTask broadcastTask;
    private ParticleTask particleTask;
    private PermissionType useRadioPermission;
    private VoiceManager voiceManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.towerManager = new TowerManager(this);
        this.guiManager = new GUIManager(this);
        this.speakerManager = new SpeakerManager(this);
        speakerManager.load();

        // Load towers from disk
        towerManager.loadTowers();
        
        // Runs every second; each tower checks its own per-tower interval
        this.broadcastTask = new BroadcastTask(this);
        broadcastTask.runTaskTimer(this, 20L, 20L);
        
        // Register Citadel/NameLayer permission for radio tower access (members and above)
        List<PlayerType> membersAndAbove = Arrays.asList(
                PlayerType.MEMBERS, PlayerType.MODS, PlayerType.ADMINS, PlayerType.OWNER);
        useRadioPermission = PermissionType.registerPermission(
                "USE_RADIO", membersAndAbove,
                "Allows interacting with radio towers reinforced on this group.");

        // Register listeners
        getServer().getPluginManager().registerEvents(new InteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new OxidationListener(this), this);
        getServer().getPluginManager().registerEvents(new ClickableInventoryListener(), this);

        int autoSaveTicks = configManager.getAutoSaveInterval();
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            towerManager.saveTowers();
            speakerManager.save();
        }, autoSaveTicks, autoSaveTicks);

        // Register command executor
        getCommand("shortwave").setExecutor(this);

        // Clean up any holograms left over from a previous server session
        Bukkit.getScheduler().runTaskLater(this, this::cleanupStuckHolograms, 1L);

        // SimpleVoiceChat — soft dependency: only load if the plugin is present.
        // VoiceManager class references SVC API classes, so we must not touch it if SVC is absent.
        if (getServer().getPluginManager().getPlugin("voicechat") != null) {
            setupVoiceChat();
            // 4-tick particle loop for tower "on-air" and speaker audio indicators
            this.particleTask = new ParticleTask(this);
            particleTask.runTaskTimer(this, 4L, 4L);
        }

        getLogger().info("Shortwave Radio Plugin enabled!");
        getLogger().info("Broadcast task running every second (per-tower intervals apply)");
        getLogger().info("Default tower range: " + configManager.getDefaultRange() + " blocks");
    }
    
    @Override
    public void onDisable() {
        // Cancel broadcast task
        if (broadcastTask != null) {
            broadcastTask.cancel();
        }
        
        // Shut down voice relay and particles
        if (particleTask != null) {
            particleTask.cancel();
        }
        if (voiceManager != null) {
            voiceManager.shutdown();
        }

        // Save towers and speakers to disk
        if (towerManager != null) {
            towerManager.saveTowers();
        }
        if (speakerManager != null) {
            speakerManager.save();
        }

        // Remove any broadcast holograms still in the world
        cleanupStuckHolograms();

        getLogger().info("Shortwave Radio Plugin disabled!");
    }
    
    public static ShortwavePlugin getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public TowerManager getTowerManager() {
        return towerManager;
    }
    
    public GUIManager getGUIManager() {
        return guiManager;
    }

    public SpeakerManager getSpeakerManager() {
        return speakerManager;
    }

    /** Null if SimpleVoiceChat is not installed or not yet started. */
    public VoiceManager getVoiceManager() {
        return voiceManager;
    }

    /** Called only when the voicechat plugin is confirmed present — keeps SVC classes isolated. */
    private void setupVoiceChat() {
        voiceManager = new VoiceManager(this);
        if (voiceManager.tryRegister()) {
            getLogger().info("SimpleVoiceChat detected — voice relay enabled.");
        } else {
            getLogger().warning("SimpleVoiceChat present but BukkitVoicechatService not available; voice relay disabled.");
            voiceManager = null;
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("shortwave")) {
            return false;
        }
        if (args.length == 0) {
            sender.sendMessage("Usage: /shortwave clearholograms");
            return true;
        }
        if (args[0].equalsIgnoreCase("clearholograms")) {
            if (!sender.hasPermission("shortwave.admin")) {
                sender.sendMessage("You don't have permission to use this command.");
                return true;
            }
            int count = cleanupStuckHolograms();
            sender.sendMessage("Removed " + count + " stuck broadcast hologram(s).");
            return true;
        }
        sender.sendMessage("Unknown subcommand. Usage: /shortwave clearholograms");
        return true;
    }

    public int cleanupStuckHolograms() {
        NamespacedKey broadcastKey = getBroadcastKey();
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof TextDisplay
                        && entity.getPersistentDataContainer().has(broadcastKey, PersistentDataType.BYTE)) {
                    entity.remove();
                    count++;
                }
            }
        }
        return count;
    }

    public NamespacedKey getBroadcastKey() {
        return new NamespacedKey(this, "broadcast_hologram");
    }

    public PermissionType getUseRadioPermission() {
        return useRadioPermission;
    }

    public NamespacedKey getKey(String key) {
        return new NamespacedKey(this, key);
    }
}
