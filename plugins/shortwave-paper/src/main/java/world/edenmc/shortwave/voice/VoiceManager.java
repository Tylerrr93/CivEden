package world.edenmc.shortwave.voice;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerLevel;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import world.edenmc.shortwave.ShortwavePlugin;
import world.edenmc.shortwave.models.RadioTower;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Integrates SimpleVoiceChat so that voice spoken near an active radio tower
 * is relayed through all speaker blocks and handheld radios tuned to the same
 * frequency within the tower's broadcast range.
 *
 * This class is only instantiated when the voicechat plugin is present.
 * Every import here is from the SVC API — if SVC is absent these classes will
 * never be loaded, so Bedrock/vanilla players are completely unaffected.
 */
public class VoiceManager implements VoicechatPlugin {

    private static final String RADIO_CATEGORY = "shortwave_radio";
    private static final Random RANDOM = new Random();

    private final ShortwavePlugin plugin;
    private VoicechatServerApi serverApi;
    private OpusDecoder decoder;
    private OpusEncoder encoder;
    private volatile boolean enabled = false;

    // Speaker copper-block location key → locational audio channel playing from that block
    private final Map<String, LocationalAudioChannel> speakerChannels = new ConcurrentHashMap<>();
    // Speaker copper-block location key → tuned frequency (kept in sync with SpeakerManager)
    private final Map<String, String> speakerFrequencies = new ConcurrentHashMap<>();
    // Speaker copper-block location key → last System.currentTimeMillis() audio was sent to it
    private final Map<String, Long> speakerLastAudioTime = new ConcurrentHashMap<>();
    // Receiver player UUID → persistent static audio channel (reused across packets, same as speaker pattern)
    private final Map<UUID, StaticAudioChannel> listenerChannels = new ConcurrentHashMap<>();

    // Refreshed on the main thread every broadcast tick; consumed from the SVC async thread.
    // Using volatile references to ConcurrentHashMaps gives a safe consistent snapshot.
    private volatile Map<UUID, Location> playerLocationCache = new ConcurrentHashMap<>();
    private volatile Map<UUID, String> playerRadioFreqCache = new ConcurrentHashMap<>();

    private final NamespacedKey frequencyKey;

    public VoiceManager(ShortwavePlugin plugin) {
        this.plugin = plugin;
        this.frequencyKey = new NamespacedKey(plugin, "frequency");
    }

    // ─── VoicechatPlugin ─────────────────────────────────────────────────────

    @Override
    public String getPluginId() {
        return "shortwave";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
    }

    // ─── SVC event handlers ──────────────────────────────────────────────────

    private void onServerStarted(VoicechatServerStartedEvent event) {
        serverApi = event.getVoicechat();
        decoder = serverApi.createDecoder();
        encoder = serverApi.createEncoder();
        enabled = true;

        VolumeCategory category = serverApi.volumeCategoryBuilder()
                .setId(RADIO_CATEGORY)
                .setName("Shortwave Radio")
                .setDescription("Volume of audio received through radio towers and speakers")
                .build();
        serverApi.registerVolumeCategory(category);

        // Speaker channels must be (re-)created after SVC server starts; schedule on main thread
        // because SpeakerManager reads Bukkit worlds.
        Bukkit.getScheduler().runTask(plugin, this::restoreAllSpeakerChannels);

        plugin.getLogger().info("[Shortwave] SimpleVoiceChat integration active.");
    }

    /**
     * Fires on the SVC audio thread (~50 Hz while someone is speaking).
     * Only Bukkit-safe reads via the pre-built caches; no direct Bukkit API calls.
     */
    private void onMicrophonePacket(MicrophonePacketEvent event) {
        if (!enabled || serverApi == null || event.isCancelled()) return;

        VoicechatConnection senderConn = event.getSenderConnection();
        if (senderConn == null) return;

        UUID senderUUID = senderConn.getPlayer().getUuid();
        Location playerLoc = playerLocationCache.get(senderUUID);
        if (playerLoc == null) return;

        // Only relay if the speaker is standing close enough to an active tower with voice on
        RadioTower tower = findNearbyActiveTower(playerLoc);
        if (tower == null || !tower.isVoiceEnabled()) return;

        byte[] opusData = applyGarbling(event.getPacket().getOpusEncodedData(), tower.getOxidationLevel());
        String frequency = tower.getFrequency();

        // ── Relay to speaker blocks ──────────────────────────────────────────
        for (Map.Entry<String, LocationalAudioChannel> entry : speakerChannels.entrySet()) {
            if (!frequency.equals(speakerFrequencies.get(entry.getKey()))) continue;
            Location speakerLoc = keyToLocation(entry.getKey());
            if (speakerLoc == null || !tower.isInRange(speakerLoc)) continue;
            entry.getValue().send(opusData);
            speakerLastAudioTime.put(entry.getKey(), System.currentTimeMillis());
        }

        // ── Relay to players holding a tuned handheld radio ──────────────────
        for (Map.Entry<UUID, String> entry : playerRadioFreqCache.entrySet()) {
            UUID receiverUUID = entry.getKey();
            if (receiverUUID.equals(senderUUID)) continue;
            if (!frequency.equals(entry.getValue())) continue;

            Location receiverLoc = playerLocationCache.get(receiverUUID);
            if (receiverLoc == null || !tower.isInRange(receiverLoc)) continue;

            VoicechatConnection conn = serverApi.getConnectionOf(receiverUUID);
            if (conn == null) {
                listenerChannels.remove(receiverUUID);
                continue;
            }

            // Reuse a persistent channel per receiver — same pattern as speaker blocks.
            // Creating a new StaticAudioChannel every packet breaks the audio stream.
            StaticAudioChannel ch = listenerChannels.computeIfAbsent(receiverUUID, id -> {
                ServerLevel lvl = serverApi.fromServerLevel(receiverLoc.getWorld());
                StaticAudioChannel newCh = serverApi.createStaticAudioChannel(UUID.randomUUID(), lvl, conn);
                if (newCh != null) newCh.setCategory(RADIO_CATEGORY);
                return newCh;
            });

            if (ch != null) ch.send(opusData);
        }
    }

    // ─── Speaker channel management ──────────────────────────────────────────

    /**
     * Call when a speaker is tuned or re-tuned.
     * Safe to call from any thread; channel creation schedules to main if needed.
     */
    public void registerSpeaker(Location copperLoc, String frequency) {
        String key = locationToKey(copperLoc);
        speakerFrequencies.put(key, frequency);
        if (enabled && serverApi != null && !speakerChannels.containsKey(key)) {
            createSpeakerChannel(key, copperLoc);
        }
    }

    /** Call when a speaker block is removed. */
    public void unregisterSpeaker(Location copperLoc) {
        String key = locationToKey(copperLoc);
        speakerFrequencies.remove(key);
        speakerChannels.remove(key);
        speakerLastAudioTime.remove(key);
    }

    /** Rebuilds all speaker channels after SVC server starts (runs on main thread). */
    private void restoreAllSpeakerChannels() {
        speakerChannels.clear();
        Map<Location, String> snapshot = plugin.getSpeakerManager().getSpeakersSnapshot();
        for (Map.Entry<Location, String> entry : snapshot.entrySet()) {
            String key = locationToKey(entry.getKey());
            speakerFrequencies.put(key, entry.getValue());
            createSpeakerChannel(key, entry.getKey());
        }
        plugin.getLogger().info("[Shortwave] Restored " + speakerChannels.size() + " speaker voice channels.");
    }

    private void createSpeakerChannel(String key, Location copperLoc) {
        if (serverApi == null) return;
        World world = copperLoc.getWorld();
        if (world == null) return;

        // Audio emanates from the centre of the decorated pot (1 block above copper base)
        ServerLevel level = serverApi.fromServerLevel(world);
        Position position = serverApi.createPosition(
                copperLoc.getBlockX() + 0.5,
                copperLoc.getBlockY() + 1.5,
                copperLoc.getBlockZ() + 0.5);

        LocationalAudioChannel channel = serverApi.createLocationalAudioChannel(
                UUID.randomUUID(), level, position);
        if (channel != null) {
            channel.setCategory(RADIO_CATEGORY);
            channel.setDistance(plugin.getConfigManager().getSpeakerAudioRange());
            speakerChannels.put(key, channel);
        }
    }

    // ─── Player cache (called from main thread in BroadcastTask) ─────────────

    /**
     * Snapshots player locations and radio tuning for safe consumption from the SVC
     * async thread. Must be called from the main Bukkit thread.
     */
    public void refreshPlayerCaches() {
        if (!enabled) return;

        Material receiverItem = plugin.getConfigManager().getReceiverItem();
        Map<UUID, Location> locs = new ConcurrentHashMap<>();
        Map<UUID, String> freqs = new ConcurrentHashMap<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            locs.put(player.getUniqueId(), player.getLocation());
            String freq = getTunedFrequency(player, receiverItem);
            if (freq != null) freqs.put(player.getUniqueId(), freq);
        }

        playerLocationCache = locs;
        playerRadioFreqCache = freqs;
    }

    private String getTunedFrequency(Player player, Material receiverItem) {
        String freq = freqFromItem(player.getInventory().getItemInMainHand(), receiverItem);
        if (freq != null) return freq;
        freq = freqFromItem(player.getInventory().getItemInOffHand(), receiverItem);
        if (freq != null) return freq;
        int heldSlot = player.getInventory().getHeldItemSlot();
        for (int slot = 0; slot <= 8; slot++) {
            if (slot == heldSlot) continue;
            freq = freqFromItem(player.getInventory().getItem(slot), receiverItem);
            if (freq != null) return freq;
        }
        return null;
    }

    private String freqFromItem(ItemStack item, Material receiverItem) {
        if (item == null || item.getType() != receiverItem || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(frequencyKey, PersistentDataType.STRING);
    }

    // ─── Audio ───────────────────────────────────────────────────────────────

    /** Finds the nearest active tower the player is close enough to transmit through. */
    private RadioTower findNearbyActiveTower(Location playerLoc) {
        double range = plugin.getConfigManager().getVoiceTransmitterRange();
        for (RadioTower tower : plugin.getTowerManager().getActiveTowers()) {
            if (!playerLoc.getWorld().equals(tower.getCopperLocation().getWorld())) continue;
            if (playerLoc.distance(tower.getCopperLocation()) <= range) return tower;
        }
        return null;
    }

    /**
     * Decodes opus data, applies oxidation-level garbling to simulate copper corrosion
     * degrading signal quality, then re-encodes. Returns the original data unchanged
     * if the oxidation level is UNAFFECTED or if codec objects are unavailable.
     */
    private byte[] applyGarbling(byte[] opusData, RadioTower.OxidationLevel oxidation) {
        int garblePercent = oxidation.getGarblePercentage();
        if (garblePercent == 0 || decoder == null || encoder == null || opusData == null) return opusData;

        float intensity = garblePercent / 100.0f;
        try {
            short[] samples = decoder.decode(opusData);
            if (samples == null || samples.length == 0) return opusData;

            double sampleRate = 48000.0;
            for (int i = 0; i < samples.length; i++) {
                float sample = samples[i];

                // Ring modulation — radio warble / alien effect
                double ring = Math.sin(2.0 * Math.PI * 150 * i / sampleRate);
                sample *= (1.0 - intensity * 0.5 + ring * intensity * 0.5);

                // Bit-crush for distortion
                int step = (int) Math.pow(2, 10 + (int) (intensity * 4));
                if (step > 0) sample = (float) (((int) (sample / step)) * step);

                // Random dropout (silence) simulating signal loss at high oxidation
                if (RANDOM.nextFloat() < intensity * 0.3f) sample = 0;

                // Noise floor
                sample += (RANDOM.nextFloat() - 0.5f) * 2000 * intensity;

                samples[i] = (short) Math.max(-32768, Math.min(32767, (int) sample));
            }
            return encoder.encode(samples);
        } catch (Exception e) {
            plugin.getLogger().warning("[Shortwave] Audio garble error: " + e.getMessage());
            return opusData;
        }
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    /**
     * Returns a read-only view of speaker keys → last audio timestamp (millis).
     * Used by ParticleTask to determine which speakers are actively playing audio.
     */
    public Map<String, Long> getSpeakerLastAudioTimes() {
        return speakerLastAudioTime;
    }

    public void shutdown() {
        enabled = false;
        speakerChannels.clear();
        speakerFrequencies.clear();
        speakerLastAudioTime.clear();
        listenerChannels.clear();
        playerLocationCache = new ConcurrentHashMap<>();
        playerRadioFreqCache = new ConcurrentHashMap<>();
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Registers this plugin with the SVC service. Returns true if SVC is present
     * and registration succeeded; false if SVC is not installed.
     */
    public boolean tryRegister() {
        BukkitVoicechatService service = Bukkit.getServicesManager().load(BukkitVoicechatService.class);
        if (service == null) return false;
        service.registerPlugin(this);
        return true;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String locationToKey(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location keyToLocation(String key) {
        String[] p = key.split(",", 4);
        if (p.length != 4) return null;
        World world = Bukkit.getWorld(p[0]);
        if (world == null) return null;
        try {
            return new Location(world, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
