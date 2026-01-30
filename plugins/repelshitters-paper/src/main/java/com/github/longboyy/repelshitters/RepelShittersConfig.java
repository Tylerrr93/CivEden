package com.github.longboyy.repelshitters;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import vg.civcraft.mc.civmodcore.config.ConfigHelper;
import vg.civcraft.mc.civmodcore.config.ConfigParser;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class RepelShittersConfig extends ConfigParser {

    private long inactivityTimeMillis;

    private CitadelConfig citadelConfig;

    private double ghastBlocksPerSecond;
    private double ghastMaxHealth;
    private int ghastConfigHash;

    public RepelShittersConfig(Plugin plugin) {
        super(plugin);
    }

    public long getInactivityTimeMillis() {
        return inactivityTimeMillis;
    }

    public double getGhastBlocksPerSecond() {
        return ghastBlocksPerSecond;
    }

    public double getGhastMaxHealth() {
        return ghastMaxHealth;
    }

    public int getGhastConfigHash(){
        return this.ghastConfigHash;
    }

    public CitadelConfig getCitadelConfig() {
        return citadelConfig;
    }

    @Override
    protected boolean parseInternal(ConfigurationSection config) {
        String timeString = config.getString("inactivityTime", "5m");
        this.inactivityTimeMillis = ConfigHelper.parseTime(timeString, TimeUnit.MILLISECONDS);
        // sqrt(BLOCK_PER_SECOND/1440) = real speed
        this.ghastBlocksPerSecond = Math.sqrt(config.getDouble("ghastBlocksPerSecond", 7.0)/1440);
        this.ghastMaxHealth = config.getDouble("ghastMaxHealth", 40.0);
        this.ghastConfigHash = Objects.hash(this.ghastBlocksPerSecond, this.ghastMaxHealth);
        var citadelConfigSection = config.getConfigurationSection("citadel");
        if(citadelConfigSection == null){
            citadelConfigSection = new MemoryConfiguration();
        }
        this.citadelConfig = CitadelConfig.parse(citadelConfigSection);
        return true;
    }
}
