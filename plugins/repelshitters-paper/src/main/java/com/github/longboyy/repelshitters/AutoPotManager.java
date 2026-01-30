package com.github.longboyy.repelshitters;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import vg.civcraft.mc.civmodcore.inventory.items.ItemMap;
import vg.civcraft.mc.civmodcore.players.settings.PlayerSettingAPI;
import vg.civcraft.mc.civmodcore.players.settings.gui.MenuSection;
import vg.civcraft.mc.civmodcore.players.settings.impl.BooleanSetting;
import vg.civcraft.mc.civmodcore.players.settings.impl.FloatSetting;

public class AutoPotManager {

    private static final ItemStack SPLASH_POTION_HEALTH_2 = new ItemStack(Material.SPLASH_POTION, 1);
    private static MenuSection MENU_SECTION = new MenuSection("Autopot Settings", "Manage settings relating to autopot", PlayerSettingAPI.getMainMenu());

    private final RepelShitters plugin;
    private BooleanSetting autopotEnabled;
    private FloatSetting autopotThreshold;

    static {
        PotionMeta meta = (PotionMeta) SPLASH_POTION_HEALTH_2.getItemMeta();
        meta.setBasePotionType(PotionType.STRONG_HEALING);
        SPLASH_POTION_HEALTH_2.setItemMeta(meta);
    }

    public AutoPotManager(RepelShitters plugin){
        this.plugin = plugin;
    }

    public void registerSettings(){
        this.autopotEnabled = new BooleanSetting(this.plugin, true, "Toggle Autopot", "autopotEnabled", "Toggles automatic use of Health Potion 2s");
        this.autopotThreshold = new FloatSetting(this.plugin, 4.0f, "Autopot Threshold", "autopotThreshold", SPLASH_POTION_HEALTH_2, "Threshold in hearts when autopot should be used");
        PlayerSettingAPI.registerSetting(autopotEnabled, MENU_SECTION);
        PlayerSettingAPI.registerSetting(autopotThreshold, MENU_SECTION);
        MENU_SECTION.registerToParentMenu();
        plugin.getLogger().info("Registered autopot settings with PlayerSettingAPI");
    }

    public boolean isAutopotEnabled(Player player){
        return this.autopotEnabled.getValue(player);
    }

    public float getAutopotThreshold(Player player) {
        // health = hearts * 2
        return this.autopotThreshold.getValue(player) * 2;
    }

    public void handleAutoPot(EntityDamageByEntityEvent event){
        if(!(event.getEntity() instanceof  Player player)) return;
        //Player player = (Player) event.getEntity();
        double newHealth = player.getHealth() - event.getDamage();
        if(!isAutopotEnabled(player) || newHealth > getAutopotThreshold(player)) return;

        ItemMap playerInvMap = new ItemMap(player.getInventory());
        int potionCount = playerInvMap.getAmount(SPLASH_POTION_HEALTH_2);
        if(potionCount == 0) return;

        //remove the potion
        ItemMap removalMap = new ItemMap(SPLASH_POTION_HEALTH_2);
        if(!removalMap.removeSafelyFrom(player.getInventory())) return;

        // apply the splash effect
        player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 1));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPLASH_POTION_BREAK, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.INSTANT_EFFECT, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0);
    }

}
