package com.github.longboyy.netherpearls;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import vg.civcraft.mc.civmodcore.config.ConfigHelper;
import vg.civcraft.mc.civmodcore.config.ConfigParser;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetherPearlsConfig extends ConfigParser {

    private Map<EntityType, Double> entityDropChances;
    private int minDrop;
    private int maxDrop;

    public NetherPearlsConfig(Plugin plugin) {
        super(plugin);
    }

    public Map<EntityType, Double> getEntityDropChances() {
        return this.entityDropChances;
    }

    public int getMinDrop() {
        return this.minDrop;
    }

    public int getMaxDrop() {
        return this.maxDrop;
    }

    @Override
    protected boolean parseInternal(ConfigurationSection config) {
        this.entityDropChances = new HashMap<EntityType, Double>();
        this.minDrop = config.getInt("minDrop", 1);
        this.maxDrop = config.getInt("maxDrop", 2);
        ConfigHelper.parseKeyValueMap(config, "dropChances", plugin.getLogger(), EntityType::valueOf, Double::parseDouble, this.entityDropChances);
        return true;
    }
}
