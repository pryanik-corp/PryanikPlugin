package com.example.pryanik;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Lets an authenticated player choose how long their password login is remembered. */
public final class AuthSessionCommand implements CommandExecutor, Listener {
    private static final String TITLE = "§8Срок авторизации";
    private static final long[] DURATIONS = {0L, 900_000L, 1_800_000L, 3_600_000L, 18_000_000L, 36_000_000L, 86_400_000L};
    private static final String[] LABELS = {"Всегда требовать пароль", "15 минут", "30 минут", "1 час", "5 часов", "10 часов", "1 день"};
    private final PryanikPlugin plugin;
    private final NamespacedKey durationKey;

    public AuthSessionCommand(PryanikPlugin plugin) {
        this.plugin = plugin;
        this.durationKey = new NamespacedKey(plugin, "auth_duration_ms");
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Эта команда доступна только игроку."); return true; }
        if (!plugin.authed(player.getUniqueId())) { player.sendMessage("§cСначала войдите через /l."); return true; }
        Inventory menu = Bukkit.createInventory(null, 27, TITLE);
        int[] slots = {9, 10, 11, 12, 14, 15, 16};
        long selected = plugin.authRememberDuration(player.getUniqueId());
        for (int i = 0; i < DURATIONS.length; i++) {
            ItemStack item = new ItemStack(i == 0 ? Material.BARRIER : i < 3 ? Material.CLOCK : Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName((selected == DURATIONS[i] ? "§a✓ " : "§e") + LABELS[i]);
            meta.setLore(i==0?List.of("§7На каждом подключении вводить пароль", "§8Выключает запоминание сессии"):List.of("§7Запомнить вход на этот срок", "§7Только с текущего IP-адреса", "§8Пароль не сохраняется"));
            meta.getPersistentDataContainer().set(durationKey, org.bukkit.persistence.PersistentDataType.LONG, DURATIONS[i]);
            item.setItemMeta(meta);
            menu.setItem(slots[i], item);
        }
        player.openInventory(menu);
        return true;
    }

    @EventHandler public void click(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!plugin.authed(player.getUniqueId())) { player.closeInventory(); return; }
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        Long duration = item.getItemMeta().getPersistentDataContainer().get(durationKey, org.bukkit.persistence.PersistentDataType.LONG);
        if (duration == null || !java.util.Arrays.stream(DURATIONS).anyMatch(value -> value == duration)) return;
        plugin.setAuthRememberDuration(player, duration);
        player.closeInventory();
        player.sendMessage(duration==0L?"§aГотово: пароль потребуется при каждом подключении.":"§aСессия запомнится на §e"+LABELS[java.util.Arrays.binarySearch(DURATIONS,duration>0?duration:0L)]+"§a только с текущего IP. Пароль не сохраняется.");
    }
}
