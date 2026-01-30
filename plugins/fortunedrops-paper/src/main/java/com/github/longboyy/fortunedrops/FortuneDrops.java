package com.github.longboyy.fortunedrops;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import vg.civcraft.mc.civmodcore.ACivMod;

public final class FortuneDrops extends ACivMod {

    private FortuneConfigManager configManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        configManager = new FortuneConfigManager(this);
        if(!configManager.parse()){
            this.severe("Failed to read config, disabling");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public FortuneConfigManager getConfigManager() {
        return configManager;
    }
}
