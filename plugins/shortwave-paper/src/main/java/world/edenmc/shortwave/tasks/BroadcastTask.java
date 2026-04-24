package world.edenmc.shortwave.tasks;

import world.edenmc.shortwave.ShortwavePlugin;
import world.edenmc.shortwave.models.RadioTower;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

public class BroadcastTask extends BukkitRunnable {
    
    private final ShortwavePlugin plugin;
    private final Random random;
    private final NamespacedKey frequencyKey;
    
    public BroadcastTask(ShortwavePlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.frequencyKey = new NamespacedKey(plugin, "frequency");
    }
    
    @Override
    public void run() {
        for (RadioTower tower : plugin.getTowerManager().getActiveTowers()) {
            if (!tower.isReadyToBroadcast()) continue;
            try {
                broadcastTower(tower);
                tower.markBroadcast();
            } catch (Exception e) {
                plugin.getLogger().warning("Error broadcasting from tower at " +
                        tower.getCopperLocation() + ": " + e.getMessage());
            }
        }
    }
    
    private void broadcastTower(RadioTower tower) {
        // All reads below hit cached model state — no block or chunk access needed.

        if (!tower.isStructureValid()) {
            return;
        }
        
        // Flatten all book pages into a single list of non-blank lines
        List<String> flatLines = tower.getFlatLines();
        if (flatLines.isEmpty()) {
            return;
        }

        int maxLines = tower.getBroadcastLinesSelected();

        // Advance through the book as a sliding window over the flat line list
        int start = tower.getCurrentPage();
        if (start >= flatLines.size()) {
            start = 0;
        }
        int end = Math.min(start + maxLines, flatLines.size());
        String rawMessage = String.join("\n", flatLines.subList(start, end));

        // Wrap to 0 when we reach the end of the book
        tower.setCurrentPage(end >= flatLines.size() ? 0 : end);

        // Apply oxidation garbling
        RadioTower.OxidationLevel oxidation = tower.getOxidationLevel();
        String message = applyGarbling(rawMessage, oxidation.getGarblePercentage());
        
        // Broadcast to players
        String frequency = tower.getFrequency();
        
        // Play jingle and send to handheld receivers
        broadcastToHandheld(tower, frequency, message);
        
        // Broadcast to speaker blocks
        broadcastToSpeakers(tower, frequency, message);
    }
    
    private void broadcastToHandheld(RadioTower tower, String frequency, String oxidizedMessage) {
        Material receiverItem = plugin.getConfigManager().getReceiverItem();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!tower.isInRange(player.getLocation())) continue;
            if (!hasTunedReceiver(player, receiverItem, frequency)) continue;

            String finalMessage = applyInterference(oxidizedMessage, tower, player.getLocation());
            player.playSound(player.getLocation(), tower.getJingle(), 0.5f, 1.0f);
            Component chatMessage = Component.text("[📻 " + frequency + "] ", NamedTextColor.GOLD)
                    .append(Component.text(finalMessage, NamedTextColor.WHITE));
            player.sendMessage(chatMessage);
        }
    }

    private boolean hasTunedReceiver(Player player, Material receiverItem, String frequency) {
        if (isTunedTo(player.getInventory().getItemInMainHand(), receiverItem, frequency)) return true;
        if (isTunedTo(player.getInventory().getItemInOffHand(), receiverItem, frequency)) return true;
        int heldSlot = player.getInventory().getHeldItemSlot();
        for (int slot = 0; slot <= 8; slot++) {
            if (slot == heldSlot) continue;
            if (isTunedTo(player.getInventory().getItem(slot), receiverItem, frequency)) return true;
        }
        return false;
    }

    private boolean isTunedTo(ItemStack item, Material receiverItem, String frequency) {
        if (item == null || item.getType() != receiverItem || !item.hasItemMeta()) return false;
        String tuned = item.getItemMeta().getPersistentDataContainer()
                .get(frequencyKey, PersistentDataType.STRING);
        return frequency.equals(tuned);
    }
    
    private void broadcastToSpeakers(RadioTower tower, String frequency, String oxidizedMessage) {
        for (Location copperLoc : plugin.getSpeakerManager().getLoadedSpeakersOnFrequency(frequency)) {
            if (!tower.isInRange(copperLoc)) continue;
            Location potLoc = copperLoc.clone().add(0, 1, 0);
            if (potLoc.getBlock().getType() == Material.DECORATED_POT) {
                String finalMessage = applyInterference(oxidizedMessage, tower, copperLoc);
                handleSpeaker(potLoc, frequency, finalMessage);
            }
        }
    }
    
    private void handleSpeaker(Location potLocation, String frequency, String message) {
        Location displayLoc = potLocation.clone().add(0.5, 1.5, 0.5);
        TextDisplay display = potLocation.getWorld().spawn(displayLoc, TextDisplay.class, entity -> {
            entity.text(Component.text("[" + frequency + "] " + message));
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setBackgroundColor(org.bukkit.Color.fromARGB(128, 0, 0, 0));
            entity.getPersistentDataContainer().set(
                    plugin.getBroadcastKey(), PersistentDataType.BYTE, (byte) 1);
        });
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (display.isValid()) display.remove();
        }, 100L);
    }
    
    private String applyInterference(String text, RadioTower source, Location targetLoc) {
        int competing = 0;
        for (RadioTower other : plugin.getTowerManager().getActiveTowers()) {
            if (other == source) continue;
            if (!other.getFrequency().equals(source.getFrequency())) continue;
            if (!other.getFlatLines().isEmpty() && other.isInRange(targetLoc)) competing++;
        }
        if (competing == 0) return text;
        int interferencePercent = Math.min(90, competing * plugin.getConfigManager().getInterferenceGarblePerTower());
        return applyGarbling(text, interferencePercent);
    }

    private String applyGarbling(String text, int percentage) {
        if (percentage == 0) {
            return text;
        }
        
        if (percentage >= 100) {
            // Completely garbled
            return text.replaceAll(".", "#");
        }
        
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] != ' ' && random.nextInt(100) < percentage) {
                chars[i] = '#';
            }
        }
        
        return new String(chars);
    }
}
