package com.github.longboyy.eve.discord.command;

import com.github.longboyy.eve.discord.context.CommandContext;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import java.util.UUID;

public abstract class AuthedDiscordCommand extends BaseDiscordCommand<AuthedDiscordCommand.AuthedCommandContext> {

    public static class AuthedCommandContext extends CommandContext {

        private final UUID playerUUID;

        public AuthedCommandContext(SlashCommandEvent event, UUID playerUUID) {
            super(event);
            this.playerUUID = playerUUID;
        }

        public UUID getPlayerUUID() {
            return this.playerUUID;
        }
    }

    @Override
    protected AuthedCommandContext buildContext(CommandContext base) {
        UUID playerUUID = DiscordSRV.getPlugin().getAccountLinkManager()
            .getUuid(base.getEvent().getUser().getId());
        if (playerUUID == null) {
            base.getEvent().reply("You need to link your accounts.")
                .setEphemeral(true).queue();
            return null;
        }
        return new AuthedCommandContext(base.getEvent(), playerUUID);
    }
}

/*
public abstract class AuthedDiscordCommand extends BaseDiscordCommand {

    public AuthedDiscordCommand(){
        this.addMiddleware((event, next) -> {
            UUID playerUUID = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(event.getUser().getId());
            if(playerUUID == null){
                event.reply("You need to link your Minecraft and Discord accounts.").setEphemeral(true).queue();
                return;
            }
            next.apply();
        });
    }

    public abstract CommandData getCommandData();
}
*/
