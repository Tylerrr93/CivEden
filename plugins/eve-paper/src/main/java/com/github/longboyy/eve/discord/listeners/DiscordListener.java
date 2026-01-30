package com.github.longboyy.eve.discord.listeners;

import github.scarsz.discordsrv.DiscordSRV;

public abstract class DiscordListener {
    public void register(){
        DiscordSRV.api.subscribe(this);
    }

    public void unregister(){
        DiscordSRV.api.unsubscribe(this);
    }
}
