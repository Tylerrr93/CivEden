package com.github.longboyy.repelshitters;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import vg.civcraft.mc.civmodcore.config.ConfigHelper;
import vg.civcraft.mc.civmodcore.config.ConfigParser;

public record CitadelConfig(double startingScalar, long gameTimeThreshold, long firstJoinThreshold, double gameTimeWeight, double firstTimeWeight) {

    public static CitadelConfig parse(ConfigurationSection section){
        double startingScalar = section.getDouble("startingScalar", 4.0D);
        long gameTimeThreshold = ConfigHelper.parseTime(section.getString("gameTimeThreshold", "12h"));
        long firstJoinThreshold = ConfigHelper.parseTime(section.getString("firstJoinThreshold", "48h"));
        double gameTimeWeighting = section.getDouble("gameTimeWeighting", 0.5D);
        double firstJoinWeighting = section.getDouble("firstJoinWeighting", 0.5D);
        return new CitadelConfig(startingScalar, gameTimeThreshold, firstJoinThreshold, gameTimeWeighting, firstJoinWeighting);
    }
}
