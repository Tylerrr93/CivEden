package com.github.longboyy.citadelgeyserfix;

import com.github.longboyy.citadelgeyserfix.listeners.PlayerListener;
import net.minecraft.server.players.PlayerList;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.ACivMod;
import java.util.Random;

public class CitadelGeyserFix extends ACivMod {

    private final CitadelGeyserFixConfig config;
    private PlayerListener playerListener;
    private final ExploitTracker exploitTracker;
    private final PositionTracker positionTracker;

    public CitadelGeyserFix() {
        this.config = new CitadelGeyserFixConfig(this);
        this.exploitTracker = new ExploitTracker(this);
        this.positionTracker = new PositionTracker(this);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if(!this.config.parse()){
            this.disable();
        }
        this.playerListener = new PlayerListener(this);
        this.registerListener(playerListener);
        ExploitTracker.startTrackerTask();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        HandlerList.unregisterAll(playerListener);
        ExploitTracker.stopTrackerTask();
    }

    public CitadelGeyserFixConfig getFixConfig(){
        return this.config;
    }
}
