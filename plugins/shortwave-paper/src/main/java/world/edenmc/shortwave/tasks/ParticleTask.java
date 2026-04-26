package world.edenmc.shortwave.tasks;

import world.edenmc.shortwave.ShortwavePlugin;
import world.edenmc.shortwave.models.RadioTower;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.Random;

/**
 * Runs every 4 ticks (~0.2 s) on the main thread and spawns:
 *
 *   • Tower   — slow NOTE particles rising from the lightning rod tip whenever
 *               voice mode is enabled and the tower is active.  Acts as a
 *               visible "on-air" indicator even when no one is speaking.
 *
 *   • Speaker — outward NOTE burst from the decorated pot while audio is
 *               actively flowing (within the last 500 ms according to
 *               VoiceManager's timestamp map).
 *
 * All particle spawning is guarded by chunk-load checks so unloaded areas
 * are never forced loaded.
 */
public class ParticleTask extends BukkitRunnable {

    private static final long SPEAKER_ACTIVE_WINDOW_MS = 500;
    private static final Random RANDOM = new Random();

    private final ShortwavePlugin plugin;
    private long tickCount = 0;

    public ParticleTask(ShortwavePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        tickCount++;

        var vm = plugin.getVoiceManager();
        if (vm == null) return;

        // ── Tower "on-air" indicator ─────────────────────────────────────────
        for (RadioTower tower : plugin.getTowerManager().getAllTowers()) {
            if (!tower.isVoiceEnabled() || !tower.isActive() || !tower.isStructureValid()) continue;
            spawnTowerParticles(tower);
        }

        // ── Speaker audio indicator ──────────────────────────────────────────
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : vm.getSpeakerLastAudioTimes().entrySet()) {
            if (now - entry.getValue() > SPEAKER_ACTIVE_WINDOW_MS) continue;
            Location copperLoc = keyToLocation(entry.getKey());
            if (copperLoc == null) continue;
            spawnSpeakerParticles(copperLoc);
        }
    }

    // ─── Particle shapes ─────────────────────────────────────────────────────

    private void spawnTowerParticles(RadioTower tower) {
        // Lightning rod sits at copper+3; spawn from its tip (+3.8)
        Location rodTip = tower.getCopperLocation().clone().add(0.5, 3.8, 0.5);
        World world = rodTip.getWorld();
        if (world == null || !isChunkLoaded(world, rodTip)) return;

        // One NOTE per tick call, slightly randomised position for organic feel.
        // NOTE particles drift upward on their own, making a rising stream effect.
        double dx = (RANDOM.nextDouble() - 0.5) * 0.35;
        double dz = (RANDOM.nextDouble() - 0.5) * 0.35;
        world.spawnParticle(Particle.NOTE, rodTip.clone().add(dx, 0, dz), 1, 0, 0, 0, 0);
    }

    private void spawnSpeakerParticles(Location copperLoc) {
        // Audio emanates from the decorated pot (1 above copper base)
        Location potCentre = copperLoc.clone().add(0.5, 1.85, 0.5);
        World world = potCentre.getWorld();
        if (world == null || !isChunkLoaded(world, potCentre)) return;

        // Two NOTE particles per call placed on opposite sides of a slowly
        // rotating ring — gives a "soundwave" expanding-outward feel.
        double angle = (tickCount * 0.4) % (2 * Math.PI);
        for (int i = 0; i < 2; i++) {
            double a = angle + i * Math.PI;
            double dx = Math.cos(a) * 0.4;
            double dz = Math.sin(a) * 0.4;
            // Small upward velocity (extra=0.05) so particles arc gently away
            world.spawnParticle(Particle.NOTE, potCentre.clone().add(dx, 0, dz), 1, 0, 0.05, 0, 0);
        }
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private boolean isChunkLoaded(World world, Location loc) {
        return world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }

    /** Parses the "world,x,y,z" key format shared with VoiceManager. */
    private Location keyToLocation(String key) {
        String[] p = key.split(",", 4);
        if (p.length != 4) return null;
        World world = plugin.getServer().getWorld(p[0]);
        if (world == null) return null;
        try {
            return new Location(world, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
