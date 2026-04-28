package world.edenmc.shortwave.managers;

import world.edenmc.shortwave.ShortwavePlugin;
import world.edenmc.shortwave.models.RadioTower;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import vg.civcraft.mc.civmodcore.inventory.gui.Clickable;
import vg.civcraft.mc.civmodcore.inventory.gui.ClickableInventory;
import vg.civcraft.mc.civmodcore.inventory.gui.DecorationStack;

import vg.civcraft.mc.civmodcore.utilities.cooldowns.MilliSecCoolDownHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class GUIManager {

    private final ShortwavePlugin plugin;
    private final MilliSecCoolDownHandler<UUID> clickCooldown = new MilliSecCoolDownHandler<>(300);

    public GUIManager(ShortwavePlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isOnCooldown(Player player) {
        if (clickCooldown.onCoolDown(player.getUniqueId())) return true;
        clickCooldown.putOnCoolDown(player.getUniqueId());
        return false;
    }

    private Clickable makeButton(ItemStack item, Consumer<Player> action) {
        return new Clickable(item) {
            @Override protected void clicked(Player p) { if (!isOnCooldown(p)) action.accept(p); }
            @Override protected void onDoubleClick(Player p) {}
            @Override protected void onRightClick(Player p) {}
            @Override protected void onMiddleClick(Player p) {}
            @Override protected void onDrop(Player p) {}
            @Override protected void onControlDrop(Player p) {}
        };
    }

    public void openTowerGUI(Player player, RadioTower tower) {
        String title = plugin.getConfigManager().getTowerGuiTitle().replace("&", "§");
        ClickableInventory gui = new ClickableInventory(27, title);

        // Store tower location so InteractionListener's chat handler can find it for frequency changes
        player.setMetadata("shortwave_tower_location",
                new FixedMetadataValue(plugin, tower.getCopperLocation()));

        gui.setSlot(new DecorationStack(createTowerInfoItem(tower)), 4);
        gui.setSlot(makeButton(createFuelManagementItem(tower), p -> openFuelGUI(p, tower)), 10);
        gui.setSlot(makeButton(createJingleSelectionItem(), p -> openJingleGUI(p, tower)), 12);
        gui.setSlot(makeButton(createRangeUpgradeItem(tower), p -> openRangeUpgradeGUI(p, tower)), 14);
        gui.setSlot(makeButton(createFrequencyItem(tower), p -> {
            p.closeInventory();
            p.sendMessage(Component.text("Enter new frequency (e.g., 104.5) in chat:", NamedTextColor.YELLOW));
            p.setMetadata("shortwave_pending_frequency",
                    new FixedMetadataValue(plugin, tower.getCopperLocation()));
        }), 16);
        gui.setSlot(makeButton(createBroadcastIntervalItem(tower), p -> openBroadcastIntervalGUI(p, tower)), 20);
        gui.setSlot(makeButton(createBroadcastLineItem(tower), p -> openBroadcastLineGUI(p, tower)), 22);

        // Voice toggle — only shown when SimpleVoiceChat is installed
        if (plugin.getVoiceManager() != null) {
            gui.setSlot(makeButton(createVoiceToggleItem(tower), p -> {
                // Capture new state once so every reference in this handler is consistent
                boolean nowEnabled = !tower.isVoiceEnabled();
                tower.setVoiceEnabled(nowEnabled);
                plugin.getTowerManager().saveTowers();

                // Always send a chat message — definitive feedback visible to the player
                // regardless of any GUI timing quirks.
                p.sendMessage(Component.text(
                        "Voice mode " + (nowEnabled ? "enabled" : "disabled")
                                + " on frequency " + tower.getFrequency() + ".",
                        nowEnabled ? NamedTextColor.GREEN : NamedTextColor.GRAY));

                // Pitch-shifted pling: high = on, low = off.
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, nowEnabled ? 1.5f : 0.8f);

                openTowerGUI(p, tower);
            }), 24);
        }

        gui.setSlot(makeButton(createCloseButton(), p -> {
            p.closeInventory();
            p.removeMetadata("shortwave_tower_location", plugin);
        }), 26);

        gui.showInventory(player);
    }

    public void openFuelGUI(Player player, RadioTower tower) {
        String title = plugin.getConfigManager().getFuelGuiTitle().replace("&", "§");
        ClickableInventory gui = new ClickableInventory(54, title);

        gui.setSlot(new DecorationStack(createFuelStatusItem(tower)), 4);
        gui.setSlot(new DecorationStack(createFuelInfoItem()), 22);

        // Primary fuel at slot 20
        Material primaryFuel = plugin.getConfigManager().getFuelItem();
        int primaryDuration = plugin.getConfigManager().getFuelDuration();
        gui.setSlot(createFuelClickable(primaryFuel, primaryDuration, tower), 20);

        // Alternate fuels at slots 21-24
        int slot = 21;
        for (Map.Entry<Material, ConfigManager.FuelConfig> entry :
                plugin.getConfigManager().getAlternateFuels().entrySet()) {
            if (slot > 24) break;
            gui.setSlot(createFuelClickable(entry.getKey(), entry.getValue().duration, tower), slot++);
        }

        gui.setSlot(makeButton(createBackButton(), p -> openTowerGUI(p, tower)), 45);

        DecorationStack border = new DecorationStack(createGlassPane());
        for (int i : new int[]{0,1,2,3,5,6,7,8,9,17,18,26,27,35,36,44,46,47,48,49,50,51,52,53}) {
            gui.setSlot(border, i);
        }

        gui.showInventory(player);
    }

    public void openJingleGUI(Player player, RadioTower tower) {
        String title = plugin.getConfigManager().getJingleGuiTitle().replace("&", "§");
        ClickableInventory gui = new ClickableInventory(27, title);

        List<ConfigManager.JingleConfig> jingles = plugin.getConfigManager().getJingles();
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        int slotIndex = 0;

        for (ConfigManager.JingleConfig jingle : jingles) {
            if (slotIndex >= slots.length) break;
            Sound sound = jingle.sound;
            gui.setSlot(makeButton(createJingleItem(jingle), p -> {
                tower.setJingle(sound);
                plugin.getTowerManager().saveTowers();
                p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
                p.sendMessage(Component.text("Jingle set!", NamedTextColor.GREEN));
                openTowerGUI(p, tower);
            }), slots[slotIndex++]);
        }

        gui.setSlot(makeButton(createBackButton(), p -> openTowerGUI(p, tower)), 22);

        DecorationStack border = new DecorationStack(createGlassPane());
        for (int i : new int[]{0,1,2,3,4,5,6,7,8,9,17,18,20,21,23,24,25,26}) {
            if (gui.getSlot(i) == null) {
                gui.setSlot(border, i);
            }
        }

        gui.showInventory(player);
    }

    public void openRangeUpgradeGUI(Player player, RadioTower tower) {
        String title = plugin.getConfigManager().getUpgradeGuiTitle().replace("&", "§");
        ClickableInventory gui = new ClickableInventory(27, title);

        ConfigManager.RangeUpgrade currentUpgrade = plugin.getConfigManager().getUpgradeByRange(tower.getRange());
        ConfigManager.RangeUpgrade nextUpgrade = plugin.getConfigManager().getNextUpgrade(tower.getRange());

        gui.setSlot(new DecorationStack(createCurrentTierItem(currentUpgrade)), 11);

        if (nextUpgrade != null) {
            gui.setSlot(new DecorationStack(createUpgradeArrow()), 13);
            gui.setSlot(makeButton(createNextTierItem(nextUpgrade), p -> performUpgrade(p, tower)), 15);
        } else {
            gui.setSlot(new DecorationStack(createMaxTierItem()), 13);
        }

        gui.setSlot(new DecorationStack(createTierInfoItem(currentUpgrade, nextUpgrade)), 22);
        gui.setSlot(makeButton(createBackButton(), p -> openTowerGUI(p, tower)), 18);

        DecorationStack border = new DecorationStack(createGlassPane());
        for (int i : new int[]{0,1,2,3,4,5,6,7,8,9,10,12,14,16,17,19,20,21,23,24,25,26}) {
            gui.setSlot(border, i);
        }

        gui.showInventory(player);
    }

    private Clickable createFuelClickable(Material material, int durationSeconds, RadioTower tower) {
        return new Clickable(createFuelClickableItem(material, durationSeconds)) {
            @Override
            public void clicked(Player p) {
                if (!isOnCooldown(p)) consumeFuel(p, tower, material, durationSeconds, false);
            }

            @Override
            protected void onShiftLeftClick(Player p) {
                if (!isOnCooldown(p)) consumeFuel(p, tower, material, durationSeconds, true);
            }

            @Override
            protected void onShiftRightClick(Player p) {
                if (!isOnCooldown(p)) consumeFuel(p, tower, material, durationSeconds, true);
            }

            @Override protected void onDoubleClick(Player p) {}
            @Override protected void onRightClick(Player p) {}
            @Override protected void onMiddleClick(Player p) {}
            @Override protected void onDrop(Player p) {}
            @Override protected void onControlDrop(Player p) {}
        };
    }

    private void consumeFuel(Player player, RadioTower tower, Material fuelType, int duration, boolean all) {
        int amountInInventory = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == fuelType) {
                amountInInventory += item.getAmount();
            }
        }

        if (amountInInventory == 0) {
            player.sendMessage(Component.text("You don't have any " +
                    fuelType.name().replace("_", " ") + " in your inventory!", NamedTextColor.RED));
            return;
        }

        int toConsume = all ? amountInInventory : 1;

        int remaining = toConsume;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == fuelType && remaining > 0) {
                int stackAmount = item.getAmount();
                if (stackAmount <= remaining) {
                    remaining -= stackAmount;
                    item.setAmount(0);
                } else {
                    item.setAmount(stackAmount - remaining);
                    remaining = 0;
                }
            }
        }

        tower.addFuel(duration * toConsume);
        plugin.getTowerManager().saveTowers();

        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1.0f, 1.0f);
        player.sendMessage(Component.text("Added " + toConsume + "x fuel! Total runtime: " +
                formatTime(tower.getRemainingFuelSeconds()), NamedTextColor.GREEN));

        openFuelGUI(player, tower);
    }

    private void performUpgrade(Player player, RadioTower tower) {
        ConfigManager.RangeUpgrade nextUpgrade = plugin.getConfigManager().getNextUpgrade(tower.getRange());

        if (nextUpgrade == null) {
            player.sendMessage(Component.text("Tower is already at maximum tier!", NamedTextColor.YELLOW));
            return;
        }

        Material upgradeType = nextUpgrade.material;
        int cost = nextUpgrade.cost;

        int amountInInventory = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == upgradeType) {
                amountInInventory += item.getAmount();
            }
        }

        if (amountInInventory < cost) {
            player.sendMessage(Component.text("You need " + cost + "x " +
                    upgradeType.name().replace("_", " ") + " to upgrade! You have " +
                    amountInInventory, NamedTextColor.RED));
            return;
        }

        int remaining = cost;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == upgradeType && remaining > 0) {
                int stackAmount = item.getAmount();
                if (stackAmount <= remaining) {
                    remaining -= stackAmount;
                    item.setAmount(0);
                } else {
                    item.setAmount(stackAmount - remaining);
                    remaining = 0;
                }
            }
        }

        tower.setRange(nextUpgrade.range);
        plugin.getTowerManager().saveTowers();

        tower.getCopperLocation().getWorld().spawnParticle(Particle.END_ROD,
                tower.getCopperLocation().clone().add(0.5, 1, 0.5),
                30, 0.3, 0.5, 0.3, 0.1);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.5f);

        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
        player.sendMessage(Component.text("⚡ TOWER UPGRADED! ⚡", NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("New Tier: " + nextUpgrade.displayName.replace("&", "§"),
                NamedTextColor.YELLOW));
        player.sendMessage(Component.text("New Range: " + nextUpgrade.range + " blocks",
                NamedTextColor.GREEN));
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));

        openRangeUpgradeGUI(player, tower);
    }

    // Item creation helpers

    private ItemStack createTowerInfoItem(RadioTower tower) {
        ItemStack item = new ItemStack(Material.LECTERN);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("📻 Tower Information", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Frequency: " + tower.getFrequency(), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Range: " + tower.getRange() + " blocks", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Status: " + (tower.isActive() ? "Active" : "No Fuel"),
                tower.isActive() ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFuelManagementItem(RadioTower tower) {
        ItemStack item = new ItemStack(plugin.getConfigManager().getFuelItem());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("⛽ Fuel Management", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        int remaining = tower.getRemainingFuelSeconds();
        lore.add(Component.text("Fuel Remaining:", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(formatTime(remaining), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("Click to manage fuel", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createJingleSelectionItem() {
        ItemStack item = new ItemStack(Material.NOTE_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("🎵 Station Jingle", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Select your broadcast sound", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("Click to choose", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRangeUpgradeItem(RadioTower tower) {
        ItemStack item = new ItemStack(Material.DIAMOND_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("⚡ Range Upgrade", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Current Range: " + tower.getRange() + " blocks", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("Click to upgrade", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFrequencyItem(RadioTower tower) {
        ItemStack item = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("📡 Frequency: " + tower.getFrequency(), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Current: " + tower.getFrequency() + " MHz", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("Click to change frequency", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("(You'll type in chat)", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFuelStatusItem(RadioTower tower) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        int remaining = tower.getRemainingFuelSeconds();
        meta.displayName(Component.text("⏰ Fuel Status", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Remaining: " + formatTime(remaining),
                remaining > 0 ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("Click fuel items below to add them", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFuelClickableItem(Material material, int durationSeconds) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        String name = material.name().replace("_", " ");
        meta.displayName(Component.text("⛽ Add " + name, NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Fuel Duration: " + formatTime(durationSeconds), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("Left-Click: Add 1 from inventory", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Shift-Click: Add all from inventory", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFuelInfoItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("ℹ How to Add Fuel", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Click the glowing fuel items above", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("They will take fuel from your inventory", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("Left-click: Add 1 item", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Shift-click: Add all from inventory", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createJingleItem(ConfigManager.JingleConfig jingle) {
        ItemStack item = new ItemStack(Material.MUSIC_DISC_13);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(jingle.displayName.replace("&", "§"), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(jingle.description, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("Click to select", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCurrentTierItem(ConfigManager.RangeUpgrade upgrade) {
        ItemStack item = new ItemStack(upgrade.material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("✓ Current Tier: " + upgrade.displayName.replace("&", "§"),
                NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Tier " + upgrade.tier, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Range: " + upgrade.range + " blocks", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("This is your current upgrade tier", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createUpgradeArrow() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("→ Upgrade Available →", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Click the next tier to upgrade!", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNextTierItem(ConfigManager.RangeUpgrade upgrade) {
        ItemStack item = new ItemStack(upgrade.material, upgrade.cost);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("⚡ Next Tier: " + upgrade.displayName.replace("&", "§"),
                NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Tier " + upgrade.tier, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Range: " + upgrade.range + " blocks", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("Cost: " + upgrade.cost + "x " +
                upgrade.material.name().replace("_", " "), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("Click to upgrade!", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("(Checks your inventory)", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createMaxTierItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("★ MAX TIER REACHED ★", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Your tower is fully upgraded!", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("No more upgrades available", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTierInfoItem(ConfigManager.RangeUpgrade current, ConfigManager.RangeUpgrade next) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("ℹ Tier Progression", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Current: " + current.displayName.replace("&", "§"), NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("→ Tier " + current.tier + " | " + current.range + " blocks",
                NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        if (next != null) {
            lore.add(Component.text("Next: " + next.displayName.replace("&", "§"), NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("→ Tier " + next.tier + " | " + next.range + " blocks",
                    NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(""));
            lore.add(Component.text("You must upgrade sequentially", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Cannot skip tiers!", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("No upgrades remaining", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void openBroadcastLineGUI(Player player, RadioTower tower) {
        String title = plugin.getConfigManager().getBroadcastLineGuiTitle().replace("&", "§");
        ClickableInventory gui = new ClickableInventory(27, title);

        int unlocked = tower.getBroadcastLinesUnlocked();
        int selected = tower.getBroadcastLinesSelected();
        ConfigManager.BroadcastLineUpgrade nextUpgrade =
                plugin.getConfigManager().getNextBroadcastLineUpgrade(unlocked);

        // Row 0: current tier summary
        gui.setSlot(new DecorationStack(createBroadcastLinesTierItem(unlocked)), 4);

        // Row 1: configure controls
        gui.setSlot(makeButton(createDecreaseLinesButton(selected), p -> {
            tower.setBroadcastLinesSelected(selected - 1);
            plugin.getTowerManager().saveTowers();
            openBroadcastLineGUI(p, tower);
        }), 11);
        gui.setSlot(new DecorationStack(createSelectedLinesDisplay(selected, unlocked)), 13);
        gui.setSlot(makeButton(createIncreaseLinesButton(selected, unlocked), p -> {
            tower.setBroadcastLinesSelected(selected + 1);
            plugin.getTowerManager().saveTowers();
            openBroadcastLineGUI(p, tower);
        }), 15);

        // Row 2: upgrade path
        if (nextUpgrade != null) {
            gui.setSlot(makeButton(createNextBroadcastLineItem(nextUpgrade),
                    p -> performBroadcastLineUpgrade(p, tower)), 22);
        } else {
            gui.setSlot(new DecorationStack(createMaxBroadcastLineItem()), 22);
        }

        gui.setSlot(makeButton(createBackButton(), p -> openTowerGUI(p, tower)), 18);

        DecorationStack border = new DecorationStack(createGlassPane());
        for (int i : new int[]{0,1,2,3,5,6,7,8,9,10,12,14,16,17,19,20,21,23,24,25,26}) {
            gui.setSlot(border, i);
        }

        gui.showInventory(player);
    }

    private void performBroadcastLineUpgrade(Player player, RadioTower tower) {
        ConfigManager.BroadcastLineUpgrade nextUpgrade =
                plugin.getConfigManager().getNextBroadcastLineUpgrade(tower.getBroadcastLinesUnlocked());

        if (nextUpgrade == null) {
            player.sendMessage(Component.text("Tower is already at maximum broadcast lines!", NamedTextColor.YELLOW));
            return;
        }

        Material upgradeType = nextUpgrade.material;
        int cost = nextUpgrade.cost;

        int amountInInventory = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == upgradeType) {
                amountInInventory += item.getAmount();
            }
        }

        if (amountInInventory < cost) {
            player.sendMessage(Component.text("You need " + cost + "x " +
                    upgradeType.name().replace("_", " ") + " to upgrade! You have " +
                    amountInInventory, NamedTextColor.RED));
            return;
        }

        int remaining = cost;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == upgradeType && remaining > 0) {
                int stackAmount = item.getAmount();
                if (stackAmount <= remaining) {
                    remaining -= stackAmount;
                    item.setAmount(0);
                } else {
                    item.setAmount(stackAmount - remaining);
                    remaining = 0;
                }
            }
        }

        tower.setBroadcastLinesUnlocked(nextUpgrade.lines);
        plugin.getTowerManager().saveTowers();

        tower.getCopperLocation().getWorld().spawnParticle(Particle.END_ROD,
                tower.getCopperLocation().clone().add(0.5, 1, 0.5),
                20, 0.3, 0.5, 0.3, 0.1);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);

        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
        player.sendMessage(Component.text("📄 BROADCAST LINES UPGRADED!", NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("New Level: " + nextUpgrade.displayName.replace("&", "§"),
                NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Max Lines Unlocked: " + nextUpgrade.lines,
                NamedTextColor.GREEN));
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));

        openBroadcastLineGUI(player, tower);
    }

    private ItemStack createBroadcastLineItem(RadioTower tower) {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("📄 Broadcast Lines", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Active: " + tower.getBroadcastLinesSelected() + " line(s)", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Unlocked Max: " + tower.getBroadcastLinesUnlocked(), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("Click to configure / upgrade", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBroadcastLinesTierItem(int unlocked) {
        ConfigManager.BroadcastLineUpgrade upgrade =
                plugin.getConfigManager().getBroadcastLineUpgradeByLines(unlocked);
        ItemStack item = new ItemStack(upgrade.material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("✓ Unlocked: " + upgrade.displayName.replace("&", "§"),
                NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Max lines available: " + unlocked, NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Use the controls below to configure", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSelectedLinesDisplay(int selected, int unlocked) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Lines: " + selected, NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Broadcasting " + selected + " line(s) per cycle", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Range: 1 – " + unlocked, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDecreaseLinesButton(int selected) {
        boolean canDecrease = selected > 1;
        ItemStack item = new ItemStack(canDecrease ? Material.RED_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName((canDecrease
                ? Component.text("◀ Fewer Lines (-1)", NamedTextColor.RED)
                : Component.text("◀ Already at minimum", NamedTextColor.DARK_GRAY))
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createIncreaseLinesButton(int selected, int unlocked) {
        boolean canIncrease = selected < unlocked;
        ItemStack item = new ItemStack(canIncrease ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName((canIncrease
                ? Component.text("More Lines (+1) ▶", NamedTextColor.GREEN)
                : Component.text("At unlocked maximum ▶", NamedTextColor.DARK_GRAY))
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNextBroadcastLineItem(ConfigManager.BroadcastLineUpgrade upgrade) {
        ItemStack item = new ItemStack(upgrade.material, Math.max(1, upgrade.cost));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("⚡ Next: " + upgrade.displayName.replace("&", "§"),
                NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Level " + upgrade.level, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Lines per Broadcast: " + upgrade.lines, NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("Cost: " + upgrade.cost + "x " +
                upgrade.material.name().replace("_", " "), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("Click to upgrade!", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("(Checks your inventory)", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createMaxBroadcastLineItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("★ MAX LINES REACHED ★", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Broadcast lines are fully upgraded!", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("No more upgrades available", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    public void openBroadcastIntervalGUI(Player player, RadioTower tower) {
        String title = plugin.getConfigManager().getBroadcastIntervalGuiTitle().replace("&", "§");
        ClickableInventory gui = new ClickableInventory(27, title);

        int unlocked = tower.getBroadcastIntervalUnlocked();
        int selected = tower.getBroadcastIntervalSelected();
        ConfigManager.BroadcastIntervalUpgrade nextUpgrade =
                plugin.getConfigManager().getNextBroadcastIntervalUpgrade(unlocked);

        // Row 0: current tier summary
        gui.setSlot(new DecorationStack(createBroadcastIntervalTierItem(unlocked)), 4);

        // Row 1: configure controls (left = faster = fewer seconds, right = slower = more seconds)
        gui.setSlot(makeButton(createFasterButton(selected, unlocked), p -> {
            tower.setBroadcastIntervalSelected(selected - 5);
            plugin.getTowerManager().saveTowers();
            openBroadcastIntervalGUI(p, tower);
        }), 11);
        gui.setSlot(new DecorationStack(createSelectedIntervalDisplay(selected, unlocked)), 13);
        gui.setSlot(makeButton(createSlowerButton(selected), p -> {
            tower.setBroadcastIntervalSelected(selected + 5);
            plugin.getTowerManager().saveTowers();
            openBroadcastIntervalGUI(p, tower);
        }), 15);

        // Row 2: upgrade path
        if (nextUpgrade != null) {
            gui.setSlot(makeButton(createNextBroadcastIntervalItem(nextUpgrade),
                    p -> performBroadcastIntervalUpgrade(p, tower)), 22);
        } else {
            gui.setSlot(new DecorationStack(createMinIntervalReachedItem()), 22);
        }

        gui.setSlot(makeButton(createBackButton(), p -> openTowerGUI(p, tower)), 18);

        DecorationStack border = new DecorationStack(createGlassPane());
        for (int i : new int[]{0,1,2,3,5,6,7,8,9,10,12,14,16,17,19,20,21,23,24,25,26}) {
            gui.setSlot(border, i);
        }

        gui.showInventory(player);
    }

    private void performBroadcastIntervalUpgrade(Player player, RadioTower tower) {
        ConfigManager.BroadcastIntervalUpgrade nextUpgrade =
                plugin.getConfigManager().getNextBroadcastIntervalUpgrade(tower.getBroadcastIntervalUnlocked());

        if (nextUpgrade == null) {
            player.sendMessage(Component.text("Tower already has the fastest interval unlocked!", NamedTextColor.YELLOW));
            return;
        }

        Material upgradeType = nextUpgrade.material;
        int cost = nextUpgrade.cost;

        int amountInInventory = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == upgradeType) amountInInventory += item.getAmount();
        }

        if (amountInInventory < cost) {
            player.sendMessage(Component.text("You need " + cost + "x " +
                    upgradeType.name().replace("_", " ") + " to upgrade! You have " +
                    amountInInventory, NamedTextColor.RED));
            return;
        }

        int remaining = cost;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == upgradeType && remaining > 0) {
                int stackAmount = item.getAmount();
                if (stackAmount <= remaining) {
                    remaining -= stackAmount;
                    item.setAmount(0);
                } else {
                    item.setAmount(stackAmount - remaining);
                    remaining = 0;
                }
            }
        }

        tower.setBroadcastIntervalUnlocked(nextUpgrade.intervalSeconds);
        plugin.getTowerManager().saveTowers();

        tower.getCopperLocation().getWorld().spawnParticle(Particle.END_ROD,
                tower.getCopperLocation().clone().add(0.5, 1, 0.5),
                20, 0.3, 0.5, 0.3, 0.1);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.5f);

        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
        player.sendMessage(Component.text("⏱ INTERVAL UPGRADED!", NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("New Tier: " + nextUpgrade.displayName.replace("&", "§"),
                NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Min Interval Unlocked: " + nextUpgrade.intervalSeconds + "s",
                NamedTextColor.GREEN));
        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));

        openBroadcastIntervalGUI(player, tower);
    }

    private ItemStack createBroadcastIntervalItem(RadioTower tower) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("⏱ Broadcast Interval", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Active: every " + tower.getBroadcastIntervalSelected() + "s", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Fastest Unlocked: " + tower.getBroadcastIntervalUnlocked() + "s", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("Click to configure / upgrade", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBroadcastIntervalTierItem(int unlocked) {
        ConfigManager.BroadcastIntervalUpgrade upgrade =
                plugin.getConfigManager().getBroadcastIntervalUpgradeByInterval(unlocked);
        ItemStack item = new ItemStack(upgrade.material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("✓ Unlocked: " + upgrade.displayName.replace("&", "§"),
                NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Fastest interval: " + unlocked + "s", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Use the controls below to configure", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSelectedIntervalDisplay(int selected, int unlocked) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Every " + selected + "s", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Current broadcast interval", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Range: " + unlocked + "s – 300s", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFasterButton(int selected, int unlocked) {
        boolean canFaster = selected - 5 >= unlocked;
        ItemStack item = new ItemStack(canFaster ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName((canFaster
                ? Component.text("◀ Faster (-5s)", NamedTextColor.GREEN)
                : Component.text("◀ Already at fastest", NamedTextColor.DARK_GRAY))
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSlowerButton(int selected) {
        boolean canSlower = selected + 5 <= 300;
        ItemStack item = new ItemStack(canSlower ? Material.RED_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName((canSlower
                ? Component.text("Slower (+5s) ▶", NamedTextColor.RED)
                : Component.text("At maximum interval ▶", NamedTextColor.DARK_GRAY))
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNextBroadcastIntervalItem(ConfigManager.BroadcastIntervalUpgrade upgrade) {
        ItemStack item = new ItemStack(upgrade.material, Math.max(1, upgrade.cost));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("⚡ Next: " + upgrade.displayName.replace("&", "§"),
                NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Tier " + upgrade.tier, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Fastest Interval: " + upgrade.intervalSeconds + "s", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("Cost: " + upgrade.cost + "x " +
                upgrade.material.name().replace("_", " "), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("Click to upgrade!", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("(Checks your inventory)", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createMinIntervalReachedItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("★ FASTEST INTERVAL UNLOCKED ★", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("No faster tier available!", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createVoiceToggleItem(RadioTower tower) {
        boolean on = tower.isVoiceEnabled();
        ItemStack item = new ItemStack(on ? Material.JUKEBOX : Material.NOTE_BLOCK);
        ItemMeta meta = item.getItemMeta();
        if (on) {
            meta.displayName(Component.text("🎙 Voice Mode: ON", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false)
                    .decoration(TextDecoration.BOLD, true));
            meta.setEnchantmentGlintOverride(true);
        } else {
            meta.displayName(Component.text("🎙 Voice Mode: OFF", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(on
                        ? "Players near this tower can transmit"
                        : "Enable to allow voice transmission",
                on ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("via SimpleVoiceChat.", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Click to toggle " + (on ? "OFF" : "ON"),
                on ? NamedTextColor.RED : NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void openSpeakerActivationGUI(Player player, Location copperLoc) {
        String title = plugin.getConfigManager().getSpeakerStartupCostGuiTitle().replace("&", "§");
        ClickableInventory gui = new ClickableInventory(27, title);

        gui.setSlot(new DecorationStack(createSpeakerActivationInfoItem()), 4);
        gui.setSlot(makeButton(createSpeakerStartupCostItem(), p -> performSpeakerStartupPayment(p, copperLoc)), 13);
        gui.setSlot(makeButton(createCancelActivationButton(), p -> p.closeInventory()), 22);

        DecorationStack border = new DecorationStack(createGlassPane());
        for (int i = 0; i < 27; i++) {
            if (gui.getSlot(i) == null) {
                gui.setSlot(border, i);
            }
        }

        gui.showInventory(player);
    }

    private void performSpeakerStartupPayment(Player player, Location copperLoc) {
        Material costMaterial = plugin.getConfigManager().getSpeakerStartupCostMaterial();
        int costAmount = plugin.getConfigManager().getSpeakerStartupCostAmount();

        int amountInInventory = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == costMaterial) {
                amountInInventory += item.getAmount();
            }
        }

        if (amountInInventory < costAmount) {
            player.sendMessage(Component.text("You need " + costAmount + "x " +
                    costMaterial.name().replace("_", " ") +
                    " to activate this speaker! You have " + amountInInventory + ".", NamedTextColor.RED));
            player.closeInventory();
            return;
        }

        int remaining = costAmount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == costMaterial && remaining > 0) {
                int stackAmount = item.getAmount();
                if (stackAmount <= remaining) {
                    remaining -= stackAmount;
                    item.setAmount(0);
                } else {
                    item.setAmount(stackAmount - remaining);
                    remaining = 0;
                }
            }
        }

        // Register the speaker immediately so re-clicking never charges again.
        // Use the minimum valid frequency as a placeholder; player sets the real one in chat.
        String placeholder = plugin.getConfigManager().normalizeFrequency(
                String.valueOf((int) plugin.getConfigManager().getMinFrequency()));
        plugin.getSpeakerManager().tuneSpeaker(copperLoc, placeholder);

        player.closeInventory();
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
        player.sendMessage(Component.text("Speaker activated! Enter frequency to tune (e.g., " +
                plugin.getConfigManager().getFrequencyFormatExample() + ") in chat:", NamedTextColor.GREEN));
        player.sendMessage(Component.text("Range: " +
                plugin.getConfigManager().getMinFrequency() + " - " +
                plugin.getConfigManager().getMaxFrequency(), NamedTextColor.GRAY));
        player.setMetadata("shortwave_pending_speaker",
                new FixedMetadataValue(plugin, copperLoc));
    }

    private ItemStack createSpeakerActivationInfoItem() {
        ItemStack item = new ItemStack(Material.DECORATED_POT);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("📻 Speaker Activation Required", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("This speaker must be activated before", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("it can receive broadcasts.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("Pay the startup cost below to", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("proceed to frequency selection.", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSpeakerStartupCostItem() {
        Material material = plugin.getConfigManager().getSpeakerStartupCostMaterial();
        int amount = plugin.getConfigManager().getSpeakerStartupCostAmount();
        String displayName = plugin.getConfigManager().getSpeakerStartupCostDisplayName();
        List<String> configLore = plugin.getConfigManager().getSpeakerStartupCostLore();

        ItemStack item = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(displayName.replace("&", "§"))
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Cost: " + amount + "x " +
                material.name().replace("_", " "), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        for (String line : configLore) {
            lore.add(Component.text(line.replace("&", "§"))
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text(""));
        lore.add(Component.text("Click to pay and activate!", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("(Checks your inventory)", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    public void openStartupCostGUI(Player player, Location copperLoc) {
        String title = plugin.getConfigManager().getStartupCostGuiTitle().replace("&", "§");
        ClickableInventory gui = new ClickableInventory(27, title);

        gui.setSlot(new DecorationStack(createStartupInfoItem()), 4);
        gui.setSlot(makeButton(createStartupCostItem(), p -> performStartupPayment(p, copperLoc)), 13);
        gui.setSlot(makeButton(createCancelActivationButton(), p -> p.closeInventory()), 22);

        DecorationStack border = new DecorationStack(createGlassPane());
        for (int i = 0; i < 27; i++) {
            if (gui.getSlot(i) == null) {
                gui.setSlot(border, i);
            }
        }

        gui.showInventory(player);
    }

    private void performStartupPayment(Player player, Location copperLoc) {
        Material costMaterial = plugin.getConfigManager().getStartupCostMaterial();
        int costAmount = plugin.getConfigManager().getStartupCostAmount();

        int amountInInventory = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == costMaterial) {
                amountInInventory += item.getAmount();
            }
        }

        if (amountInInventory < costAmount) {
            player.sendMessage(Component.text("You need " + costAmount + "x " +
                    costMaterial.name().replace("_", " ") +
                    " to activate this tower! You have " + amountInInventory + ".", NamedTextColor.RED));
            player.closeInventory();
            return;
        }

        int remaining = costAmount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == costMaterial && remaining > 0) {
                int stackAmount = item.getAmount();
                if (stackAmount <= remaining) {
                    remaining -= stackAmount;
                    item.setAmount(0);
                } else {
                    item.setAmount(stackAmount - remaining);
                    remaining = 0;
                }
            }
        }

        // Register the tower immediately so re-clicking never charges again.
        // Use the minimum valid frequency as a placeholder; player sets the real one via the GUI.
        String placeholder = plugin.getConfigManager().normalizeFrequency(
                String.valueOf((int) plugin.getConfigManager().getMinFrequency()));
        RadioTower tower = plugin.getTowerManager().createTower(copperLoc, placeholder);
        tower.validateStructureFromBlocks();
        tower.refreshOxidationFromBlock();
        tower.refreshBookPages();

        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
        player.sendMessage(Component.text("Tower activated! Set your frequency via the tower menu.", NamedTextColor.GREEN));

        openTowerGUI(player, tower);
    }

    private ItemStack createStartupInfoItem() {
        ItemStack item = new ItemStack(Material.LIGHTNING_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("📡 Tower Activation Required", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("This tower must be activated before", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("it can begin broadcasting.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("Pay the startup cost below to", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("proceed to frequency selection.", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStartupCostItem() {
        Material material = plugin.getConfigManager().getStartupCostMaterial();
        int amount = plugin.getConfigManager().getStartupCostAmount();
        String displayName = plugin.getConfigManager().getStartupCostDisplayName();
        List<String> configLore = plugin.getConfigManager().getStartupCostLore();

        ItemStack item = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(displayName.replace("&", "§"))
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Cost: " + amount + "x " +
                material.name().replace("_", " "), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        for (String line : configLore) {
            lore.add(Component.text(line.replace("&", "§"))
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text(""));
        lore.add(Component.text("Click to pay and activate!", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("(Checks your inventory)", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCancelActivationButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("✖ Cancel Activation", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("The structure will remain but", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("will not be activated.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("← Back", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("✖ Close", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGlassPane() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        item.setItemMeta(meta);
        return item;
    }

    private String formatTime(int seconds) {
        if (seconds <= 0) {
            return "0 seconds (Empty)";
        }
        int days = seconds / 86400;
        int hours = (seconds % 86400) / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (secs > 0 || sb.length() == 0) sb.append(secs).append("s");
        return sb.toString().trim();
    }
}
