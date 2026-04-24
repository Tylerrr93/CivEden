package world.edenmc.shortwave.models;

import world.edenmc.shortwave.ShortwavePlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

public class RadioTower {

    private final Location copperLocation;
    private String frequency;
    private long fuelEndTime;
    private int currentPage;
    private Sound jingle;
    private int range;
    private int broadcastLinesUnlocked;
    private int broadcastLinesSelected;
    private int broadcastIntervalUnlocked;
    private int broadcastIntervalSelected;
    private transient long lastBroadcastTime;

    // Cached state — updated by events and on GUI open, read by the broadcast task.
    // This avoids block access (and chunk loading) on every broadcast tick.
    private List<String> cachedPages;
    private OxidationLevel cachedOxidation;
    private boolean structureIntact;

    public RadioTower(Location copperLocation, String frequency) {
        this.copperLocation = copperLocation;
        this.frequency = frequency;
        this.fuelEndTime = 0;
        this.currentPage = 0;
        this.jingle = ShortwavePlugin.getInstance().getConfigManager().getDefaultJingle();
        this.range = ShortwavePlugin.getInstance().getConfigManager().getDefaultRange();
        this.broadcastLinesUnlocked = 1;
        this.broadcastLinesSelected = 1;
        this.broadcastIntervalUnlocked = 45;
        this.broadcastIntervalSelected = 45;
        this.cachedPages = new ArrayList<>();
        this.cachedOxidation = OxidationLevel.UNAFFECTED;
        this.structureIntact = true;
    }

    // Constructor for deserialization
    public RadioTower(Location copperLocation, String frequency, long fuelEndTime, int currentPage,
                      String jingleName, int range,
                      int broadcastLinesUnlocked, int broadcastLinesSelected,
                      int broadcastIntervalUnlocked, int broadcastIntervalSelected,
                      List<String> cachedPages, String cachedOxidationName) {
        this.copperLocation = copperLocation;
        this.frequency = frequency;
        this.fuelEndTime = fuelEndTime;
        this.currentPage = currentPage;
        this.range = range;
        this.broadcastLinesUnlocked = broadcastLinesUnlocked > 0 ? broadcastLinesUnlocked : 1;
        this.broadcastLinesSelected = broadcastLinesSelected > 0
                ? Math.min(broadcastLinesSelected, this.broadcastLinesUnlocked) : 1;
        this.broadcastIntervalUnlocked = broadcastIntervalUnlocked > 0 ? broadcastIntervalUnlocked : 45;
        this.broadcastIntervalSelected = broadcastIntervalSelected > 0
                ? Math.max(broadcastIntervalSelected, this.broadcastIntervalUnlocked) : this.broadcastIntervalUnlocked;
        try {
            this.jingle = Sound.valueOf(jingleName);
        } catch (IllegalArgumentException e) {
            this.jingle = Sound.BLOCK_BELL_USE;
        }
        this.cachedPages = cachedPages != null ? new ArrayList<>(cachedPages) : new ArrayList<>();
        try {
            this.cachedOxidation = cachedOxidationName != null
                    ? OxidationLevel.valueOf(cachedOxidationName)
                    : OxidationLevel.UNAFFECTED;
        } catch (IllegalArgumentException e) {
            this.cachedOxidation = OxidationLevel.UNAFFECTED;
        }
        this.structureIntact = true;
    }

    public Location getCopperLocation() {
        return copperLocation;
    }

    public Location getLecternLocation() {
        return copperLocation.clone().add(0, 1, 0);
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
        this.currentPage = 0;
    }

    public long getFuelEndTime() {
        return fuelEndTime;
    }

    public void addFuel(int seconds) {
        long now = System.currentTimeMillis();
        if (fuelEndTime < now) {
            fuelEndTime = now + (seconds * 1000L);
        } else {
            fuelEndTime += (seconds * 1000L);
        }
    }

    public boolean isActive() {
        return System.currentTimeMillis() < fuelEndTime;
    }

    public int getRemainingFuelSeconds() {
        long remaining = fuelEndTime - System.currentTimeMillis();
        return remaining > 0 ? (int) (remaining / 1000) : 0;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int page) {
        this.currentPage = page;
    }

    public Sound getJingle() {
        return jingle;
    }

    public void setJingle(Sound jingle) {
        this.jingle = jingle;
    }

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
    }

    public int getBroadcastLinesUnlocked() {
        return broadcastLinesUnlocked;
    }

    public void setBroadcastLinesUnlocked(int max) {
        this.broadcastLinesUnlocked = Math.max(1, Math.min(max, 6));
        if (broadcastLinesSelected > this.broadcastLinesUnlocked) {
            broadcastLinesSelected = this.broadcastLinesUnlocked;
        }
    }

    public int getBroadcastLinesSelected() {
        return broadcastLinesSelected;
    }

    public void setBroadcastLinesSelected(int lines) {
        this.broadcastLinesSelected = Math.max(1, Math.min(lines, broadcastLinesUnlocked));
    }

    public int getBroadcastIntervalUnlocked() {
        return broadcastIntervalUnlocked;
    }

    public void setBroadcastIntervalUnlocked(int minSeconds) {
        this.broadcastIntervalUnlocked = Math.max(1, minSeconds);
        if (broadcastIntervalSelected < this.broadcastIntervalUnlocked) {
            broadcastIntervalSelected = this.broadcastIntervalUnlocked;
        }
    }

    public int getBroadcastIntervalSelected() {
        return broadcastIntervalSelected;
    }

    public void setBroadcastIntervalSelected(int seconds) {
        this.broadcastIntervalSelected = Math.max(broadcastIntervalUnlocked, Math.min(seconds, 300));
    }

    public boolean isReadyToBroadcast() {
        return System.currentTimeMillis() - lastBroadcastTime >= broadcastIntervalSelected * 1000L;
    }

    public void markBroadcast() {
        lastBroadcastTime = System.currentTimeMillis();
    }

    public boolean isInRange(Location location) {
        if (!location.getWorld().equals(copperLocation.getWorld())) {
            return false;
        }
        return location.distance(copperLocation) <= range;
    }

    // --- Cached state accessors (used by broadcast path — no block access) ---

    /** Returns the cached structure state. Updated by events and on GUI open. */
    public boolean isStructureValid() {
        return structureIntact;
    }

    public void setStructureIntact(boolean intact) {
        this.structureIntact = intact;
    }

    /** Returns the cached book pages. Updated when a book is placed and on GUI open. */
    public List<String> getBookPages() {
        return new ArrayList<>(cachedPages);
    }

    /**
     * Flattens all book pages into a single ordered list of non-blank lines.
     * This is what the broadcast task iterates — blank lines are ignored entirely.
     */
    public List<String> getFlatLines() {
        List<String> flat = new ArrayList<>();
        for (String page : cachedPages) {
            for (String line : page.split("\n")) {
                if (!line.trim().isEmpty()) {
                    flat.add(line);
                }
            }
        }
        return flat;
    }

    public List<String> getCachedPages() {
        return cachedPages;
    }

    /** Returns the cached oxidation level. Updated by OxidationListener and on GUI open. */
    public OxidationLevel getOxidationLevel() {
        return cachedOxidation;
    }

    public OxidationLevel getCachedOxidation() {
        return cachedOxidation;
    }

    public void setCachedOxidation(OxidationLevel level) {
        this.cachedOxidation = level;
    }

    // --- Block-reading refresh methods (call only when chunk is known loaded) ---

    /**
     * Reads the four structure blocks and updates {@code structureIntact}.
     * Call this when a player opens the tower GUI or on chunk load for this tower's chunk.
     */
    public boolean validateStructureFromBlocks() {
        Block copper = copperLocation.getBlock();
        if (!isChiseledCopper(copper.getType())) {
            structureIntact = false;
            return false;
        }
        Block lectern = copperLocation.clone().add(0, 1, 0).getBlock();
        if (lectern.getType() != Material.LECTERN) {
            structureIntact = false;
            return false;
        }
        Block copperTop = copperLocation.clone().add(0, 2, 0).getBlock();
        if (!isChiseledCopper(copperTop.getType())) {
            structureIntact = false;
            return false;
        }
        Block rod = copperLocation.clone().add(0, 3, 0).getBlock();
        structureIntact = rod.getType() == Material.LIGHTNING_ROD;
        return structureIntact;
    }

    /**
     * Reads the lectern's book and updates the cached pages list.
     * Call this after a book is placed/removed and on GUI open.
     * Returns the (now current) page list.
     */
    public List<String> refreshBookPages() {
        ItemStack book = getBook();
        if (book == null || !book.hasItemMeta() || !(book.getItemMeta() instanceof BookMeta meta)) {
            cachedPages = new ArrayList<>();
        } else {
            cachedPages = new ArrayList<>(meta.getPages());
        }
        return cachedPages;
    }

    /**
     * Reads the copper block's material and updates the cached oxidation level.
     * Call this on GUI open; OxidationListener keeps it current between opens.
     */
    public OxidationLevel refreshOxidationFromBlock() {
        String typeName = copperLocation.getBlock().getType().name();
        if (typeName.contains("OXIDIZED")) {
            cachedOxidation = OxidationLevel.OXIDIZED;
        } else if (typeName.contains("WEATHERED")) {
            cachedOxidation = OxidationLevel.WEATHERED;
        } else if (typeName.contains("EXPOSED")) {
            cachedOxidation = OxidationLevel.EXPOSED;
        } else {
            cachedOxidation = OxidationLevel.UNAFFECTED;
        }
        return cachedOxidation;
    }

    /**
     * Gets the physical book item from the lectern block.
     * Only call when the chunk is loaded (e.g. from GUI context).
     */
    public ItemStack getBook() {
        Block lecternBlock = getLecternLocation().getBlock();
        if (lecternBlock.getType() != Material.LECTERN) {
            return null;
        }
        Lectern lectern = (Lectern) lecternBlock.getState();
        return lectern.getInventory().getItem(0);
    }

    private boolean isChiseledCopper(Material material) {
        return material.name().contains("CHISELED_COPPER");
    }

    public enum OxidationLevel {
        UNAFFECTED(0),
        EXPOSED(10),
        WEATHERED(30),
        OXIDIZED(100);

        private final int garblePercentage;

        OxidationLevel(int garblePercentage) {
            this.garblePercentage = garblePercentage;
        }

        public int getGarblePercentage() {
            return garblePercentage;
        }

        public static OxidationLevel fromMaterialName(String name) {
            if (name.contains("OXIDIZED")) return OXIDIZED;
            if (name.contains("WEATHERED")) return WEATHERED;
            if (name.contains("EXPOSED")) return EXPOSED;
            return UNAFFECTED;
        }
    }
}
