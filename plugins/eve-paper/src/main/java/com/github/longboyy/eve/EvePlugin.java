package com.github.longboyy.eve;

import com.github.longboyy.eve.database.EveDAO;
import com.github.longboyy.eve.discord.DiscordCommandManager;
import com.github.longboyy.eve.discord.listeners.DiscordReadyListener;
import com.github.longboyy.eve.listeners.SnitchListener;
import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import vg.civcraft.mc.civmodcore.ACivMod;
import vg.civcraft.mc.civmodcore.dao.ManagedDatasource;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.NameLayerPlugin;

public class EvePlugin extends ACivMod {
    private final EvePluginConfig config;
    private final DiscordCommandManager discordCommandManager;
    private final RelayManager relayManager;
    private final DiscordReadyListener discordReadyListener;

    private EveDAO dao;
    private SnitchListener snitchListener;

    public EvePlugin() {
        this.config = new EvePluginConfig(this);
        this.discordCommandManager = new DiscordCommandManager(this);
        this.relayManager = new RelayManager(this);
        this.discordReadyListener = new DiscordReadyListener(this);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if(!this.config.parse()){
            this.disable();
        }
        EvePermissionHandler.setup();
        this.dao = new EveDAO(this.config.getDatabase());
        this.dao.updateDatabase();
        this.snitchListener = new SnitchListener(this);
        this.relayManager.reloadRelays();

        DiscordSRV.api.subscribe(this.discordReadyListener);
        this.registerListener(this.snitchListener);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        DiscordSRV.api.unsubscribe(this.discordReadyListener);
        HandlerList.unregisterAll(this);
    }

    public EvePluginConfig getConfigManager(){
        return this.config;
    }

    public EveDAO getDatabase(){
        return this.dao;
    }

    public RelayManager getRelayManager(){
        return this.relayManager;
    }

    public void setupDiscord(){
        this.discordCommandManager.registerCommands();
    }
}
