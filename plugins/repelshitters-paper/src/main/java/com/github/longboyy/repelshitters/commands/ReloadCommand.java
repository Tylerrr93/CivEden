package com.github.longboyy.repelshitters.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import com.github.longboyy.repelshitters.RepelShitters;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

public class ReloadCommand extends BaseCommand {

    private final RepelShitters plugin;

    public ReloadCommand(RepelShitters plugin){
        this.plugin = plugin;
    }

    @CommandAlias("rsreload")
    @Description("Reloads the RS plugin")
    @CommandPermission("repelshitters.op")
    public void onReload(Player player) {
        if(!plugin.getConfigManager().parse()){
            player.sendMessage(Component.text("Something went wrong whilst reloading the config.").color(TextColor.color(255,0,0)));
        }
    }


}
