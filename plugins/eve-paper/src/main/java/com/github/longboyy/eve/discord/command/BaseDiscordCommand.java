package com.github.longboyy.eve.discord.command;

import com.github.longboyy.eve.discord.DiscordCommand;
import com.github.longboyy.eve.discord.context.CommandContext;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;

public abstract class BaseDiscordCommand<C extends CommandContext> implements DiscordCommand {

    @Override
    public void handle(SlashCommandEvent event) {
        CommandContext base = new CommandContext(event);
        C ctx = buildContext(base);
        if (ctx != null) {
            execute(ctx);
        }
    }

    protected C buildContext(CommandContext base) {
        if(base.getEvent().getGuild() == null){
            base.getEvent().reply("You need to be in a discord channel to run this command.").queue();
            return null;
        }

        @SuppressWarnings("unchecked")
        C ctx = (C) base;
        return ctx;
    }

    public abstract CommandData getCommandData();
    protected abstract void execute(C ctx);
}

/*
public abstract class BaseDiscordCommand implements DiscordCommand {

    @FunctionalInterface
    interface NextFunction {
        void apply();
    }

    @FunctionalInterface
    interface Middleware {
        void handle(SlashCommandEvent event, NextFunction next);
    }

    private final List<Middleware> middlewares = new ArrayList<>();

    @Override
    public void handle(SlashCommandEvent event) {
        if (middlewares.isEmpty()) {
            execute(event);
        } else {
            executeMiddlewareChain(event, 0);
        }
    }

    public BaseDiscordCommand addMiddleware(Middleware middleware) {
        this.middlewares.add(middleware);
        return this;
    }


    // Private methods
    private void executeMiddlewareChain(SlashCommandEvent event, int index) {
        if (index >= middlewares.size()) {
            execute(event);
            return;
        }

        Middleware middleware = middlewares.get(index);
        middleware.handle(event, () -> executeMiddlewareChain(event, index + 1));
    }


    // Abstract methods
    public abstract CommandData getCommandData();
    protected abstract void execute(SlashCommandEvent event);
}
*/
