package com.example.pryanik.commands;

import com.example.pryanik.PryanikPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CodeCommand implements CommandExecutor {
    private final PryanikPlugin plugin;
    public CodeCommand(PryanikPlugin plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Эта команда доступна только игроку."); return true; }
        if (!plugin.authed(player.getUniqueId())) { player.sendMessage("§cСначала войдите."); return true; }
        try {
            String code = plugin.db().code(player.getUniqueId());
            if (code == null || code.isBlank()) player.sendMessage("§cКод регистрации не найден.");
            else player.sendMessage("§aВаш код регистрации: §e" + code);
        } catch (Exception ex) {
            player.sendMessage("§cНе удалось получить код регистрации.");
            plugin.getLogger().warning("Could not load registration code: " + ex.getMessage());
        }
        return true;
    }
}
