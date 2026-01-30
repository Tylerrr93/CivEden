package com.github.longboyy.repelshitters;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActivityManager {

    private final Map<UUID, Long> lastActive = new HashMap<>();
    private final RepelShitters plugin;

    public ActivityManager(RepelShitters plugin) {
        this.plugin = plugin;
    }

    public void trackActivity(Player player){
        this.lastActive.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void stopTracking(Player player){
        this.lastActive.remove(player.getUniqueId());
    }

    public boolean isActive(Player player){
        return this.lastActive.containsKey(player.getUniqueId()) && System.currentTimeMillis() - this.lastActive.get(player.getUniqueId()) < this.plugin.getConfigManager().getInactivityTimeMillis();
    }

}
