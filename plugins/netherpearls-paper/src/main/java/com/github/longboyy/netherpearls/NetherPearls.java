package com.github.longboyy.netherpearls;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import vg.civcraft.mc.civmodcore.ACivMod;
import java.util.Random;

public class NetherPearls extends ACivMod implements Listener {

    private final Random random = new Random();
    private final NetherPearlsConfig config;

    public NetherPearls() {
        this.config = new NetherPearlsConfig(this);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if(!this.config.parse()){
            this.disable();
        }
        this.registerListener(this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        HandlerList.unregisterAll((Listener) this);
    }

    @EventHandler(ignoreCancelled = true)
    public void OnEntityDeath(EntityDeathEvent event) {

        if(event.getDamageSource().getCausingEntity() == null || event.getDamageSource().getCausingEntity().getType() != EntityType.PLAYER) {
            return;
        }

        if(!config.getEntityDropChances().containsKey(event.getEntityType())){
            return;
        }

        if(random.nextFloat() < config.getEntityDropChances().get(event.getEntityType())){
            // figure out how many we should add to drops
            int amount = random.nextInt(Math.min(1, config.getMinDrop()), config.getMaxDrop() + 1);
            event.getDrops().add(new ItemStack(Material.ENDER_PEARL, amount));
        }
    }
}
