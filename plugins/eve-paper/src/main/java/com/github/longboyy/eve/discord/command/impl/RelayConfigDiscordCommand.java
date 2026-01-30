package com.github.longboyy.eve.discord.command.impl;

import com.github.longboyy.eve.discord.DiscordCommand;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.SubcommandData;

public class RelayConfigDiscordCommand implements DiscordCommand {

    public static final CommandData COMMAND_DATA = new CommandData("relayconfig", "Relay config commands")
        .addSubcommands(
            new SubcommandData("create", "Create a new relay config")
                .addOption(OptionType.STRING, "name", "The identifying name of the config", true),
            new SubcommandData("edit", "Edit an existing relay config")
                .addOption(OptionType.STRING, "name", "The identifying name of the config", true)
                .addOption(OptionType.STRING, "option", "The config option you want to edit", true)
                .addOption(OptionType.STRING, "value", "The new value for the config option", true),
            new SubcommandData("delete", "Delete an existing relay config")
                .addOption(OptionType.STRING, "name", "The identifying name of the config", true),
            new SubcommandData("list", "List the config options for a relay")
                .addOption(OptionType.STRING, "name", "The identifying name of the config", true),
            new SubcommandData("setconfig", "Apply the specified config to a relay in this channel")
                .addOption(OptionType.STRING, "config_name", "The identifying name of the config", true)
                .addOption(OptionType.STRING, "group_name", "The group name to setup a config for")
        );

    @Override
    public void handle(SlashCommandEvent event) {
        switch(event.getCommandString()){
            case "create":
                break;
            case "edit":
                break;
            case "delete":
                break;
            case "list":
                break;
            default:
                break;
        }
    }

    @Override
    public CommandData getCommandData() {
        return COMMAND_DATA;
    }

    private void handleSubcommandCreate(SlashCommandEvent event){

    }

    private void handleSubcommandEdit(SlashCommandEvent event){

    }

    private void handleSubcommandDelete(SlashCommandEvent event){

    }

    private void handleSubcommandList(SlashCommandEvent event){

    }
}
