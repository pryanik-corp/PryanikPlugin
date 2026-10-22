package com.example.pryanik;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Small confirmation menu used for command-portal actions that navigate elsewhere. */
public final class PortalActionMenu {
    public static final String MODE_TITLE = "§8Выбор режима";
    private final PryanikPlugin plugin;
    public PortalActionMenu(PryanikPlugin plugin) { this.plugin = plugin; }

    public void openModes(Player player) {
        if (!player.isOnline() || !plugin.authed(player.getUniqueId())) return;
        Inventory inventory = Bukkit.createInventory(null, 27, MODE_TITLE);
        inventory.setItem(11, item(Material.GRASS_BLOCK, "§aВыживание", "§720 грифов, до 20 игроков"));
        inventory.setItem(13, item(Material.IRON_SWORD, "§cХардкор", "§75 грифов, до 15 игроков"));
        inventory.setItem(15, item(Material.TNT, "§6Анархия", "§715 грифов, до 15 игроков"));
        player.openInventory(inventory);
    }
    private ItemStack item(Material material, String name, String lore) {
        ItemStack stack = new ItemStack(material); ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name); meta.setLore(List.of(lore)); stack.setItemMeta(meta); return stack;
    }
}
