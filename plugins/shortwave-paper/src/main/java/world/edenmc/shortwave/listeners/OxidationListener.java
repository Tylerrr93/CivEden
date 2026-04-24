package world.edenmc.shortwave.listeners;

import world.edenmc.shortwave.ShortwavePlugin;
import world.edenmc.shortwave.models.RadioTower;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFadeEvent;

import java.util.Random;

public class OxidationListener implements Listener {

    private final ShortwavePlugin plugin;
    private final Random random = new Random();

    public OxidationListener(ShortwavePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockFade(BlockFadeEvent event) {
        Block block = event.getBlock();

        if (!block.getType().name().contains("CHISELED_COPPER")) {
            return;
        }

        RadioTower tower = plugin.getTowerManager().getTower(block.getLocation());
        if (tower == null) {
            return;
        }

        // Cancel this oxidation attempt with the configured probability.
        // A roll below cancelChance suppresses the event; above it lets oxidation through.
        double cancelChance = plugin.getConfigManager().getOxidationCancelChance();
        if (random.nextDouble() < cancelChance) {
            event.setCancelled(true);
        } else {
            // Oxidation will proceed — update the cached level from the incoming block state
            // so the broadcast task doesn't need block access to know the current oxidation.
            RadioTower.OxidationLevel newLevel = RadioTower.OxidationLevel
                    .fromMaterialName(event.getNewState().getType().name());
            tower.setCachedOxidation(newLevel);
        }
    }
}
