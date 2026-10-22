package com.example.pryanik.commands;

import com.example.pryanik.PryanikPlugin;
import com.example.pryanik.Rank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PvpToggleCommand implements CommandExecutor {
    private final PryanikPlugin plugin;
    public PvpToggleCommand(PryanikPlugin plugin) { this.plugin=plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Только игрок."); return true; }
        if (!plugin.authed(player.getUniqueId())) { player.sendMessage("§cСначала войдите."); return true; }
        try {
            if (plugin.db().rank(player.getUniqueId())!=Rank.CSRV && !player.isOp()) { player.sendMessage("§cКоманда доступна только CSRV."); return true; }
            if (args.length!=1 || !(args[0].equalsIgnoreCase("on")||args[0].equalsIgnoreCase("off"))) { player.sendMessage("§eИспользование: /pvp <on|off>"); return true; }
            String scope=plugin.clanScope(player);
            if (!plugin.gameModes().isModeWorld(player.getWorld()) || scope.equals("legacy") || scope.equals("lobby")) { player.sendMessage("§cНастроить PvP можно только в игровом грифе."); return true; }
            boolean enabled=args[0].equalsIgnoreCase("on");
            plugin.db().setPvpEnabled(scope,enabled);
            for(org.bukkit.World world:plugin.getServer().getWorlds())if(scope.equals(plugin.gameModes().instanceOf(world.getName())))plugin.setWorldPvpFlag(world,enabled&&!plugin.gameModes().isSpawnWorld(world));
            player.sendMessage("§aPvP в грифе §e"+scope+" §a"+(enabled?"включено":"выключено")+".");
        } catch (Exception ex) { player.sendMessage("§cНе удалось изменить настройку PvP."); plugin.getLogger().warning("PvP toggle: "+ex); }
        return true;
    }
}
