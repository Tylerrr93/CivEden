package net.civmc.heliodor.vein;

import net.civmc.heliodor.HeliodorPlugin;
import net.civmc.heliodor.vein.data.VerticalBlockPos;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EnderSignal;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.List;

public class EnderEyeListener implements Listener {
    private final HeliodorPlugin plugin;
    private final String worldName;
    private final List<VerticalBlockPos> positions;

    public EnderEyeListener(HeliodorPlugin plugin, String worldName, List<VerticalBlockPos> positions) {
        this.plugin = plugin;
        this.worldName = worldName;
        this.positions = positions;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEyeInteract(PlayerInteractEvent event){

        Player player = event.getPlayer();
        if(player.getInventory().getItemInMainHand().getType() != Material.ENDER_EYE){
            return;
        }

        if(event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getAction() != Action.RIGHT_CLICK_AIR){
            return;
        }

        World world = event.getPlayer().getWorld();
        if(!world.getName().equals(this.worldName)){
            return;
        }

        plugin.info("Player used an ender eye, cancelling event and spawning new eye");
        event.setCancelled(true);
        EnderSignal eye = world.spawn(player.getEyeLocation(), EnderSignal.class);

        Location closestLocation = null;
        double distanceSquared = Double.MAX_VALUE;

        if (positions.isEmpty()) {
            return;
        }

        for (VerticalBlockPos position : positions) {
            Location portal = new Location(world, position.x(), world.getMinHeight(), position.z());
            double portalDistanceSquared = portal.distanceSquared(eye.getLocation());
            if (portalDistanceSquared < distanceSquared) {
                closestLocation = portal;
                distanceSquared = portalDistanceSquared;
            }
        }

        eye.setTargetLocation(closestLocation);
        plugin.info("A new ender eye has been spawned!");
    }

    /*
    @EventHandler
    public void onEyeSpawn(EntitySpawnEvent event) {
        if (!(event.getEntity() instanceof EnderSignal eye)) {
            return;
        }

        World world = event.getEntity().getWorld();
        if (!world.getName().equals(worldName)) {
            return;
        }

        Location closestLocation = null;
        double distanceSquared = Double.MAX_VALUE;

        if (positions.isEmpty()) {
            return;
        }

        for (VerticalBlockPos position : positions) {
            Location portal = new Location(world, position.x(), world.getMinHeight(), position.z());
            double portalDistanceSquared = portal.distanceSquared(eye.getLocation());
            if (portalDistanceSquared < distanceSquared) {
                closestLocation = portal;
                distanceSquared = portalDistanceSquared;
            }
        }

        eye.setTargetLocation(closestLocation);
    }

     */

}
