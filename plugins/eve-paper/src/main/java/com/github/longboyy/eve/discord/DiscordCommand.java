package com.github.longboyy.eve.discord;

import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;

public interface DiscordCommand {
    void handle(SlashCommandEvent event);

    CommandData getCommandData();
}
