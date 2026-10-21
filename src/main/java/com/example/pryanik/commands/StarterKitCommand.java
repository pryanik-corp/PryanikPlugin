package com.example.pryanik.commands;

import com.example.pryanik.PryanikPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Per-player, per-managed-world-set starter kit. */
public final class StarterKitCommand implements CommandExecutor {
    private static final long COOLDOWN_MS = TimeUnit.HOURS.toMillis(24);
    private final PryanikPlugin plugin;

    public StarterKitCommand(PryanikPlugin plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Только игрок."); return true; }
        if (!plugin.authed(player.getUniqueId())) { player.sendMessage("§cСначала войдите."); return true; }
        String instance = plugin.instanceOf(player.getWorld().getName());
        if (instance == null || !(instance.startsWith("survival_") || instance.startsWith("hardcore_") || instance.startsWith("anarchy_"))) {
            player.sendMessage("§cНабор доступен только в выживании, хардкоре и анархии."); return true;
        }
        try {
            long remaining = plugin.db().cooldownRemaining(player.getUniqueId(), "starter_kit", instance);
            if (remaining > 0) { player.sendMessage("§cНабор в этом грифе уже брали. Повторно через " + format(remaining) + "."); return true; }
            if (!plugin.db().consumeDailyKit(player.getUniqueId(), instance, COOLDOWN_MS)) {
                player.sendMessage("§cНабор в этом грифе уже брали. Повторно через " + format(plugin.db().cooldownRemaining(player.getUniqueId(), "starter_kit", instance)) + "."); return true;
            }
            List<ItemStack> kit = new ArrayList<>();
            kit.add(new ItemStack(Material.CHAINMAIL_HELMET));
            kit.add(new ItemStack(Material.IRON_CHESTPLATE));
            kit.add(new ItemStack(Material.CHAINMAIL_LEGGINGS));
            kit.add(new ItemStack(Material.CHAINMAIL_BOOTS));
            kit.add(new ItemStack(Material.STONE_SWORD));
            kit.add(new ItemStack(Material.STONE_PICKAXE));
            kit.add(new ItemStack(Material.STONE_AXE));
            kit.add(new ItemStack(Material.STONE_SHOVEL));
            kit.add(new ItemStack(Material.STONE_HOE));
            kit.add(new ItemStack(Material.OAK_LOG, 16));
            kit.add(new ItemStack(Material.FURNACE));
            kit.add(new ItemStack(Material.STONE, 32));
            kit.add(new ItemStack(Material.TORCH, 16));
            kit.add(new ItemStack(Material.GOLDEN_APPLE, 2));
            kit.add(new ItemStack(Material.COOKED_BEEF, 8));
            for (ItemStack item : kit) player.getInventory().addItem(item).values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
            player.sendMessage("§aСтартовый набор выдан. В этом грифе его можно получить снова через 24 часа.");
        } catch (Exception ex) {
            plugin.getLogger().warning("Starter kit for " + player.getName() + ": " + ex);
            player.sendMessage("§cНе удалось выдать набор — попробуйте позже.");
        }
        return true;
    }

    private static String format(long millis) {
        long total = Math.max(1, (millis + 59_999) / 60_000);
        long hours = total / 60, minutes = total % 60;
        return (hours > 0 ? hours + " ч " : "") + minutes + " мин";
    }
}
