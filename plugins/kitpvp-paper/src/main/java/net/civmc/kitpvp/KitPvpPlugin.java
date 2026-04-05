package net.civmc.kitpvp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.civmc.kitpvp.anvil.AnvilGui;
import net.civmc.kitpvp.arena.ArenaCleaner;
import net.civmc.kitpvp.arena.ArenaCommand;
import net.civmc.kitpvp.arena.ArenaManager;
import net.civmc.kitpvp.arena.FileSlimeLoader;
import net.civmc.kitpvp.arena.PrivateArenaListener;
import net.civmc.kitpvp.arena.RespawnListener;
import net.civmc.kitpvp.arena.data.Arena;
import net.civmc.kitpvp.arena.data.SqlArenaDao;
import net.civmc.kitpvp.command.ClearCommand;
import net.civmc.kitpvp.command.KitCommand;
import net.civmc.kitpvp.kit.CustomItemConfig;
import net.civmc.kitpvp.kit.KitCategory;
import net.civmc.kitpvp.kit.KitCost;
import net.civmc.kitpvp.kit.KitItem;
import net.civmc.kitpvp.ranked.Elo;
import net.civmc.kitpvp.ranked.EloCommand;
import net.civmc.kitpvp.ranked.RankedCommand;
import net.civmc.kitpvp.ranked.RankedConfig;
import net.civmc.kitpvp.ranked.RankedPlaceholders;
import net.civmc.kitpvp.ranked.RankedPlayers;
import net.civmc.kitpvp.ranked.RankedQueueListener;
import net.civmc.kitpvp.ranked.RankedQueueManager;
import net.civmc.kitpvp.ranked.SqlRankedDao;
import net.civmc.kitpvp.ranked.UnrankedCommand;
import net.civmc.kitpvp.snapshot.DeathListener;
import net.civmc.kitpvp.snapshot.InventorySnapshotManager;
import net.civmc.kitpvp.snapshot.ViewInventorySnapshotCommand;
import net.civmc.kitpvp.spawn.SetSpawnCommand;
import net.civmc.kitpvp.spawn.SpawnCommand;
import net.civmc.kitpvp.spawn.SpawnListener;
import net.civmc.kitpvp.spawn.SpawnProvider;
import net.civmc.kitpvp.spawn.SqlSpawnProvider;
import net.civmc.kitpvp.sql.SqlKitPvpDao;
import net.civmc.kitpvp.warp.WarpMain;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import vg.civcraft.mc.civmodcore.ACivMod;
import vg.civcraft.mc.civmodcore.dao.DatabaseCredentials;
import vg.civcraft.mc.civmodcore.dao.ManagedDatasource;

public class KitPvpPlugin extends ACivMod {

    private ManagedDatasource source;
    private List<CustomItemConfig> customItems = List.of();
    private Set<KitItem> disabledItems = Set.of();

    public List<CustomItemConfig> getCustomItems() {
        return customItems;
    }

    public Set<KitItem> getDisabledItems() {
        return disabledItems;
    }

    @Override
    public void onEnable() {
        AnvilGui anvilGui = new AnvilGui();
        getServer().getPluginManager().registerEvents(anvilGui, this);

        saveDefaultConfig();

        // --- Custom item PDC key ---
        CustomItemConfig.init(new NamespacedKey(this, "custom_item"));

        // --- Kit config ---
        int maxPoints = getConfig().getInt("kit.max_points", 50);
        int unbreakableCost = getConfig().getInt("kit.unbreakable_cost", 100);
        int tippedArrowCost = getConfig().getInt("kit.tipped_arrow_cost", 4);

        Map<Enchantment, Integer> enchantmentCosts = new HashMap<>();
        ConfigurationSection enchSection = getConfig().getConfigurationSection("kit.enchantment_costs");
        if (enchSection != null) {
            for (String name : enchSection.getKeys(false)) {
                Enchantment ench = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(NamespacedKey.minecraft(name.toLowerCase()));
                if (ench != null) {
                    enchantmentCosts.put(ench, enchSection.getInt(name));
                } else {
                    getLogger().warning("Unknown enchantment in config: " + name);
                }
            }
        }

        customItems = loadCustomItems();
        disabledItems = loadDisabledItems();
        KitCost.configure(maxPoints, unbreakableCost, tippedArrowCost, enchantmentCosts, customItems);

        // --- Elo config ---
        Elo.configure(getConfig().getDouble("ranked.elo_k_factor", 32.0));

        // --- Database ---
        DatabaseCredentials credentials = (DatabaseCredentials) getConfig().get("database");
        source = ManagedDatasource.construct(this, credentials);
        SqlKitPvpDao dao = new SqlKitPvpDao(source);
        SqlRankedDao ranked = new SqlRankedDao(source);
        getCommand("kit").setExecutor(new KitCommand(dao, ranked, anvilGui));
        new WarpMain(this, source);
        getCommand("clear").setExecutor(new ClearCommand());

        InventorySnapshotManager inventorySnapshotManager = new InventorySnapshotManager();
        DeathListener deathListener = new DeathListener(inventorySnapshotManager);
        getServer().getPluginManager().registerEvents(deathListener, this);
        getCommand("viewinventorysnapshot").setExecutor(new ViewInventorySnapshotCommand(inventorySnapshotManager));

        if (Bukkit.getPluginManager().isPluginEnabled("BreweryX")) {
            getServer().getPluginManager().registerEvents(new DrunkDeathListener(), this);
        }

        SpawnProvider spawnProvider = new SqlSpawnProvider(source);
        getCommand("spawn").setExecutor(new SpawnCommand(spawnProvider));
        getCommand("setspawn").setExecutor(new SetSpawnCommand(spawnProvider));
        getServer().getPluginManager().registerEvents(new SpawnListener(spawnProvider), this);

        int maxArenas = getConfig().getInt("max_arenas");
        ArenaManager manager = new ArenaManager(maxArenas, this, spawnProvider, new FileSlimeLoader(new java.io.File("slime_worlds")));
        SqlArenaDao arenaDao = new SqlArenaDao(source);
        source.updateDatabase();

        List<Arena> arenas = arenaDao.getArenas();
        Arena rankedArena = null;
        for (Arena arena : arenas) {
            if (arena.name().equals(getConfig().getString("ranked_arena"))) {
                rankedArena = arena;
            }
        }

        // --- Ranked config ---
        RankedConfig rankedConfig = loadRankedConfig();

        RankedPlayers players = new RankedPlayers(ranked);
        RankedQueueManager queueManager = new RankedQueueManager(dao, ranked, manager, spawnProvider, rankedArena, players, rankedConfig);
        getCommand("ranked").setExecutor(new RankedCommand(queueManager));
        getCommand("unranked").setExecutor(new UnrankedCommand(queueManager));
        getCommand("elo").setExecutor(new EloCommand(players));
        getServer().getPluginManager().registerEvents(new RankedQueueListener(queueManager, deathListener), this);
        new RankedPlaceholders(players).register();

        PrivateArenaListener privateArenaListener = new PrivateArenaListener(spawnProvider, manager);
        getServer().getPluginManager().registerEvents(privateArenaListener, this);
        getCommand("arena").setExecutor(new ArenaCommand(this, arenaDao, ranked, queueManager, manager, privateArenaListener));
        getServer().getPluginManager().registerEvents(new RespawnListener(manager), this);
        Bukkit.getScheduler().runTaskTimer(this, new ArenaCleaner(manager), 20 * 60, 20 * 60);
    }

    private Set<KitItem> loadDisabledItems() {
        Set<KitItem> result = new HashSet<>();
        for (String name : getConfig().getStringList("kit.disabled_items")) {
            try {
                result.add(KitItem.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException e) {
                getLogger().warning("Unknown disabled item in config: " + name);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private List<CustomItemConfig> loadCustomItems() {
        List<CustomItemConfig> items = new ArrayList<>();
        for (Map<?, ?> raw : getConfig().getMapList("kit.custom_items")) {
            try {
                String key = (String) raw.get("key");
                Material material = Material.valueOf(((String) raw.get("material")).toUpperCase());
                String displayName = (String) raw.get("display_name");
                @SuppressWarnings("unchecked")
                List<String> lore = raw.containsKey("lore") ? (List<String>) raw.get("lore") : List.of();
                int cost = (int) raw.get("cost");
                KitCategory category = KitCategory.valueOf(((String) raw.get("category")).toUpperCase());
                String armourSlot = category == KitCategory.ARMOUR && raw.containsKey("armour_slot")
                    ? ((String) raw.get("armour_slot")).toUpperCase()
                    : null;
                Map<Enchantment, Integer> enchantments = new java.util.HashMap<>();
                // Accept both "enchantments:" and "enchants:" as the config key
                String enchKey = raw.containsKey("enchantments") ? "enchantments"
                    : raw.containsKey("enchants") ? "enchants" : null;
                if (enchKey != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> rawEnchants = (Map<String, Integer>) raw.get(enchKey);
                    for (Map.Entry<String, Integer> entry : rawEnchants.entrySet()) {
                        Enchantment ench = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(NamespacedKey.minecraft(entry.getKey().toLowerCase()));
                        if (ench != null) {
                            enchantments.put(ench, entry.getValue());
                        } else {
                            getLogger().warning("Unknown enchantment on custom item '" + key + "': " + entry.getKey());
                        }
                    }
                }
                String group = raw.containsKey("group") ? (String) raw.get("group") : null;
                items.add(new CustomItemConfig(key, material, displayName, lore, cost, category, armourSlot, enchantments, group));
            } catch (Exception e) {
                getLogger().warning("Failed to load custom item: " + raw + " — " + e.getMessage());
            }
        }
        return List.copyOf(items);
    }

    private RankedConfig loadRankedConfig() {
        ConfigurationSection r = getConfig().getConfigurationSection("ranked");
        ConfigurationSection wb = r.getConfigurationSection("world_border");
        ConfigurationSection s1 = r.getConfigurationSection("spawn1");
        ConfigurationSection s2 = r.getConfigurationSection("spawn2");
        ConfigurationSection mm = r.getConfigurationSection("matchmaking");
        return new RankedConfig(
            r.getInt("match_timeout_minutes", 10),
            r.getInt("max_height", 90),
            r.getString("default_kit_name", "Ranked"),
            r.getDouble("elo_k_factor", 32.0),
            r.getInt("recent_match_cooldown_seconds", 45),
            wb.getDouble("center_x", 72.5),
            wb.getDouble("center_z", 72.5),
            wb.getDouble("initial_size", 143),
            wb.getDouble("final_size", 5),
            wb.getLong("shrink_seconds", 480),
            wb.getDouble("damage_amount", 3.0),
            s1.getDouble("x", 42.5), s1.getDouble("y", 72.0), s1.getDouble("z", 33.5),
            (float) s1.getDouble("yaw", -45.0), (float) s1.getDouble("pitch", 0.0),
            s2.getDouble("x", 96.5), s2.getDouble("y", 72.0), s2.getDouble("z", 89.5),
            (float) s2.getDouble("yaw", 135.0), (float) s2.getDouble("pitch", 0.0),
            mm.getInt("initial_elo_gap", 200),
            mm.getInt("gap_at_20s", 300),
            mm.getInt("gap_at_40s", 400),
            mm.getInt("gap_at_60s", 10000)
        );
    }

    @Override
    public void onDisable() {
        this.source.close();
    }
}
