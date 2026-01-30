package com.github.longboyy.evenmorerewards.rewards;

import com.github.longboyy.evenmorerewards.EvenMoreRewards;
import com.github.maxopoly.essenceglue.EssenceGluePlugin;
import com.oheers.fish.api.reward.RewardType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class EssenceRewardType extends RewardType {

    private final EvenMoreRewards plugin;

    public EssenceRewardType(EvenMoreRewards plugin) {
        this.plugin = plugin;
    }

    @Override
    public void doReward(@NotNull Player player, @NotNull String key, @NotNull String value, Location hookLocation) {
        EssenceGluePlugin essenceGlue = (EssenceGluePlugin) Bukkit.getPluginManager().getPlugin("EssenceGlue");
        if (essenceGlue == null) {
            plugin.warning("Could not find EssenceGlue plugin");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            plugin.warning("Invalid number specified for RewardType " + getIdentifier() + ": " + value);
            return;
        }

        essenceGlue.getRewardManager().giveLoginReward(player, amount);

    }

    @NotNull
    @Override
    public String getIdentifier() {
        return "ESSENCE";
    }

    @NotNull
    @Override
    public String getAuthor() {
        return "Longboyy";
    }

    @Override
    public @NotNull Plugin getPlugin() {
        return this.plugin;
    }
}
