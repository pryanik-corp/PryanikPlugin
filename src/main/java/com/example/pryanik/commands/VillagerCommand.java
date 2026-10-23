package com.example.pryanik.commands;

import com.example.pryanik.PryanikPlugin;
import com.example.pryanik.PortalCustomSpawnPermit;
import com.example.pryanik.Rank;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** CSRV-configured blacksmith villagers that run a fixed safe player action. */
public final class VillagerCommand implements CommandExecutor, Listener {
    private static final String TITLE = "§8Команда жителя";
    private static final List<String> ACTIONS = List.of("mode", "lobby", "kit_start", "home", "chome", "rtp");
    private final PryanikPlugin plugin;
    private final NamespacedKey actionKey;
    private final NamespacedKey villagerKey;

    public VillagerCommand(PryanikPlugin plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "villager_action");
        this.villagerKey = new NamespacedKey(plugin, "command_villager");
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Только игрок."); return true; }
        if (!plugin.authed(player.getUniqueId())) { player.sendMessage("§cСначала войдите."); return true; }
        try {
            if (plugin.db().rank(player.getUniqueId()) != Rank.CSRV && !player.isOp()) {
                player.sendMessage("§cКоманда /villager доступна только CSRV."); return true;
            }
        } catch (Exception ex) { player.sendMessage("§cНе удалось проверить ранг."); return true; }
        Inventory inventory = Bukkit.createInventory(null, 27, TITLE);
        inventory.setItem(10, option(Material.COMPASS, "mode", "§aОткрыть выбор режима"));
        inventory.setItem(11, option(Material.ENDER_PEARL, "lobby", "§bПерейти в лобби"));
        inventory.setItem(12, option(Material.CHEST, "kit_start", "§eСтартовый набор"));
        inventory.setItem(14, option(Material.RED_BED, "home", "§dОткрыть меню домов"));
        inventory.setItem(15, option(Material.BEACON, "chome", "§6Дом клана"));
        inventory.setItem(16, option(Material.ENDER_EYE, "rtp", "§cСлучайная телепортация"));
        player.openInventory(inventory);
        return true;
    }

    private ItemStack option(Material material, String action, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler public void chooseAction(InventoryClickEvent event) {
        if (!TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !plugin.authed(player.getUniqueId())) {
            event.getWhoClicked().closeInventory(); return;
        }
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String action = clicked.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action == null || !ACTIONS.contains(action)) return;
        try {
            if (plugin.db().rank(player.getUniqueId()) != Rank.CSRV && !player.isOp()) {
                player.closeInventory(); player.sendMessage("§cКоманда /villager доступна только CSRV."); return;
            }
        } catch (Exception ex) { player.closeInventory(); return; }
        player.closeInventory();
        final Villager[] spawned = new Villager[1];
        PortalCustomSpawnPermit.run(() -> spawned[0] = (Villager) player.getWorld().spawnEntity(player.getLocation(), EntityType.VILLAGER));
        Villager villager = spawned[0];
        if (villager == null) { player.sendMessage("§cНе удалось создать жителя в этом месте."); return; }
        villager.setProfession(Villager.Profession.TOOLSMITH);
        villager.setVillagerLevel(5);
        villager.setCustomName("§6Кузнец");
        villager.setCustomNameVisible(true);
        villager.setPersistent(true);
        villager.setInvulnerable(true);
        villager.setGravity(false);
        villager.setCollidable(false);
        villager.setAI(false);
        villager.setAware(false);
        villager.getPersistentDataContainer().set(villagerKey, PersistentDataType.BYTE, (byte) 1);
        villager.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        player.sendMessage("§aКузнец создан. При нажатии он выполнит: §e/" + action);
    }

    @EventHandler public void clickVillager(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) return;
        String action = villager.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action == null || villager.getPersistentDataContainer().get(villagerKey, PersistentDataType.BYTE) == null || !ACTIONS.contains(action)) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!plugin.authed(player.getUniqueId())) { player.sendMessage("§cСначала войдите."); return; }
        plugin.runPortalPlayerCommand(player, action);
    }

}
