package com.programmerdan.minecraft.simpleadminhacks.hacks;

import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.configs.LivestockHackConfig;
import com.programmerdan.minecraft.simpleadminhacks.configs.WikiHackConfig;
import com.programmerdan.minecraft.simpleadminhacks.framework.SimpleHack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockShearEntityEvent;
import java.util.Random;

public class LivestockHack extends SimpleHack<LivestockHackConfig> implements Listener {

    private static final Random RANDOM = new Random();

    public LivestockHack(SimpleAdminHacks plugin, LivestockHackConfig config) {
        super(plugin, config);
    }

    @Override
    public void onEnable() {
        plugin.registerListener(this);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onEntityShear(BlockShearEntityEvent event) {
        if(!(event.getEntity() instanceof Sheep sheep)){
            return;
        }

        double chance = config.getDispenserDamageChancePercent();
        if(chance <= 1.0 && chance < RANDOM.nextDouble()) {
            return;
        }

        sheep.damage(config.getDispenserShearDamage());
    }

    public static LivestockHackConfig generate(SimpleAdminHacks plugin, ConfigurationSection config) {
        return new LivestockHackConfig(plugin, config);
    }
}
