package com.github.longboyy.fortunedrops;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.HashMap;
import java.util.Map;

public class FortuneListener implements Listener {

    private final FortuneDrops plugin;
    private final Map<Material, FortuneSettings> settings;

    public FortuneListener(FortuneDrops plugin) {
        this.plugin = plugin;
        this.settings = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void OnBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if(!tool.hasItemMeta() || !tool.getItemMeta().hasEnchant(Enchantment.FORTUNE)){
            return;
        }
        Block block = event.getBlock();



        ItemMeta meta = tool.getItemMeta();
        int fortuneLevel = meta.getEnchantLevel(Enchantment.FORTUNE);

    }

    public void OnLavaCast(BlockFormEvent event){
        Block block = event.getBlock();
    }

}
