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

public class GenericArmourSelectionGui extends ItemSelectionGui {

    public GenericArmourSelectionGui(KitPvpDao dao, Player player, int slot, Kit kit, Runnable parent, EditKitGui gui) {
        super(dao, "Armour", player, slot, kit, parent, gui);
    }

    private Clickable toClickable(KitItem kitItem) {
        ItemStack item = new ItemStack(kitItem.getItem());
        return toClickable(KitCost.setPoints(item, kitItem.getCost()), item);
    }

    private List<CustomItemConfig> customArmour() {
        return JavaPlugin.getPlugin(KitPvpPlugin.class).getCustomItems().stream()
            .filter(item -> item.category() == KitCategory.ARMOUR && item.armourSlot() != null)
            .toList();
    }

    @Override
    protected int totalPages() {
        List<CustomItemConfig> armour = customArmour();
        return armour.isEmpty() ? 1 : 1 + groupedPagesNeeded(armour);
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

            add(inventory, KitItem.LEATHER_HELMET,     0,  disabled);
            add(inventory, KitItem.LEATHER_CHESTPLATE, 9,  disabled);
            add(inventory, KitItem.LEATHER_LEGGINGS,   18, disabled);
            add(inventory, KitItem.LEATHER_BOOTS,      27, disabled);

            add(inventory, KitItem.GOLDEN_HELMET,      1,  disabled);
            add(inventory, KitItem.GOLDEN_CHESTPLATE,  10, disabled);
            add(inventory, KitItem.GOLDEN_LEGGINGS,    19, disabled);
            add(inventory, KitItem.GOLDEN_BOOTS,       28, disabled);

            add(inventory, KitItem.CHAINMAIL_HELMET,     2,  disabled);
            add(inventory, KitItem.CHAINMAIL_CHESTPLATE, 11, disabled);
            add(inventory, KitItem.CHAINMAIL_LEGGINGS,   20, disabled);
            add(inventory, KitItem.CHAINMAIL_BOOTS,      29, disabled);

            add(inventory, KitItem.IRON_HELMET,      3,  disabled);
            add(inventory, KitItem.IRON_CHESTPLATE,  12, disabled);
            add(inventory, KitItem.IRON_LEGGINGS,    21, disabled);
            add(inventory, KitItem.IRON_BOOTS,       30, disabled);

            add(inventory, KitItem.DIAMOND_HELMET,     4,  disabled);
            add(inventory, KitItem.DIAMOND_CHESTPLATE, 13, disabled);
            add(inventory, KitItem.DIAMOND_LEGGINGS,   22, disabled);
            add(inventory, KitItem.DIAMOND_BOOTS,      31, disabled);

            add(inventory, KitItem.NETHERITE_HELMET,     5,  disabled);
            add(inventory, KitItem.NETHERITE_CHESTPLATE, 14, disabled);
            add(inventory, KitItem.NETHERITE_LEGGINGS,   23, disabled);
            add(inventory, KitItem.NETHERITE_BOOTS,      32, disabled);

            add(inventory, KitItem.TURTLE_HELMET, 7,  disabled);
            add(inventory, KitItem.ELYTRA,        16, disabled);
            // Slot 53 = Next Page (set by base class if custom armour exists)
        } else {
            placeGroupedItems(inventory, customArmour(), page - 1);
        }
    }
}
