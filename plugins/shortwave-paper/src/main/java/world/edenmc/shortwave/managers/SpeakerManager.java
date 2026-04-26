package world.edenmc.shortwave.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import world.edenmc.shortwave.ShortwavePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpeakerManager {

    private final ShortwavePlugin plugin;
    private final Map<String, SpeakerEntry> speakers; // key: "world,x,y,z" of copper block
    private final File speakersFile;
    private final Gson gson;

    public SpeakerManager(ShortwavePlugin plugin) {
        this.plugin = plugin;
        this.speakers = new ConcurrentHashMap<>();
        this.speakersFile = new File(plugin.getDataFolder(), "speakers.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void tuneSpeaker(Location copperLocation, String frequency) {
        speakers.put(locationToKey(copperLocation), new SpeakerEntry(copperLocation, frequency));
        save();
        var vm = plugin.getVoiceManager();
        if (vm != null) vm.registerSpeaker(copperLocation, frequency);
    }

    public void removeSpeaker(Location copperLocation) {
        if (speakers.remove(locationToKey(copperLocation)) != null) {
            save();
            var vm = plugin.getVoiceManager();
            if (vm != null) vm.unregisterSpeaker(copperLocation);
        }
    }

    /**
     * Returns the copper block locations of all speakers tuned to the given frequency
     * whose chunk is currently loaded. Unloaded speakers are intentionally skipped.
     */
    public List<Location> getLoadedSpeakersOnFrequency(String frequency) {
        List<Location> result = new ArrayList<>();
        for (SpeakerEntry entry : speakers.values()) {
            if (!frequency.equals(entry.frequency)) continue;
            Location loc = entry.toLocation();
            if (loc == null) continue;
            if (loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                result.add(loc);
            }
        }
        return result;
    }

    /** Returns a snapshot of all registered speakers (copper location → frequency). */
    public java.util.Map<Location, String> getSpeakersSnapshot() {
        java.util.Map<Location, String> snapshot = new java.util.HashMap<>();
        for (SpeakerEntry entry : speakers.values()) {
            Location loc = entry.toLocation();
            if (loc != null) snapshot.put(loc, entry.frequency);
        }
        return snapshot;
    }

    public void save() {
        try {
            if (!speakersFile.exists()) {
                speakersFile.getParentFile().mkdirs();
                speakersFile.createNewFile();
            }
            try (Writer writer = new FileWriter(speakersFile)) {
                gson.toJson(new ArrayList<>(speakers.values()), writer);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save speakers: " + e.getMessage());
        }
    }

    public void load() {
        if (!speakersFile.exists()) return;
        try (Reader reader = new FileReader(speakersFile)) {
            Type listType = new TypeToken<List<SpeakerEntry>>() {}.getType();
            List<SpeakerEntry> list = gson.fromJson(reader, listType);
            if (list != null) {
                for (SpeakerEntry entry : list) {
                    if (entry.world != null && entry.frequency != null) {
                        speakers.put(entry.locationKey(), entry);
                    }
                }
                plugin.getLogger().info("Loaded " + speakers.size() + " speakers");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load speakers: " + e.getMessage());
        }
    }

    private String locationToKey(Location loc) {
        return String.format("%s,%d,%d,%d",
                loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private static class SpeakerEntry {
        String world;
        int x, y, z;
        String frequency;

        SpeakerEntry(Location loc, String frequency) {
            this.world = loc.getWorld().getName();
            this.x = loc.getBlockX();
            this.y = loc.getBlockY();
            this.z = loc.getBlockZ();
            this.frequency = frequency;
        }

        String locationKey() {
            return world + "," + x + "," + y + "," + z;
        }

        Location toLocation() {
            World w = Bukkit.getWorld(world);
            return w != null ? new Location(w, x, y, z) : null;
        }
    }
}
