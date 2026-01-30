package com.programmerdan.minecraft.simpleadminhacks.hacks;

import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.configs.SpawnerNerfConfig;
import com.programmerdan.minecraft.simpleadminhacks.configs.ToggleLampConfig;
import com.programmerdan.minecraft.simpleadminhacks.framework.SimpleHack;
import org.apache.commons.lang3.RandomUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.ReinforcementManager;
import vg.civcraft.mc.citadel.model.Reinforcement;
import vg.civcraft.mc.civmodcore.utilities.BiasedRandomPicker;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SpawnerNerf extends SimpleHack<SpawnerNerfConfig> implements Listener {

    public static final String NAME = "SpawnerNerf";

    private final Random random = new Random();

    public SpawnerNerf(SimpleAdminHacks plugin, SpawnerNerfConfig config) {
        super(plugin, config);
    }

    @Override
    public void onEnable() {
        plugin.registerListener(this);
    }

    @Override
    public void onDisable(){
        HandlerList.unregisterAll(this);
    }

    @Override
    public String status() {
        return this.isEnabled() ? "Spawner Nerf enabled." : "Spawner Nerf disabled.";
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void OnCreatureSpawn(CreatureSpawnEvent event){
        if(!config().isDisableSpawners()){
            return;
        }

        if(event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void OnGenerateLoot(LootGenerateEvent event){
        if(!config().isReplaceSpawnerLoot()){
            return;
        }

        var picker = config().getBiasedRandomPicker();

        List<ItemStack> newLoot = new ArrayList<>();
        for(ItemStack lootItem : event.getLoot()){
            if(lootItem.getType().isAir()){
                newLoot.add(new ItemStack(Material.AIR));
                continue;
            }

            var newItem = picker.getRandom();
            int quantity = random.nextInt(newItem.minAmount(), newItem.maxAmount() + 1);
            newLoot.add(new ItemStack(newItem.material(), quantity));
        }

        event.setLoot(newLoot);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void OnBlockBreak(BlockBreakEvent event){
        if(!config.isSpawnersDropXPBottles()){
            this.logger.info("Spawner broke but drop xp bottles is not enabled");
            return;
        }

        if(event.getBlock().getType() != Material.SPAWNER){
            return;
        }

        int quantity = random.nextInt(config().getXpBottlesMin(), config().getXpBottlesMax() + 1);

        Location loc = event.getBlock().getLocation();
        event.setDropItems(false);
        loc.getWorld().dropItemNaturally(loc, ItemStack.of(Material.EXPERIENCE_BOTTLE, quantity));
    }

    //This really doesn't belong here, but I need to deploy a quick fix
    @EventHandler(priority = EventPriority.HIGHEST)
    public void OnPlayerInteract(PlayerInteractEvent event){
        if(event.getClickedBlock() == null){
            return;
        }

        Block block = event.getClickedBlock();
        Location loc = block.getLocation();
        World world = block.getWorld();
        if(loc.getBlockY() >= 127 && block.getType() != Material.END_PORTAL && world.getName().contains("nether")){
            event.setCancelled(true);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if(loc.getBlock().getType() != Material.BEDROCK){
                    loc.getBlock().setType(Material.BEDROCK);
                }
            }, 1L);
        }
    }

    public static SpawnerNerfConfig generate(SimpleAdminHacks plugin, ConfigurationSection config) {
        return new SpawnerNerfConfig(plugin, config);
    }

}
