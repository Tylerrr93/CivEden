package com.github.longboyy.repelshitters.listeners;

import com.github.longboyy.repelshitters.RepelShitters;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import vg.civcraft.mc.citadel.events.ReinforcementDamageEvent;

public class CitadelListener implements Listener {

    private final RepelShitters plugin;

    public CitadelListener(RepelShitters plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onReinforcementDamage(ReinforcementDamageEvent event){
        event.setDamageDone(event.getDamageDone() / calculateScalar(event.getPlayer()));
    }

    private float calculateScalar(Player player){
        double initialScalar = plugin.getConfigManager().getCitadelConfig().startingScalar();
        double gameTimeWeighting = plugin.getConfigManager().getCitadelConfig().gameTimeWeight();
        double joinTimeWeighting = plugin.getConfigManager().getCitadelConfig().firstTimeWeight();

        long gameTimeThreshold = plugin.getConfigManager().getCitadelConfig().gameTimeThreshold();
        long joinTimeThreshold = plugin.getConfigManager().getCitadelConfig().firstJoinThreshold();

        long playerGameTime = player.getStatistic(Statistic.PLAY_ONE_MINUTE) * 50L;
        long playerJoinTime = System.currentTimeMillis() - player.getFirstPlayed();

        float gameTimeProgress = Math.min(1.0f, (float) playerGameTime / gameTimeThreshold);
        float joinTimeProgress = Math.min(1.0f, (float) playerJoinTime / joinTimeThreshold);

        double totalWeight = gameTimeWeighting + joinTimeWeighting;
        double weightedProgress = ((gameTimeProgress * gameTimeWeighting) + (joinTimeProgress * joinTimeWeighting))/totalWeight;

        return (float) (initialScalar - (initialScalar - 1.0f) * weightedProgress);
    }

}
