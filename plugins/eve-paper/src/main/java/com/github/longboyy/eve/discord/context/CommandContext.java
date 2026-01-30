package com.github.longboyy.eve.discord.context;

import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;

public class CommandContext {
    private final SlashCommandEvent event;

    public CommandContext(SlashCommandEvent event){
        this.event = event;
    }

    public SlashCommandEvent getEvent(){
        return this.event;
    }
}
