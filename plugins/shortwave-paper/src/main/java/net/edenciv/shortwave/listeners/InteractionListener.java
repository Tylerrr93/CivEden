package net.edenciv.shortwave.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.edenciv.shortwave.ShortwavePlugin;
import net.edenciv.shortwave.models.RadioTower;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import vg.civcraft.mc.citadel.ReinforcementLogic;
import vg.civcraft.mc.citadel.model.Reinforcement;

import java.util.*;

public class InteractionListener implements Listener {
    
    private final ShortwavePlugin plugin;
    private final NamespacedKey frequencyKey;
    private final Map<UUID, PendingAction> pendingActions;
    
    public InteractionListener(ShortwavePlugin plugin) {
        this.plugin = plugin;
        this.frequencyKey = new NamespacedKey(plugin, "frequency");
        this.pendingActions = new HashMap<>();
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        // Check if right-clicking with receiver (tuning radio)
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                && item.getType() == plugin.getConfigManager().getReceiverItem()) {
            event.setCancelled(true);
            handleCompassTuning(player);
            return;
        }
        
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }
        
        // Check if clicking chiseled copper
        if (clicked.getType().name().contains("CHISELED_COPPER")) {
            handleCopperClick(event, player, clicked);
        }
        // Check if clicking lectern
        else if (clicked.getType() == Material.LECTERN) {
            handleLecternClick(event, player, clicked);
        }
        // Check if clicking decorated pot (speaker tuning)
        else if (clicked.getType() == Material.DECORATED_POT) {
            handleSpeakerClick(event, player, clicked);
        }
    }
    
    private void handleCompassTuning(Player player) {
        player.sendMessage(Component.text("Enter frequency to tune radio (e.g., 104.5):", NamedTextColor.YELLOW));
        pendingActions.put(player.getUniqueId(), new PendingAction(PendingAction.Type.TUNE_COMPASS, null));
    }
    
    private boolean hasRadioAccess(Block block, Player player) {
        Reinforcement rein = ReinforcementLogic.getReinforcementProtecting(block);
        if (rein == null) return true;
        return rein.hasPermission(player, plugin.getUseRadioPermission());
    }

    private void handleCopperClick(PlayerInteractEvent event, Player player, Block copper) {
        ItemStack item = player.getInventory().getItemInMainHand();

        // Check if player is shift-clicking with empty hand (open tower GUI)
        if (player.isSneaking() && item.getType() == Material.AIR) {
            event.setCancelled(true);

            if (!hasRadioAccess(copper, player)) {
                player.sendMessage(Component.text("You don't have permission to access this radio tower!", NamedTextColor.RED));
                return;
            }
            
            // Check if valid tower structure (4 blocks high)
            Block lectern = copper.getLocation().clone().add(0, 1, 0).getBlock();
            Block copperTop = copper.getLocation().clone().add(0, 2, 0).getBlock();
            Block rod = copper.getLocation().clone().add(0, 3, 0).getBlock();
            
            if (lectern.getType() != Material.LECTERN || 
                !copperTop.getType().name().contains("CHISELED_COPPER") ||
                rod.getType() != Material.LIGHTNING_ROD) {
                player.sendMessage(Component.text("Invalid tower structure!", NamedTextColor.RED));
                player.sendMessage(Component.text("Required (bottom to top):", NamedTextColor.GRAY));
                player.sendMessage(Component.text("1. Chiseled Copper", NamedTextColor.GRAY));
                player.sendMessage(Component.text("2. Lectern", NamedTextColor.GRAY));
                player.sendMessage(Component.text("3. Chiseled Copper", NamedTextColor.GRAY));
                player.sendMessage(Component.text("4. Lightning Rod", NamedTextColor.GRAY));
                return;
            }
            
            RadioTower tower = plugin.getTowerManager().getTowerByBlock(copper);

            if (tower == null) {
                // Prompt for frequency to create tower
                player.sendMessage(Component.text("Enter frequency (e.g., " +
                        plugin.getConfigManager().getFrequencyFormatExample() + ") in chat:", NamedTextColor.YELLOW));
                player.sendMessage(Component.text("Range: " +
                        plugin.getConfigManager().getMinFrequency() + " - " +
                        plugin.getConfigManager().getMaxFrequency(), NamedTextColor.GRAY));
                pendingActions.put(player.getUniqueId(), new PendingAction(PendingAction.Type.SET_FREQUENCY, copper.getLocation()));
            } else {
                // Chunk is loaded (player is here) — refresh all cached state before opening GUI
                tower.validateStructureFromBlocks();
                tower.refreshOxidationFromBlock();
                tower.refreshBookPages();
                plugin.getGUIManager().openTowerGUI(player, tower);
            }
            return;
        }
        
        // If not shift-clicking, allow normal interactions
        // Players can still right-click with items as before
    }
    
    private void handleLecternClick(PlayerInteractEvent event, Player player, Block lectern) {
        // If this lectern belongs to a tower, refresh cached pages after vanilla places the book
        RadioTower tower = plugin.getTowerManager().getTower(lectern.getLocation().subtract(0, 1, 0));
        if (tower != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                tower.refreshBookPages();
                tower.setCurrentPage(0); // book changed, restart from the beginning
                plugin.getTowerManager().saveTowers();
            }, 1L);
        }
    }
    
    private void handleSpeakerClick(PlayerInteractEvent event, Player player, Block pot) {
        // Check if there's a copper block below
        Location below = pot.getLocation().clone().subtract(0, 1, 0);
        if (!below.getBlock().getType().name().contains("COPPER")) {
            return;
        }

        event.setCancelled(true);

        if (!hasRadioAccess(below.getBlock(), player)) {
            player.sendMessage(Component.text("You don't have permission to access this speaker!", NamedTextColor.RED));
            return;
        }

        // Prompt for frequency
        player.sendMessage(Component.text("Enter frequency to tune speaker (e.g., 104.5):", NamedTextColor.YELLOW));
        pendingActions.put(player.getUniqueId(), new PendingAction(PendingAction.Type.TUNE_SPEAKER, below));
    }
    
    
    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Check for GUI-initiated frequency change
        if (player.hasMetadata("shortwave_pending_frequency")) {
            event.setCancelled(true);

            String message = plugin.getConfigManager().normalizeFrequency(
                    PlainTextComponentSerializer.plainText().serialize(event.message()));

            // Validate frequency using config
            if (!plugin.getConfigManager().isValidFrequency(message)) {
                player.sendMessage(Component.text("Invalid frequency!", NamedTextColor.RED));
                player.sendMessage(Component.text("Format: " + plugin.getConfigManager().getFrequencyFormatExample(), NamedTextColor.GRAY));
                player.sendMessage(Component.text("Range: " + plugin.getConfigManager().getMinFrequency() + 
                        " - " + plugin.getConfigManager().getMaxFrequency(), NamedTextColor.GRAY));
                player.removeMetadata("shortwave_pending_frequency", plugin);
                return;
            }
            
            // Get tower location from metadata
            Location towerLoc = (Location) player.getMetadata("shortwave_pending_frequency").get(0).value();
            player.removeMetadata("shortwave_pending_frequency", plugin);
            
            // Execute on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                RadioTower tower = plugin.getTowerManager().getTower(towerLoc);
                if (tower == null) {
                    player.sendMessage(Component.text("Tower no longer exists!", NamedTextColor.RED));
                    return;
                }
                
                tower.setFrequency(message);
                plugin.getTowerManager().saveTowers();
                
                player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
                player.sendMessage(Component.text("📡 Frequency Changed!", NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true));
                player.sendMessage(Component.text(""));
                player.sendMessage(Component.text("New Frequency: " + message + " MHz", NamedTextColor.GREEN));
                player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
            });
            return;
        }
        
        // Check for old-style pending actions
        if (!pendingActions.containsKey(uuid)) {
            return;
        }
        
        event.setCancelled(true);

        PendingAction action = pendingActions.remove(uuid);
        String message = plugin.getConfigManager().normalizeFrequency(
                PlainTextComponentSerializer.plainText().serialize(event.message()));

        // Validate frequency using config
        if (!plugin.getConfigManager().isValidFrequency(message)) {
            player.sendMessage(Component.text("Invalid frequency!", NamedTextColor.RED));
            player.sendMessage(Component.text("Format: " + plugin.getConfigManager().getFrequencyFormatExample(), NamedTextColor.GRAY));
            player.sendMessage(Component.text("Range: " + plugin.getConfigManager().getMinFrequency() + 
                    " - " + plugin.getConfigManager().getMaxFrequency(), NamedTextColor.GRAY));
            return;
        }
        
        // Execute action on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (action.type == PendingAction.Type.SET_FREQUENCY) {
                handleSetFrequency(player, action.location, message);
            } else if (action.type == PendingAction.Type.TUNE_SPEAKER) {
                handleTuneSpeaker(player, action.location, message);
            } else if (action.type == PendingAction.Type.TUNE_COMPASS) {
                handleTuneCompass(player, message);
            }
        });
    }
    
    private void handleTuneCompass(Player player, String frequency) {
        Material receiverItem = plugin.getConfigManager().getReceiverItem();
        ItemStack compass = player.getInventory().getItemInMainHand();
        if (compass.getType() != receiverItem) {
            player.sendMessage(Component.text("You must be holding a radio receiver!", NamedTextColor.RED));
            return;
        }
        
        ItemMeta meta = compass.getItemMeta();
        meta.getPersistentDataContainer().set(frequencyKey, PersistentDataType.STRING, frequency);
        meta.displayName(Component.text("Radio [" + frequency + "]", NamedTextColor.GOLD));
        compass.setItemMeta(meta);
        
        player.sendMessage(Component.text("Radio tuned to " + frequency, NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
    }
    
    private void handleSetFrequency(Player player, Location copperLoc, String frequency) {
        RadioTower tower = plugin.getTowerManager().getTower(copperLoc);
        
        if (tower == null) {
            // Create new tower
            tower = plugin.getTowerManager().createTower(copperLoc, frequency);
            player.sendMessage(Component.text("Radio tower created on frequency " + frequency, NamedTextColor.GREEN));
        } else {
            // Update existing tower
            tower.setFrequency(frequency);
            player.sendMessage(Component.text("Frequency set to " + frequency, NamedTextColor.GREEN));
        }
        
        plugin.getTowerManager().saveTowers();
    }
    
    private void handleTuneSpeaker(Player player, Location copperLoc, String frequency) {
        plugin.getSpeakerManager().tuneSpeaker(copperLoc, frequency);
        player.sendMessage(Component.text("Speaker tuned to " + frequency, NamedTextColor.GREEN));
        copperLoc.getWorld().playSound(copperLoc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();

        // Base copper block — remove the tower entirely
        if (block.getType().name().contains("CHISELED_COPPER")) {
            RadioTower tower = plugin.getTowerManager().getTower(loc);
            if (tower != null) {
                plugin.getTowerManager().removeTower(loc);
                event.getPlayer().sendMessage(Component.text("Radio tower destroyed!", NamedTextColor.YELLOW));
                return;
            }
        }

        // Upper structure blocks (lectern=+1, top copper=+2, lightning rod=+3) —
        // mark the tower invalid so it stops broadcasting until repaired.
        for (int offset = 1; offset <= 3; offset++) {
            RadioTower tower = plugin.getTowerManager().getTower(loc.clone().subtract(0, offset, 0));
            if (tower != null) {
                tower.setStructureIntact(false);
                event.getPlayer().sendMessage(Component.text("Radio tower structure damaged!", NamedTextColor.YELLOW));
                break;
            }
        }

        // Deregister speaker if the copper base or its pot is broken
        if (block.getType() == Material.DECORATED_POT) {
            plugin.getSpeakerManager().removeSpeaker(loc.clone().subtract(0, 1, 0));
        } else {
            plugin.getSpeakerManager().removeSpeaker(loc);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        pendingActions.remove(event.getPlayer().getUniqueId());
    }

    private static class PendingAction {
        enum Type {
            SET_FREQUENCY,
            TUNE_SPEAKER,
            TUNE_COMPASS
        }
        
        final Type type;
        final Location location;
        
        PendingAction(Type type, Location location) {
            this.type = type;
            this.location = location;
        }
    }
}
