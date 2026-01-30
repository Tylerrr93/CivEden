package com.programmerdan.minecraft.simpleadminhacks.configs;

import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.framework.SimpleHackConfig;
import com.programmerdan.minecraft.simpleadminhacks.framework.autoload.AutoLoad;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.config.ConfigHelper;
import vg.civcraft.mc.civmodcore.utilities.BiasedRandomPicker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpawnerNerfConfig extends SimpleHackConfig {

    public record ItemChance(Material material, double weight, int minAmount, int maxAmount) { }

    private boolean disableSpawners;
    private boolean replaceSpawnerLoot;
    private boolean spawnersDropXPBottles;
    private int xpBottlesMin;

    private int xpBottlesMax;

    private List<ItemChance> itemChances;
    private BiasedRandomPicker<ItemChance>  biasedRandomPicker;

    public SpawnerNerfConfig(SimpleAdminHacks plugin, ConfigurationSection base) {
        super(plugin, base);
    }

    public boolean isDisableSpawners() {
        return disableSpawners;
    }

    public boolean isReplaceSpawnerLoot() {
        return replaceSpawnerLoot;
    }

    public BiasedRandomPicker<ItemChance> getBiasedRandomPicker() {
        return biasedRandomPicker;
    }

    public boolean isSpawnersDropXPBottles() {
        return spawnersDropXPBottles;
    }

    public int getXpBottlesMin() {
        return xpBottlesMin;
    }

    public int getXpBottlesMax() {
        return xpBottlesMax;
    }

    @Override
    protected void wireup(ConfigurationSection config) {
        this.disableSpawners = config.getBoolean("disableSpawners", false);
        this.replaceSpawnerLoot = config.getBoolean("replaceSpawnerLoot", false);
        this.spawnersDropXPBottles = config.getBoolean("spawnersDropXPBottles", false);
        this.xpBottlesMin = config.getInt("xpBottlesMin", 1);
        this.xpBottlesMax = config.getInt("xpBottlesMax", 1);

        this.itemChances = new ArrayList<>();
        if(this.replaceSpawnerLoot) {
            List<Map<?, ?>> itemList = config.getMapList("newLoot");
            for (Map<?, ?> itemMap : itemList) {
                try {
                    Material material = Material.valueOf((String) itemMap.get("material"));
                    double weight = ((Number) itemMap.get("weight")).doubleValue();
                    int minAmount = ((Number) itemMap.get("minAmount")).intValue();
                    int maxAmount = ((Number) itemMap.get("maxAmount")).intValue();
                    itemChances.add(new ItemChance(material, weight, minAmount, maxAmount));
                } catch (IllegalArgumentException e) {
                    plugin().getLogger().warning("Invalid material in spawnerChestItems: " + itemMap.get("material"));
                }
            }
            this.biasedRandomPicker = generateBiasedPicker();
        }
    }

    private BiasedRandomPicker<ItemChance> generateBiasedPicker() {
        Map<ItemChance, Double> normalizedChances = new HashMap<>();
        double totalWeight = 0;
        for (ItemChance itemChance : itemChances) {
            totalWeight += itemChance.weight;
        }

        // The total of all chances must equal 1, and the chance is based off their weight
        for(ItemChance itemChance : itemChances) {
            double realChance = itemChance.weight / totalWeight;
            normalizedChances.put(itemChance, realChance);
        }
        return new BiasedRandomPicker<>(normalizedChances);
    }

}
