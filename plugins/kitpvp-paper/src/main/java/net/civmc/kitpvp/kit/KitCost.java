package net.civmc.kitpvp.kit;

import com.dre.brewery.Brew;
import com.dre.brewery.recipe.BRecipe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;

public class KitCost {

    public static int MAX_POINTS = 50;
    public static int UNBREAKABLE_COST = 100;
    public static int TIPPED_ARROW_COST = 4;
    public static Map<Enchantment, Integer> ENCHANTMENT_COST_PER_LEVEL = new HashMap<>();
    private static List<CustomItemConfig> customItems = List.of();

    public static void configure(int maxPoints, int unbreakableCost, int tippedArrowCost,
                                  Map<Enchantment, Integer> enchantmentCosts,
                                  List<CustomItemConfig> items) {
        MAX_POINTS = maxPoints;
        UNBREAKABLE_COST = unbreakableCost;
        TIPPED_ARROW_COST = tippedArrowCost;
        ENCHANTMENT_COST_PER_LEVEL = enchantmentCosts;
        customItems = items;
    }

    public static ItemStack setPoints(ItemStack item, int points) {
        ItemStack cloned = item.clone();
        if (points == 0) {
            return cloned;
        }
        ItemMeta clonedMeta = cloned.getItemMeta();
        List<Component> lore = clonedMeta.hasLore() ? clonedMeta.lore() : new ArrayList<>();
        lore.add(0, Component.text(points + " point" + (points == 1 ? "" : "s"), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        clonedMeta.lore(lore);
        cloned.setItemMeta(clonedMeta);
        return cloned;
    }

    public static int getCost(ItemStack[] items) {
        int cost = 0;
        for (ItemStack item : items) {
            cost += getCost(item);
        }
        return cost;
    }

    public static int getCost(ItemStack item) {
        if (item.isEmpty()) {
            return 0;
        }

        String customKey = CustomItemConfig.getKey(item);
        if (customKey != null) {
            for (CustomItemConfig customItem : customItems) {
                if (customItem.key().equals(customKey)) {
                    return customItem.cost();
                }
            }
        }

        int cost = 0;

        for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
            cost += ENCHANTMENT_COST_PER_LEVEL.getOrDefault(entry.getKey(), 0) * entry.getValue();
        }

        for (KitItem kitItem : KitItem.values()) {
            if (item.getType() == kitItem.getItem()) {
                cost += kitItem.getCost();
                break;
            }
        }

        if (item.hasItemMeta() && item.getItemMeta() instanceof PotionMeta meta) {
            if (item.getType() == Material.TIPPED_ARROW) {
                cost += TIPPED_ARROW_COST;
            }
            for (KitPotion potion : KitPotion.values()) {
                if (meta.getBasePotionType() == potion.getType()) {
                    cost += potion.getCost();
                }
            }
        }

        if (Bukkit.getPluginManager().isPluginEnabled("BreweryX")) {
            Brew brew = Brew.get(item);
            if (brew != null) {
                for (KitDrugs drugs : KitDrugs.values()) {
                    BRecipe recipe = BRecipe.getMatching(drugs.getBrew());
                    if (recipe != null && recipe.getRecipeName().equals(brew.getCurrentRecipe().getRecipeName())) {
                        cost += drugs.getCost();
                    }
                }
            }
        }

        if (item.getItemMeta().isUnbreakable()) {
            cost += UNBREAKABLE_COST;
        }

        return cost;
    }
}
