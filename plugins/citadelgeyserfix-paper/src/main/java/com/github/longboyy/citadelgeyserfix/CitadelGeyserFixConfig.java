package com.github.longboyy.citadelgeyserfix;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import vg.civcraft.mc.civmodcore.config.ConfigHelper;
import vg.civcraft.mc.civmodcore.config.ConfigParser;
import java.util.HashMap;
import java.util.Map;

public class CitadelGeyserFixConfig extends ConfigParser {

    private int firstWarningLevel;
    private int kickWarningLevel;
    private int banWarningLevel;
    private int sampleTimeMillis;
    private int reduceWarningTimeSeconds;

    public CitadelGeyserFixConfig(CitadelGeyserFix plugin) {
        super(plugin);
    }

    public int getFirstWarningLevel() {
        return firstWarningLevel;
    }

    public int getKickWarningLevel() {
        return kickWarningLevel;
    }

    public int getBanWarningLevel() {
        return banWarningLevel;
    }

    public int getSampleTimeMillis() {
        return sampleTimeMillis;
    }

    public int getReduceWarningTimeSeconds() {
        return reduceWarningTimeSeconds;
    }

    @Override
    protected boolean parseInternal(ConfigurationSection config) {
        this.firstWarningLevel = config.getInt("firstWarningLevel", 2);
        this.kickWarningLevel = config.getInt("kickWarningLevel", 3);
        this.banWarningLevel = config.getInt("banWarningLevel", 6);
        this.sampleTimeMillis = config.getInt("timeBetweenSamplesMillis", 1000);
        this.reduceWarningTimeSeconds = config.getInt("timeBetweenReducedWarningSeconds", 60);

        return true;
    }
}
