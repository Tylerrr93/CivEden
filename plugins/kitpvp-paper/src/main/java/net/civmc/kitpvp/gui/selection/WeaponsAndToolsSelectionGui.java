package net.civmc.kitpvp.gui.selection;

import net.civmc.kitpvp.KitPvpPlugin;
import net.civmc.kitpvp.kit.CustomItemConfig;
import net.civmc.kitpvp.kit.Kit;
import net.civmc.kitpvp.kit.KitCategory;
import net.civmc.kitpvp.kit.KitItem;
import net.civmc.kitpvp.kit.KitPvpDao;
import net.civmc.kitpvp.gui.EditKitGui;
import net.civmc.kitpvp.kit.KitCost;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import vg.civcraft.mc.civmodcore.inventory.gui.Clickable;
import vg.civcraft.mc.civmodcore.inventory.gui.ClickableInventory;

import java.util.List;
import java.util.Set;

public class WeaponsAndToolsSelectionGui extends ItemSelectionGui {

    public WeaponsAndToolsSelectionGui(KitPvpDao dao, Player player, int slot, Kit kit, Runnable parent, EditKitGui gui) {
        super(dao, "Weapons and Tools", player, slot, kit, parent, gui);
    }

    private Clickable toClickable(KitItem kitItem) {
        ItemStack item = new ItemStack(kitItem.getItem());
        return toClickable(KitCost.setPoints(item, kitItem.getCost()), item);
    }

    private List<CustomItemConfig> customTools() {
        return JavaPlugin.getPlugin(KitPvpPlugin.class).getCustomItems().stream()
            .filter(item -> item.category() == KitCategory.TOOL)
            .toList();
    }

    @Override
    protected int totalPages() {
        List<CustomItemConfig> tools = customTools();
        return tools.isEmpty() ? 1 : 1 + groupedPagesNeeded(tools);
    }

    private void add(ClickableInventory inventory, KitItem item, int slot, Set<KitItem> disabled) {
        if (!disabled.contains(item)) {
            inventory.setSlot(toClickable(item), slot);
        }
    }

    @Override
    public void addItems(ClickableInventory inventory, int page) {
        if (page == 0) {
            Set<KitItem> disabled = JavaPlugin.getPlugin(KitPvpPlugin.class).getDisabledItems();

            add(inventory, KitItem.WOODEN_SWORD,    0,  disabled);
            add(inventory, KitItem.WOODEN_AXE,      9,  disabled);
            add(inventory, KitItem.WOODEN_PICKAXE,  18, disabled);
            add(inventory, KitItem.WOODEN_SHOVEL,   27, disabled);
            add(inventory, KitItem.WOODEN_HOE,      36, disabled);

            add(inventory, KitItem.GOLDEN_SWORD,    1,  disabled);
            add(inventory, KitItem.GOLDEN_AXE,      10, disabled);
            add(inventory, KitItem.GOLDEN_PICKAXE,  19, disabled);
            add(inventory, KitItem.GOLDEN_SHOVEL,   28, disabled);
            add(inventory, KitItem.GOLDEN_HOE,      37, disabled);

            add(inventory, KitItem.STONE_SWORD,     2,  disabled);
            add(inventory, KitItem.STONE_AXE,       11, disabled);
            add(inventory, KitItem.STONE_PICKAXE,   20, disabled);
            add(inventory, KitItem.STONE_SHOVEL,    29, disabled);
            add(inventory, KitItem.STONE_HOE,       38, disabled);

            add(inventory, KitItem.IRON_SWORD,      3,  disabled);
            add(inventory, KitItem.IRON_AXE,        12, disabled);
            add(inventory, KitItem.IRON_PICKAXE,    21, disabled);
            add(inventory, KitItem.IRON_SHOVEL,     30, disabled);
            add(inventory, KitItem.IRON_HOE,        39, disabled);

            add(inventory, KitItem.DIAMOND_SWORD,   4,  disabled);
            add(inventory, KitItem.DIAMOND_AXE,     13, disabled);
            add(inventory, KitItem.DIAMOND_PICKAXE, 22, disabled);
            add(inventory, KitItem.DIAMOND_SHOVEL,  31, disabled);
            add(inventory, KitItem.DIAMOND_HOE,     40, disabled);

            add(inventory, KitItem.NETHERITE_SWORD,   5,  disabled);
            add(inventory, KitItem.NETHERITE_AXE,     14, disabled);
            add(inventory, KitItem.NETHERITE_PICKAXE, 23, disabled);
            add(inventory, KitItem.NETHERITE_SHOVEL,  32, disabled);
            add(inventory, KitItem.NETHERITE_HOE,     41, disabled);

            add(inventory, KitItem.SHIELD,          6,  disabled);
            add(inventory, KitItem.TRIDENT,         7,  disabled);
            add(inventory, KitItem.FLINT_AND_STEEL, 8,  disabled);

            add(inventory, KitItem.SHEARS,          15, disabled);
            add(inventory, KitItem.ENDER_PEARL,     16, disabled);
            add(inventory, KitItem.FIREWORK_ROCKET, 17, disabled);

            add(inventory, KitItem.BUCKET,          24, disabled);
            add(inventory, KitItem.WATER_BUCKET,    25, disabled);
            add(inventory, KitItem.LAVA_BUCKET,     26, disabled);

            add(inventory, KitItem.MILK_BUCKET,         33, disabled);
            add(inventory, KitItem.POWDER_SNOW_BUCKET,  34, disabled);
            add(inventory, KitItem.ARROW,               35, disabled);

            add(inventory, KitItem.BOW,          42, disabled);
            add(inventory, KitItem.CROSSBOW,     43, disabled);
            add(inventory, KitItem.FISHING_ROD,  44, disabled);

            // Slot 45 = Back (set by base class)
            // Slot 46 = Prev Page (set by base class, not used on page 0)
            add(inventory, KitItem.OAK_BOAT,     47, disabled);
            add(inventory, KitItem.TNT_MINECART, 48, disabled);
            // Slot 53 = Next Page (set by base class if custom tools exist)
        } else {
            placeGroupedItems(inventory, customTools(), page - 1);
        }
    }
}
