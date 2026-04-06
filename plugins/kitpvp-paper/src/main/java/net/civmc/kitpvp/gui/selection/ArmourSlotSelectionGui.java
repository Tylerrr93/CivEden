package net.civmc.kitpvp.gui.selection;

import net.civmc.kitpvp.KitPvpPlugin;
import net.civmc.kitpvp.kit.CustomItemConfig;
import net.civmc.kitpvp.kit.Kit;
import net.civmc.kitpvp.kit.KitCategory;
import net.civmc.kitpvp.kit.KitItem;
import net.civmc.kitpvp.kit.KitPvpDao;
import net.civmc.kitpvp.gui.EditKitGui;
import net.civmc.kitpvp.kit.KitCost;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import vg.civcraft.mc.civmodcore.inventory.gui.ClickableInventory;
import java.util.List;
import java.util.Set;

public class ArmourSlotSelectionGui extends ItemSelectionGui {

    private final List<Material> items;

    public ArmourSlotSelectionGui(KitPvpDao dao, Player player, int slot, Kit kit, EditKitGui gui, List<Material> items) {
        super(dao, "Armour", player, slot, kit, gui::open, gui);
        this.items = items;
    }

    private String armourSlotName() {
        return switch (slot) {
            case 36 -> "BOOTS";
            case 37 -> "LEGGINGS";
            case 38 -> "CHESTPLATE";
            case 39 -> "HELMET";
            default -> null;
        };
    }

    @Override
    public void addItems(ClickableInventory inventory, int page) {
        Set<KitItem> disabled = JavaPlugin.getPlugin(KitPvpPlugin.class).getDisabledItems();

        ItemStack none = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta noneMeta = none.getItemMeta();
        noneMeta.itemName(Component.text("None", NamedTextColor.GRAY));
        none.setItemMeta(noneMeta);
        inventory.setSlot(toClickable(none, null), 18);

        int displayIndex = 0;
        for (Material material : items) {
            for (KitItem kitItem : KitItem.values()) {
                if (kitItem.getItem() == material && !disabled.contains(kitItem)) {
                    ItemStack stack = new ItemStack(material);
                    inventory.setSlot(toClickable(KitCost.setPoints(stack, kitItem.getCost()), stack), 19 + displayIndex);
                    displayIndex++;
                    break;
                }
            }
        }

        String slotName = armourSlotName();
        if (slotName == null) return;

        for (CustomItemConfig item : JavaPlugin.getPlugin(KitPvpPlugin.class).getCustomItems()) {
            if (item.category() == KitCategory.ARMOUR && slotName.equals(item.armourSlot())) {
                inventory.setSlot(toClickable(item), 19 + displayIndex++);
            }
        }
    }
}
