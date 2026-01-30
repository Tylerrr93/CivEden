package com.github.longboyy.citadelgeyserfix;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PositionTracker {

    private static PositionTracker _instance;

    private final CitadelGeyserFix _plugin;

    private Map<UUID, Location> locationMap = new HashMap<UUID, Location>();
    private Map<UUID, Long> cooldownMap = new HashMap<>();

    public PositionTracker(CitadelGeyserFix plugin) {
        this._plugin = plugin;
        _instance = this;
    }

    public static void updatePosition(Player player) {
        _instance.updatePosition_Internal(player);
    }

    public static void teleportBack(Player player){
        _instance.teleportBack_Internal(player);
    }

    private void updatePosition_Internal(Player player){
        if(!cooldownMap.containsKey(player.getUniqueId())) {
            updateLocationAndCooldown(player);
            return;
        }

        if(System.currentTimeMillis() - cooldownMap.get(player.getUniqueId()) >= this._plugin.getFixConfig().getSampleTimeMillis()){
            updateLocationAndCooldown(player);
        }
    }

    private void updateLocationAndCooldown(Player player){
        locationMap.put(player.getUniqueId(), player.getLocation());
        cooldownMap.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private void teleportBack_Internal(Player player){
        Location location = locationMap.get(player.getUniqueId());
        if(location == null){
            player.kick(Component.text("Attempted to open a locked door too quickly"));
            return;
        }
        player.teleport(location);
    }

}
