package com.example.pryanik.commands;

import com.example.pryanik.PryanikPlugin;
import com.example.pryanik.Rank;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class GameModeToggleCommand implements CommandExecutor {
    private final PryanikPlugin plugin;
    public GameModeToggleCommand(PryanikPlugin plugin) { this.plugin=plugin; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Только игрок."); return true; }
        if (!plugin.authed(player.getUniqueId())) { player.sendMessage("§cСначала войдите."); return true; }
        if (plugin.inCombat(player.getUniqueId())) { player.sendMessage("§cНельзя менять режим в бою."); return true; }
        if (args.length!=1 || !args[0].matches("[012]")) { player.sendMessage("§eИспользование: /gm <0|1|2>"); return true; }
        try {
            int value=Integer.parseInt(args[0]);
            Rank rank=plugin.db().rank(player.getUniqueId());
            if(!rank.atLeast(Rank.ADMIN)){player.sendMessage("§cКоманда /gm доступна только ADMIN и выше.");return true;}
            if (value==2 && !rank.canUseVanish()) { player.sendMessage("§cНаблюдатель доступен только CSRV."); return true; }
            if (plugin.gameModes().isLobby(player.getWorld()) && !(value==1&&rank==Rank.CSRV)) { player.sendMessage("§cВ лобби только CSRV может использовать /gm 1."); return true; }
            GameMode mode=switch(value){case 0->GameMode.SURVIVAL;case 1->GameMode.CREATIVE;default->GameMode.SPECTATOR;};
            player.setGameMode(mode);
            player.sendMessage("§aРежим игры изменён: §e"+mode.name()+".");
        } catch (Exception ex) { player.sendMessage("§cНе удалось проверить ранг или изменить режим."); }
        return true;
    }
}
