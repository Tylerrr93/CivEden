package net.civmc.kitpvp.gui.selection;

import com.github.maxopoly.finale.Finale;
import net.civmc.kitpvp.KitPvpPlugin;
import net.civmc.kitpvp.kit.CustomItemConfig;
import net.civmc.kitpvp.kit.Kit;
import net.civmc.kitpvp.kit.KitCost;
import net.civmc.kitpvp.kit.KitPvpDao;
import net.civmc.kitpvp.gui.EditKitGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import vg.civcraft.mc.civmodcore.inventory.gui.Clickable;
import vg.civcraft.mc.civmodcore.inventory.gui.ClickableInventory;
import vg.civcraft.mc.civmodcore.inventory.gui.LClickable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public abstract class ItemSelectionGui {
    protected final KitPvpDao dao;
    private final String title;
    private final Player player;
    private final Runnable parent;
    protected final EditKitGui gui;
    protected final int slot;
    protected final Kit kit;
    private ClickableInventory inventory;


    public ItemSelectionGui(KitPvpDao dao, String title, Player player, int slot, Kit kit, Runnable parent, EditKitGui gui) {
        this.dao = dao;
        this.title = title;
        this.player = player;
        this.parent = parent;
        this.gui = gui;
        this.slot = slot;
        this.kit = kit;
    }

    public void open() {
        open(0);
    }

    protected void open(int page) {
        inventory = new ClickableInventory(54, title);
        addItems(inventory, page);

        inventory.setOnClose(parent);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.itemName(Component.text("Back", NamedTextColor.GRAY));
        back.setItemMeta(backMeta);
        inventory.setSlot(new LClickable(back, p -> openParent()), 45);

        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.itemName(Component.text("← Previous Page", NamedTextColor.YELLOW));
            prev.setItemMeta(prevMeta);
            final ClickableInventory inv = inventory;
            inventory.setSlot(new LClickable(prev, p -> {
                inv.setOnClose(null);
                Bukkit.getScheduler().runTask(JavaPlugin.getProvidingPlugin(KitPvpPlugin.class), () -> open(page - 1));
            }), 46);
        }

        if (page < totalPages() - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.itemName(Component.text("Next Page →", NamedTextColor.YELLOW));
            next.setItemMeta(nextMeta);
            final ClickableInventory inv = inventory;
            inventory.setSlot(new LClickable(next, p -> {
                inv.setOnClose(null);
                Bukkit.getScheduler().runTask(JavaPlugin.getProvidingPlugin(KitPvpPlugin.class), () -> open(page + 1));
            }), 53);
        }

        inventory.showInventory(player);
    }

    protected int totalPages() {
        return 1;
    }

    private void openParent() {
        Bukkit.getScheduler().runTask(JavaPlugin.getProvidingPlugin(KitPvpPlugin.class), () -> {
            inventory.setOnClose(null);
            this.parent.run();
        });
    }

    public abstract void addItems(ClickableInventory inventory, int page);

    /**
     * Computes the column/row layout for a list of custom items, grouping items
     * with the same group name into the same column (top-to-bottom). A new group
     * always starts a new column. Items with no group each get their own column.
     *
     * Returns a list of int[3]: { itemIndex, column, row }.
     */
    protected List<int[]> computeGroupedLayout(List<CustomItemConfig> items) {
        LinkedHashMap<String, List<Integer>> groups = new LinkedHashMap<>();
        for (int i = 0; i < items.size(); i++) {
            String g = items.get(i).group();
            // Ungrouped items each get a unique key so they don't share a column
            String key = (g != null && !g.isBlank()) ? g : "\0" + i;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
        }

        List<int[]> layout = new ArrayList<>();
        int col = 0;
        for (List<Integer> group : groups.values()) {
            int row = 0;
            for (int idx : group) {
                if (row >= 5) { col++; row = 0; } // overflow within a group → next column
                layout.add(new int[]{idx, col, row});
                row++;
            }
            col++; // next group always starts a fresh column
        }
        return layout;
    }

    /**
     * How many 9-column pages are needed to display these items with group-based layout.
     */
    protected int groupedPagesNeeded(List<CustomItemConfig> items) {
        if (items.isEmpty()) return 0;
        List<int[]> layout = computeGroupedLayout(items);
        int maxCol = layout.stream().mapToInt(a -> a[1]).max().orElse(0);
        return maxCol / 9 + 1;
    }

    /**
     * Places custom items into the inventory for the given custom-item page (0-indexed),
     * using group-based column layout.
     */
    protected void placeGroupedItems(ClickableInventory inventory, List<CustomItemConfig> items, int customPage) {
        List<int[]> layout = computeGroupedLayout(items);
        int pageStart = customPage * 9;
        int pageEnd = pageStart + 9;
        for (int[] entry : layout) {
            int itemIdx = entry[0], col = entry[1], row = entry[2];
            if (col >= pageStart && col < pageEnd) {
                inventory.setSlot(toClickable(items.get(itemIdx)), row * 9 + (col - pageStart));
            }
        }
    }

    /**
     * Custom items bypass Finale entirely so their config-defined enchantments are
     * preserved. The player can re-enchant the item normally after selecting it.
     */
    protected Clickable toClickable(CustomItemConfig customItem) {
        ItemStack actual = customItem.buildItemStack();
        ItemStack display = KitCost.setPoints(actual.clone(), customItem.cost());
        return new Clickable(display) {
            @Override
            protected void clicked(@NotNull Player clicker) {
                ItemStack[] items = kit.items().clone();
                items[slot] = actual.clone();
                gui.setLastItem(actual);

                JavaPlugin plugin = JavaPlugin.getProvidingPlugin(KitPvpPlugin.class);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    Kit updatedKit = dao.updateKit(kit.id(), kit.icon(), items);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        gui.updateKit(updatedKit);
                        inventory.setOnClose(null);
                        gui.open();
                    });
                });
            }
        };
    }

    protected Clickable toClickable(ItemStack item) {
        return toClickable(item, item);
    }

    protected Clickable toClickable(ItemStack displayItem, ItemStack actualItem) {
        ItemStack cloned = displayItem == null ? null : displayItem.clone();
        if (cloned != null) {
            Finale.getPlugin().update(cloned);
        }
        ItemStack actualCloned = actualItem == null ? null : actualItem.clone();
        if (actualCloned != null) {
            Finale.getPlugin().update(actualCloned);
        }
        return new Clickable(cloned) {
            @Override
            protected void clicked(@NotNull Player clicker) {
                ItemStack[] items = kit.items().clone();
                items[slot] = actualCloned;
                if (actualCloned != null) {
                    gui.setLastItem(actualCloned);
                }

                JavaPlugin plugin = JavaPlugin.getProvidingPlugin(KitPvpPlugin.class);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    Kit updatedKit = dao.updateKit(kit.id(), kit.icon(), items);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        gui.updateKit(updatedKit);
                        inventory.setOnClose(null);
                        gui.open();
                    });
                });
            }
        };
    }
}
