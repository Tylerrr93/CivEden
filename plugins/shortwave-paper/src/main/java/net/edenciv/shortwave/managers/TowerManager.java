package net.edenciv.shortwave.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.edenciv.shortwave.ShortwavePlugin;
import net.edenciv.shortwave.models.RadioTower;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TowerManager {
    
    private final ShortwavePlugin plugin;
    private final Map<String, RadioTower> towers; // Key: "world,x,y,z"
    private final File towersFile;
    private final Gson gson;
    
    public TowerManager(ShortwavePlugin plugin) {
        this.plugin = plugin;
        this.towers = new ConcurrentHashMap<>();
        this.towersFile = new File(plugin.getDataFolder(), "towers.json");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }
    
    /**
     * Creates a new tower at the given copper block location
     */
    public RadioTower createTower(Location copperLocation, String frequency) {
        String key = locationToKey(copperLocation);
        RadioTower tower = new RadioTower(copperLocation, frequency);
        towers.put(key, tower);
        saveTowers();
        return tower;
    }
    
    /**
     * Gets a tower by its copper block location
     */
    public RadioTower getTower(Location copperLocation) {
        String key = locationToKey(copperLocation);
        return towers.get(key);
    }
    
    /**
     * Gets a tower by block (checks if it's the copper or lectern)
     */
    public RadioTower getTowerByBlock(Block block) {
        // Check if this block is the copper
        RadioTower tower = getTower(block.getLocation());
        if (tower != null) {
            return tower;
        }
        
        // Check if this block is the lectern (one above copper)
        Location below = block.getLocation().subtract(0, 1, 0);
        return getTower(below);
    }
    
    /**
     * Removes a tower
     */
    public void removeTower(Location copperLocation) {
        String key = locationToKey(copperLocation);
        towers.remove(key);
        saveTowers();
    }
    
    /**
     * Gets all active towers
     */
    public Collection<RadioTower> getAllTowers() {
        return new ArrayList<>(towers.values());
    }
    
    /**
     * Gets all active towers (currently broadcasting)
     */
    public Collection<RadioTower> getActiveTowers() {
        List<RadioTower> active = new ArrayList<>();
        for (RadioTower tower : towers.values()) {
            if (tower.isActive() && tower.isStructureValid()) {
                active.add(tower);
            }
        }
        return active;
    }
    
    /**
     * Save towers to JSON file
     */
    public void saveTowers() {
        try {
            if (!towersFile.exists()) {
                towersFile.getParentFile().mkdirs();
                towersFile.createNewFile();
            }
            
            List<TowerData> dataList = new ArrayList<>();
            for (RadioTower tower : towers.values()) {
                dataList.add(new TowerData(tower));
            }
            
            try (Writer writer = new FileWriter(towersFile)) {
                gson.toJson(dataList, writer);
            }
            
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Saved " + towers.size() + " radio towers");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save towers: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Load towers from JSON file
     */
    public void loadTowers() {
        if (!towersFile.exists()) {
            plugin.getLogger().info("No towers file found, starting fresh");
            return;
        }
        
        try (Reader reader = new FileReader(towersFile)) {
            Type listType = new TypeToken<List<TowerData>>(){}.getType();
            List<TowerData> dataList = gson.fromJson(reader, listType);
            
            if (dataList == null) {
                plugin.getLogger().info("No towers to load");
                return;
            }
            
            int loaded = 0;
            for (TowerData data : dataList) {
                try {
                    RadioTower tower = data.toTower();
                    if (tower != null) {
                        String key = locationToKey(tower.getCopperLocation());
                        towers.put(key, tower);
                        loaded++;
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load tower: " + e.getMessage());
                }
            }
            
            plugin.getLogger().info("Loaded " + loaded + " radio towers");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load towers: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String locationToKey(Location loc) {
        return String.format("%s,%d,%d,%d",
                loc.getWorld().getName(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ());
    }
    
    /**
     * Data class for JSON serialization
     */
    private static class TowerData {
        String world;
        int x, y, z;
        String frequency;
        long fuelEndTime;
        int currentPage;
        String jingle;
        int range;
        int broadcastLinesUnlocked;
        int broadcastLinesSelected;
        int broadcastIntervalUnlocked;
        int broadcastIntervalSelected;
        List<String> cachedPages;
        String cachedOxidation;

        TowerData(RadioTower tower) {
            Location loc = tower.getCopperLocation();
            this.world = loc.getWorld().getName();
            this.x = loc.getBlockX();
            this.y = loc.getBlockY();
            this.z = loc.getBlockZ();
            this.frequency = tower.getFrequency();
            this.fuelEndTime = tower.getFuelEndTime();
            this.currentPage = tower.getCurrentPage();
            this.jingle = tower.getJingle().name();
            this.range = tower.getRange();
            this.broadcastLinesUnlocked = tower.getBroadcastLinesUnlocked();
            this.broadcastLinesSelected = tower.getBroadcastLinesSelected();
            this.broadcastIntervalUnlocked = tower.getBroadcastIntervalUnlocked();
            this.broadcastIntervalSelected = tower.getBroadcastIntervalSelected();
            this.cachedPages = tower.getCachedPages();
            this.cachedOxidation = tower.getCachedOxidation().name();
        }

        RadioTower toTower() {
            World w = Bukkit.getWorld(world);
            if (w == null) {
                return null;
            }
            Location loc = new Location(w, x, y, z);
            return new RadioTower(loc, frequency, fuelEndTime, currentPage, jingle, range,
                    broadcastLinesUnlocked, broadcastLinesSelected,
                    broadcastIntervalUnlocked, broadcastIntervalSelected,
                    cachedPages, cachedOxidation);
        }
    }
}
