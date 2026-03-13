package com.programmerdan.minecraft.simpleadminhacks.configs;

import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.framework.SimpleHackConfig;
import org.bukkit.configuration.ConfigurationSection;

public class LivestockHackConfig extends SimpleHackConfig {

    private double dispenserShearDamage;
    private double dispenserDamageChancePercent;

    public LivestockHackConfig(SimpleAdminHacks plugin, ConfigurationSection base) {
        super(plugin, base);
    }

    @Override
    protected void wireup(ConfigurationSection config) {
        this.dispenserShearDamage = config.getDouble("dispenserShearDamage", 2.0);
        this.dispenserDamageChancePercent = config.getDouble("dispenserDamageChancePercent", 0.2);
    }

    public double getDispenserShearDamage() {
        return dispenserShearDamage;
    }

    public double getDispenserDamageChancePercent() {
        return dispenserDamageChancePercent;
    }
}
