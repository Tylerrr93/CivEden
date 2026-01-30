package com.github.longboyy.fortunedrops;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import vg.civcraft.mc.civmodcore.config.ConfigParser;

public class FortuneConfigManager extends ConfigParser {

    private boolean debugEnabled;

    public FortuneConfigManager(Plugin plugin) {
        super(plugin);
    }

    @Override
    protected boolean parseInternal(ConfigurationSection config) {
        this.debugEnabled = config.getBoolean("debug");
        return true;
    }
}
