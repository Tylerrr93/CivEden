package com.github.longboyy.fortunedrops;

import org.bukkit.configuration.ConfigurationSection;
import java.util.List;

/**
 *
 * @param enabled Whether this fortune setting is enabled or not
 * @param fortuneChances The chances for each level (1,2,3,...)
 * @param maxRolls How many times should we roll the fortune chance every time we break a block
 */
public record FortuneSettings(boolean enabled, List<Double> fortuneChances, int maxRolls) {

    public static FortuneSettings parseConfig(ConfigurationSection section){
        boolean enabled = section.getBoolean("enabled");
        List<Double> fortuneChances = section.getDoubleList("fortuneChances");
        int maxRolls = section.getInt("maxRolls");
        return new FortuneSettings(enabled, fortuneChances, maxRolls);
    }

}
