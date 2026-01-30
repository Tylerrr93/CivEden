package com.github.longboyy.repelshitters.listeners;

import com.github.longboyy.repelshitters.RepelShitters;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

public class GhastListener implements Listener {

    private final RepelShitters plugin;

    public GhastListener(RepelShitters plugin){
        this.plugin = plugin;
    }

    // TODO: Fix ghast bastion damage
    /*
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityMove(EntityMoveEvent event){
        if(event.getEntity().getType() != EntityType.HAPPY_GHAST) return;
        this.plugin.getHappyGhastManager().handleGhastMove(event);
    }
     */

    @EventHandler(ignoreCancelled = true)
    public void onEntityMount(EntityMountEvent event){
        if(event.getEntity().getType() != EntityType.PLAYER || !(event.getMount() instanceof LivingEntity livingEntity)) return;
        this.plugin.getHappyGhastManager().modifyGhastStats(livingEntity);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event){
        if(!(event.getEntity() instanceof LivingEntity livingEntity)) return;
        this.plugin.getHappyGhastManager().modifyGhastStats(livingEntity);
    }

    /*
    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event){
        plugin.getLogger().info("Vehicle moving");
        if(event.getVehicle().getType() != EntityType.HAPPY_GHAST) return;
        plugin.getLogger().info("vehicle is happy ghast");

        this.plugin.getHappyGhastManager().handleGhastMove(event);
    }
     */

}
