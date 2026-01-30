package com.github.longboyy.repelshitters.listeners;

import com.devotedmc.ExilePearl.ExilePearlApi;
import com.devotedmc.ExilePearl.ExilePearlPlugin;
import com.devotedmc.ExilePearl.event.PearlDecayEvent;
import com.github.longboyy.repelshitters.RepelShitters;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ExilePearlListener implements Listener {

    private final RepelShitters plugin;

    public ExilePearlListener(RepelShitters plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPearlDecay(PearlDecayEvent event) {
        plugin.getLogger().info(String.format("Pearl for player %s is decaying with long time multiplier of %s, making the final damage %s",
            event.getPearl().getPlayerName(),
            event.getPearl().getLongTimeMultiplier(),
            event.getDamageAmount()));

        event.setDamageAmount(ExilePearlPlugin.getApi().getPearlConfig().getPearlHealthDecayAmount());
    }

}
