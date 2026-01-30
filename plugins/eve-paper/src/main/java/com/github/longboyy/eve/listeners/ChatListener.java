package com.github.longboyy.eve.listeners;

import com.github.longboyy.eve.EvePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import vg.civcraft.mc.civchat2.event.GlobalChatEvent;
import vg.civcraft.mc.civchat2.event.GroupChatEvent;

public class ChatListener implements Listener {
    private final EvePlugin plugin;

    public ChatListener(EvePlugin plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onGlobalChat(GlobalChatEvent event){

    }

    @EventHandler
    public void onGroupChat(GroupChatEvent event){

    }
}
