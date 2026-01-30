package com.github.longboyy.eve;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import vg.civcraft.mc.civmodcore.config.ConfigHelper;
import vg.civcraft.mc.civmodcore.config.ConfigParser;
import vg.civcraft.mc.civmodcore.dao.DatabaseCredentials;
import vg.civcraft.mc.civmodcore.dao.ManagedDatasource;

public class EvePluginConfig extends ConfigParser {

    private final EvePlugin plugin;

    private ManagedDatasource database;

    public EvePluginConfig(EvePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    protected boolean parseInternal(ConfigurationSection config) {
        this.database = ManagedDatasource.construct(this.plugin, (DatabaseCredentials) config.get("database"));
        return true;
    }

    public ManagedDatasource getDatabase() {
        return this.database;
    }
}
