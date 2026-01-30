package com.github.longboyy.repelshitters;

import io.papermc.paper.event.entity.EntityMoveEvent;
import isaac.bastion.Bastion;
import isaac.bastion.Permissions;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import vg.civcraft.mc.namelayer.NameLayerAPI;
import vg.civcraft.mc.namelayer.permission.PermissionType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HappyGhastManager {

    private static final NamespacedKey GHAST_KEY = new NamespacedKey("repelshitters", "stats_modified");

    private record HappyGhastData(Location location, int overlaps){}

    private final RepelShitters plugin;
    private final Map<UUID, HappyGhastData> ghastDataMap = new ConcurrentHashMap<>();
    private BukkitTask damageTask;

    public HappyGhastManager(RepelShitters plugin){
        this.plugin = plugin;
    }

    public void startDamageTask(){
        this.damageTask = new BukkitRunnable(){
            @Override
            public void run(){
                // create a fresh copy of the data to ensure we're using the most up to date stuff
                Map<UUID, HappyGhastData> ghastDataCopy = new HashMap<>(ghastDataMap);
                for(var entry : ghastDataCopy.entrySet()){
                    var ghast = entry.getValue().location.getWorld().getEntity(entry.getKey());
                    if(ghast == null || ghast.getType() != EntityType.HAPPY_GHAST || entry.getValue().overlaps == 0){
                        ghastDataMap.remove(entry.getKey());
                        return;
                    }

                    ((HappyGhast)ghast).damage(entry.getValue().overlaps);
                }
            }
        }.runTaskTimer(this.plugin, 0L, 2L);
    }

    public void stopDamageTask(){
        this.damageTask.cancel();
    }

    public void handleGhastMove(EntityMoveEvent event){
        if(event.getEntity().getType() != EntityType.HAPPY_GHAST) return;

        int overlaps = this.countOverlaps((HappyGhast) event.getEntity());
        if(overlaps == 0){
            this.ghastDataMap.remove(event.getEntity().getUniqueId());
            return;
        }

        this.ghastDataMap.put(event.getEntity().getUniqueId(), new HappyGhastData(event.getEntity().getLocation(), overlaps));
    }

    /*
    public void handleGhastExit(VehicleExitEvent event){
        if(event.getVehicle().getType() != EntityType.HAPPY_GHAST) return;

        int overlaps = this.countOverlaps((HappyGhast) event.getVehicle());

        this.ghastDataMap.remove(event.getVehicle().getUniqueId());
        if(overlaps > 0) {
            event.getVehicle().remove();
        }
    }
     */

    public void modifyGhastStats(LivingEntity entity){
        if(entity.getType() != EntityType.HAPPY_GHAST) return;

        var pdc = entity.getPersistentDataContainer();
        int ghastHash = pdc.has(GHAST_KEY) ? pdc.get(GHAST_KEY, PersistentDataType.INTEGER) : -1;

        if(ghastHash == this.plugin.getConfigManager().getGhastConfigHash()) {
            //plugin.getLogger().info("Same hash, skipping ghast");
            return;
        }

        var speedAttribute = entity.getAttribute(Attribute.FLYING_SPEED);
        if(speedAttribute != null) {
            speedAttribute.setBaseValue(this.plugin.getConfigManager().getGhastBlocksPerSecond());
        }

        var maxHealthAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
        if(maxHealthAttribute != null) {
            maxHealthAttribute.setBaseValue(this.plugin.getConfigManager().getGhastMaxHealth());
            entity.setHealth(maxHealthAttribute.getBaseValue());
        }



        pdc.set(GHAST_KEY, PersistentDataType.INTEGER, this.plugin.getConfigManager().getGhastConfigHash());
    }

    private int countOverlaps(HappyGhast ghast){
        var bastions = Bastion.getBastionManager().getBlockingBastions(ghast.getLocation());
        if(bastions.isEmpty()) return 0;

        List<Entity> passengers = ghast.getPassengers();
        if(passengers.isEmpty() || passengers.getFirst().getType() != EntityType.PLAYER) return 0;
        Player player = (Player) passengers.getFirst();

        int overlaps = 0;
        for(var bastion : bastions){
            if(!NameLayerAPI.getGroupManager().hasAccess(bastion.getGroup(), player.getUniqueId(), PermissionType.getPermission(Permissions.BASTION_PLACE))){
                overlaps++;
            }
        }

        return overlaps;
    }


}
