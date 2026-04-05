package net.civmc.kitpvp.kit;

import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public record CustomItemConfig(String key, Material material, String displayName, List<String> lore, int cost, KitCategory category, String armourSlot, Map<Enchantment, Integer> enchantments, String group) {

    private static NamespacedKey CUSTOM_ITEM_KEY;

    public static void init(NamespacedKey key) {
        CUSTOM_ITEM_KEY = key;
    }

    public ItemStack buildItemStack() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.itemName(Component.text(displayName).decoration(TextDecoration.ITALIC, false));
        if (!lore.isEmpty()) {
            List<Component> loreComponents = lore.stream()
                .<Component>map(line -> Component.text(line).decoration(TextDecoration.ITALIC, false))
                .toList();
            meta.lore(loreComponents);
        }
        meta.getPersistentDataContainer().set(CUSTOM_ITEM_KEY, PersistentDataType.STRING, key);
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            meta.addEnchant(entry.getKey(), entry.getValue(), true);
        }
        item.setItemMeta(meta);
        return item;
    }

    public static String getKey(ItemStack item) {
        if (CUSTOM_ITEM_KEY == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(CUSTOM_ITEM_KEY, PersistentDataType.STRING);
    }
}
