package world.edenmc.shortwave.managers;

import world.edenmc.shortwave.ShortwavePlugin;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class ConfigManager {
    
    private final ShortwavePlugin plugin;
    
    private List<RangeUpgrade> cachedRangeUpgrades;
    private List<BroadcastLineUpgrade> cachedBroadcastLineUpgrades;
    private List<BroadcastIntervalUpgrade> cachedBroadcastIntervalUpgrades;
    private Map<Material, FuelConfig> cachedAlternateFuels;

    public ConfigManager(ShortwavePlugin plugin) {
        this.plugin = plugin;
    }

    public void clearCache() {
        cachedRangeUpgrades = null;
        cachedBroadcastLineUpgrades = null;
        cachedBroadcastIntervalUpgrades = null;
        cachedAlternateFuels = null;
    }

    // Broadcast settings
    public int getDefaultRange() {
        return plugin.getConfig().getInt("default-tower-range", 500);
    }
    
    // Frequency settings
    public double getMinFrequency() {
        return plugin.getConfig().getDouble("frequency.min", 88.0);
    }
    
    public double getMaxFrequency() {
        return plugin.getConfig().getDouble("frequency.max", 108.0);
    }
    
    public int getFrequencyDecimalPlaces() {
        return plugin.getConfig().getInt("frequency.decimal-places", 1);
    }
    
    public String getFrequencyRegex() {
        int decimals = getFrequencyDecimalPlaces();
        if (decimals == 1) {
            return "\\d+\\.\\d";
        } else if (decimals == 2) {
            return "\\d+\\.\\d{2}";
        }
        return "\\d+\\.\\d+";
    }
    
    public String normalizeFrequency(String input) {
        if (input == null) return null;
        int decimals = getFrequencyDecimalPlaces();
        if (input.matches("\\d+")) {
            return input + "." + "0".repeat(Math.max(1, decimals));
        }
        // Pad trailing zeros if fewer decimal places than required (e.g. "100.2" → "100.20")
        if (input.matches("\\d+\\.\\d+")) {
            int currentDecimals = input.length() - input.indexOf('.') - 1;
            if (currentDecimals < decimals) {
                return input + "0".repeat(decimals - currentDecimals);
            }
        }
        return input;
    }

    public boolean isValidFrequency(String frequency) {
        String normalized = normalizeFrequency(frequency);
        if (!normalized.matches(getFrequencyRegex())) {
            return false;
        }
        try {
            double freq = Double.parseDouble(normalized);
            return freq >= getMinFrequency() && freq <= getMaxFrequency();
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public String getFrequencyFormatExample() {
        int decimals = getFrequencyDecimalPlaces();
        if (decimals == 1) {
            return "104.5";
        } else if (decimals == 2) {
            return "104.53";
        }
        return "104.5";
    }
    
    // Fuel settings
    public Material getFuelItem() {
        String item = plugin.getConfig().getString("fuel.item", "COAL");
        try {
            return Material.valueOf(item.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid fuel item: " + item + ", using COAL");
            return Material.COAL;
        }
    }
    
    public int getFuelDuration() {
        return plugin.getConfig().getInt("fuel.duration-seconds", 600);
    }
    
    public String getFuelDisplayName() {
        return plugin.getConfig().getString("fuel.display-name", "&6Coal Fuel");
    }
    
    public Map<Material, FuelConfig> getAlternateFuels() {
        if (cachedAlternateFuels != null) return cachedAlternateFuels;
        Map<Material, FuelConfig> fuels = new HashMap<>();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("alternate-fuels");
        if (section == null) {
            cachedAlternateFuels = fuels;
            return cachedAlternateFuels;
        }

        for (String key : section.getKeys(false)) {
            try {
                Material material = Material.valueOf(key.toUpperCase());
                int duration = section.getInt(key + ".duration-seconds", 600);
                String displayName = section.getString(key + ".display-name", key);
                fuels.put(material, new FuelConfig(material, duration, displayName));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid fuel material: " + key);
            }
        }

        cachedAlternateFuels = fuels;
        return cachedAlternateFuels;
    }
    
    public int getFuelDurationForItem(Material material) {
        // Check primary fuel
        if (material == getFuelItem()) {
            return getFuelDuration();
        }
        
        // Check alternate fuels
        Map<Material, FuelConfig> alternates = getAlternateFuels();
        if (alternates.containsKey(material)) {
            return alternates.get(material).duration;
        }
        
        return 0; // Not a fuel item
    }
    
    // Receiver settings
    public Material getReceiverItem() {
        String item = plugin.getConfig().getString("receiver.item", "COMPASS");
        try {
            return Material.valueOf(item.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid receiver item: " + item + ", using COMPASS");
            return Material.COMPASS;
        }
    }
    
    public String getReceiverDisplayName(String frequency) {
        return plugin.getConfig().getString("receiver.display-name", "&6Radio [{frequency}]")
                .replace("{frequency}", frequency);
    }
    
    public List<String> getReceiverLore(String frequency) {
        List<String> lore = plugin.getConfig().getStringList("receiver.lore");
        List<String> processed = new ArrayList<>();
        for (String line : lore) {
            processed.add(line.replace("{frequency}", frequency));
        }
        return processed;
    }
    
    // Jingle settings
    public List<JingleConfig> getJingles() {
        List<JingleConfig> jingles = new ArrayList<>();
        
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("jingles");
        if (section == null) {
            // Return defaults if not configured
            jingles.add(new JingleConfig("the-alert", Sound.BLOCK_BELL_USE, "&6The Alert", "Classic bell ding"));
            jingles.add(new JingleConfig("the-tech", Sound.BLOCK_BEACON_ACTIVATE, "&bThe Tech", "Futuristic beacon hum"));
            jingles.add(new JingleConfig("the-magic", Sound.BLOCK_AMETHYST_BLOCK_CHIME, "&dThe Magic", "Mystical amethyst chime"));
            jingles.add(new JingleConfig("the-classic", Sound.BLOCK_NOTE_BLOCK_PLING, "&eThe Classic", "Note block pling"));
            jingles.add(new JingleConfig("the-retro", Sound.ENTITY_EXPERIENCE_ORB_PICKUP, "&aThe Retro", "XP orb pickup sound"));
            return jingles;
        }
        
        for (String key : section.getKeys(false)) {
            try {
                String soundName = section.getString(key + ".sound", "BLOCK_BELL_USE");
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                String displayName = section.getString(key + ".display-name", key);
                String description = section.getString(key + ".description", "");
                
                jingles.add(new JingleConfig(key, sound, displayName, description));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid jingle sound: " + key + " - " + e.getMessage());
            }
        }
        
        return jingles;
    }
    
    public Sound getDefaultJingle() {
        String soundName = plugin.getConfig().getString("default-jingle", "BLOCK_BELL_USE");
        try {
            return Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid default jingle sound: " + soundName);
            return Sound.BLOCK_BELL_USE;
        }
    }
    
    // Range upgrade settings
    public List<RangeUpgrade> getRangeUpgrades() {
        if (cachedRangeUpgrades != null) return cachedRangeUpgrades;
        List<RangeUpgrade> upgrades = new ArrayList<>();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("range-upgrades");
        if (section == null) {
            upgrades.add(new RangeUpgrade(1, Material.CHISELED_COPPER, 500, "&7Basic Tier", 0));
            upgrades.add(new RangeUpgrade(2, Material.IRON_BLOCK, 1000, "&7Iron Tier", 3));
            upgrades.add(new RangeUpgrade(3, Material.GOLD_BLOCK, 2500, "&eGold Tier", 5));
            upgrades.add(new RangeUpgrade(4, Material.DIAMOND_BLOCK, 5000, "&bDiamond Tier", 7));
            upgrades.add(new RangeUpgrade(5, Material.NETHERITE_BLOCK, 10000, "&5Netherite Tier", 9));
            cachedRangeUpgrades = upgrades;
            return cachedRangeUpgrades;
        }

        List<String> tierKeys = new ArrayList<>(section.getKeys(false));
        tierKeys.sort((a, b) -> {
            int tierA = Integer.parseInt(a.replace("tier-", ""));
            int tierB = Integer.parseInt(b.replace("tier-", ""));
            return Integer.compare(tierA, tierB);
        });

        for (String key : tierKeys) {
            try {
                int tier = Integer.parseInt(key.replace("tier-", ""));
                String materialName = section.getString(key + ".material", "IRON_BLOCK");
                Material material = Material.valueOf(materialName.toUpperCase());
                int range = section.getInt(key + ".range", 1000);
                String displayName = section.getString(key + ".display-name", "&7Tier " + tier);
                int cost = section.getInt(key + ".cost", 1);

                upgrades.add(new RangeUpgrade(tier, material, range, displayName, cost));
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid upgrade tier: " + key + " - " + e.getMessage());
            }
        }

        cachedRangeUpgrades = upgrades;
        return cachedRangeUpgrades;
    }
    
    public RangeUpgrade getUpgradeByRange(int range) {
        List<RangeUpgrade> upgrades = getRangeUpgrades();
        for (RangeUpgrade upgrade : upgrades) {
            if (upgrade.range == range) {
                return upgrade;
            }
        }
        return upgrades.get(0); // Return first tier if not found
    }
    
    public RangeUpgrade getNextUpgrade(int currentRange) {
        List<RangeUpgrade> upgrades = getRangeUpgrades();
        for (int i = 0; i < upgrades.size() - 1; i++) {
            if (upgrades.get(i).range == currentRange) {
                return upgrades.get(i + 1); // Return next tier
            }
        }
        return null; // Already at max tier
    }
    
    // Broadcast line upgrade settings
    public List<BroadcastLineUpgrade> getBroadcastLineUpgrades() {
        if (cachedBroadcastLineUpgrades != null) return cachedBroadcastLineUpgrades;
        List<BroadcastLineUpgrade> upgrades = new ArrayList<>();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("broadcast-line-upgrades");
        if (section == null) {
            upgrades.add(new BroadcastLineUpgrade(1, 1, 0, Material.CHISELED_COPPER, "&7Basic (1 Line)"));
            upgrades.add(new BroadcastLineUpgrade(2, 2, 3, Material.IRON_BLOCK, "&7Standard (2 Lines)"));
            upgrades.add(new BroadcastLineUpgrade(3, 3, 5, Material.GOLD_BLOCK, "&eEnhanced (3 Lines)"));
            upgrades.add(new BroadcastLineUpgrade(4, 4, 7, Material.DIAMOND_BLOCK, "&bPremium (4 Lines)"));
            upgrades.add(new BroadcastLineUpgrade(5, 5, 9, Material.NETHERITE_BLOCK, "&5Elite (5 Lines)"));
            upgrades.add(new BroadcastLineUpgrade(6, 6, 12, Material.EMERALD_BLOCK, "&aMax (6 Lines)"));
            cachedBroadcastLineUpgrades = upgrades;
            return cachedBroadcastLineUpgrades;
        }

        List<String> levelKeys = new ArrayList<>(section.getKeys(false));
        levelKeys.sort((a, b) -> {
            int la = Integer.parseInt(a.replace("level-", ""));
            int lb = Integer.parseInt(b.replace("level-", ""));
            return Integer.compare(la, lb);
        });

        for (String key : levelKeys) {
            try {
                int level = Integer.parseInt(key.replace("level-", ""));
                int lines = Math.min(section.getInt(key + ".lines", 1), 6);
                int cost = section.getInt(key + ".cost", 0);
                String materialName = section.getString(key + ".material", "IRON_BLOCK");
                Material material = Material.valueOf(materialName.toUpperCase());
                String displayName = section.getString(key + ".display-name", "&7Level " + level);
                upgrades.add(new BroadcastLineUpgrade(level, lines, cost, material, displayName));
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid broadcast-line-upgrade level: " + key + " - " + e.getMessage());
            }
        }

        cachedBroadcastLineUpgrades = upgrades;
        return cachedBroadcastLineUpgrades;
    }

    public BroadcastLineUpgrade getBroadcastLineUpgradeByLines(int lines) {
        for (BroadcastLineUpgrade u : getBroadcastLineUpgrades()) {
            if (u.lines == lines) return u;
        }
        return getBroadcastLineUpgrades().get(0);
    }

    public BroadcastLineUpgrade getNextBroadcastLineUpgrade(int currentLines) {
        List<BroadcastLineUpgrade> upgrades = getBroadcastLineUpgrades();
        for (int i = 0; i < upgrades.size() - 1; i++) {
            if (upgrades.get(i).lines == currentLines) return upgrades.get(i + 1);
        }
        return null;
    }

    // Broadcast interval upgrade settings
    public List<BroadcastIntervalUpgrade> getBroadcastIntervalUpgrades() {
        if (cachedBroadcastIntervalUpgrades != null) return cachedBroadcastIntervalUpgrades;
        List<BroadcastIntervalUpgrade> upgrades = new ArrayList<>();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("broadcast-interval-upgrades");
        if (section == null) {
            upgrades.add(new BroadcastIntervalUpgrade(1, 45, 0, Material.CHISELED_COPPER, "&7Standard (45s min)"));
            upgrades.add(new BroadcastIntervalUpgrade(2, 30, 3, Material.IRON_BLOCK, "&7Fast (30s min)"));
            upgrades.add(new BroadcastIntervalUpgrade(3, 20, 5, Material.GOLD_BLOCK, "&eFaster (20s min)"));
            upgrades.add(new BroadcastIntervalUpgrade(4, 10, 7, Material.DIAMOND_BLOCK, "&bFastest (10s min)"));
            cachedBroadcastIntervalUpgrades = upgrades;
            return cachedBroadcastIntervalUpgrades;
        }

        List<String> tierKeys = new ArrayList<>(section.getKeys(false));
        tierKeys.sort((a, b) -> {
            int ta = Integer.parseInt(a.replace("tier-", ""));
            int tb = Integer.parseInt(b.replace("tier-", ""));
            return Integer.compare(ta, tb);
        });

        for (String key : tierKeys) {
            try {
                int tier = Integer.parseInt(key.replace("tier-", ""));
                int intervalSeconds = section.getInt(key + ".interval-seconds", 45);
                int cost = section.getInt(key + ".cost", 0);
                String materialName = section.getString(key + ".material", "CHISELED_COPPER");
                Material material = Material.valueOf(materialName.toUpperCase());
                String displayName = section.getString(key + ".display-name", "&7Tier " + tier);
                upgrades.add(new BroadcastIntervalUpgrade(tier, intervalSeconds, cost, material, displayName));
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid broadcast-interval-upgrade tier: " + key + " - " + e.getMessage());
            }
        }

        cachedBroadcastIntervalUpgrades = upgrades;
        return cachedBroadcastIntervalUpgrades;
    }

    public BroadcastIntervalUpgrade getBroadcastIntervalUpgradeByInterval(int intervalSeconds) {
        for (BroadcastIntervalUpgrade u : getBroadcastIntervalUpgrades()) {
            if (u.intervalSeconds == intervalSeconds) return u;
        }
        return getBroadcastIntervalUpgrades().get(0);
    }

    public BroadcastIntervalUpgrade getNextBroadcastIntervalUpgrade(int currentMinInterval) {
        List<BroadcastIntervalUpgrade> upgrades = getBroadcastIntervalUpgrades();
        for (int i = 0; i < upgrades.size() - 1; i++) {
            if (upgrades.get(i).intervalSeconds == currentMinInterval) return upgrades.get(i + 1);
        }
        return null;
    }

    // GUI settings
    public String getTowerGuiTitle() {
        return plugin.getConfig().getString("gui.tower-gui-title", "&6&l📻 Radio Tower Control");
    }
    
    public String getFuelGuiTitle() {
        return plugin.getConfig().getString("gui.fuel-gui-title", "&6&l⛽ Tower Fuel Management");
    }
    
    public String getJingleGuiTitle() {
        return plugin.getConfig().getString("gui.jingle-gui-title", "&6&l🎵 Select Station Jingle");
    }
    
    public String getUpgradeGuiTitle() {
        return plugin.getConfig().getString("gui.upgrade-gui-title", "&6&l⚡ Tower Range Upgrade");
    }

    public String getBroadcastLineGuiTitle() {
        return plugin.getConfig().getString("gui.broadcast-line-gui-title", "&6&l📄 Broadcast Lines");
    }

    public String getBroadcastIntervalGuiTitle() {
        return plugin.getConfig().getString("gui.broadcast-interval-gui-title", "&6&l⏱ Broadcast Interval");
    }

    public String getStartupCostGuiTitle() {
        return plugin.getConfig().getString("gui.startup-cost-gui-title", "&6&l📡 Tower Activation");
    }

    public String getSpeakerStartupCostGuiTitle() {
        return plugin.getConfig().getString("gui.speaker-startup-cost-gui-title", "&6&l📻 Speaker Activation");
    }

    // Startup cost settings
    public Material getStartupCostMaterial() {
        String item = plugin.getConfig().getString("startup-cost.material", "IRON_BLOCK");
        try {
            return Material.valueOf(item.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid startup cost material: " + item + ", using IRON_BLOCK");
            return Material.IRON_BLOCK;
        }
    }

    public int getStartupCostAmount() {
        return plugin.getConfig().getInt("startup-cost.amount", 5);
    }

    public String getStartupCostDisplayName() {
        return plugin.getConfig().getString("startup-cost.display-name", "&7Activation Cost");
    }

    public List<String> getStartupCostLore() {
        return plugin.getConfig().getStringList("startup-cost.lore");
    }

    // Speaker startup cost settings
    public Material getSpeakerStartupCostMaterial() {
        String item = plugin.getConfig().getString("speaker-startup-cost.material", "IRON_INGOT");
        try {
            return Material.valueOf(item.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid speaker startup cost material: " + item + ", using IRON_INGOT");
            return Material.IRON_INGOT;
        }
    }

    public int getSpeakerStartupCostAmount() {
        return plugin.getConfig().getInt("speaker-startup-cost.amount", 8);
    }

    public String getSpeakerStartupCostDisplayName() {
        return plugin.getConfig().getString("speaker-startup-cost.display-name", "&7Speaker Activation Cost");
    }

    public List<String> getSpeakerStartupCostLore() {
        return plugin.getConfig().getStringList("speaker-startup-cost.lore");
    }

    // Other settings

    /**
     * Probability (0.0–1.0) that a natural oxidation tick on tower copper is cancelled.
     * 0.98 → oxidizes at ~2% of vanilla speed.
     */
    public int getInterferenceGarblePerTower() {
        return plugin.getConfig().getInt("interference-garble-per-tower", 25);
    }

    /** Blocks from the tower base copper within which a player can transmit voice. */
    public double getVoiceTransmitterRange() {
        return plugin.getConfig().getDouble("voice.transmitter-range", 5.0);
    }

    /** Block radius from a speaker block within which players can hear relayed voice audio. */
    public int getSpeakerAudioRange() {
        return plugin.getConfig().getInt("voice.speaker-audio-range", 32);
    }

    public double getOxidationCancelChance() {
        return plugin.getConfig().getDouble("tower-oxidation-cancel-chance", 0.98);
    }
    
    public boolean isDebug() {
        return plugin.getConfig().getBoolean("debug", false);
    }
    
    public int getAutoSaveInterval() {
        return plugin.getConfig().getInt("auto-save-interval-ticks", 6000);
    }
    
    // Helper classes
    public static class FuelConfig {
        public final Material material;
        public final int duration;
        public final String displayName;
        
        public FuelConfig(Material material, int duration, String displayName) {
            this.material = material;
            this.duration = duration;
            this.displayName = displayName;
        }
    }
    
    public static class RangeUpgrade {
        public final int tier;
        public final Material material;
        public final int range;
        public final String displayName;
        public final int cost;

        public RangeUpgrade(int tier, Material material, int range, String displayName, int cost) {
            this.tier = tier;
            this.material = material;
            this.range = range;
            this.displayName = displayName;
            this.cost = cost;
        }
    }
    
    public static class JingleConfig {
        public final String id;
        public final Sound sound;
        public final String displayName;
        public final String description;

        public JingleConfig(String id, Sound sound, String displayName, String description) {
            this.id = id;
            this.sound = sound;
            this.displayName = displayName;
            this.description = description;
        }
    }

    public static class BroadcastLineUpgrade {
        public final int level;
        public final int lines;
        public final int cost;
        public final Material material;
        public final String displayName;

        public BroadcastLineUpgrade(int level, int lines, int cost, Material material, String displayName) {
            this.level = level;
            this.lines = lines;
            this.cost = cost;
            this.material = material;
            this.displayName = displayName;
        }
    }

    public static class BroadcastIntervalUpgrade {
        public final int tier;
        public final int intervalSeconds;
        public final int cost;
        public final Material material;
        public final String displayName;

        public BroadcastIntervalUpgrade(int tier, int intervalSeconds, int cost, Material material, String displayName) {
            this.tier = tier;
            this.intervalSeconds = intervalSeconds;
            this.cost = cost;
            this.material = material;
            this.displayName = displayName;
        }
    }
}
