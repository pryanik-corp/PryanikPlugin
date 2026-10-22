package com.example.pryanik.commands;

import com.example.pryanik.PryanikPlugin;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

/** Teleports players to safe random surface locations at least 1,000 blocks away. */
public final class RtpCommand implements CommandExecutor {
    private static final int MIN_DISTANCE = 1_000;
    private static final int MAX_RADIUS = 10_000;
    private static final int MAX_ATTEMPTS = 64;
    private final PryanikPlugin plugin;

    public RtpCommand(PryanikPlugin plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Только игрок."); return true; }
        if (!plugin.authed(player.getUniqueId())) { player.sendMessage("§cСначала войдите."); return true; }
        if (plugin.inCombat(player.getUniqueId())) { player.sendMessage("§cНельзя использовать /rtp в бою."); return true; }
        World source = player.getWorld();
        boolean fromSpawn = plugin.gameModes().isSpawnWorld(source);
        World target = fromSpawn ? ordinaryWorldForSpawn(source) : ordinaryWorld(source);
        if (target == null) { player.sendMessage("§cОбычный мир сейчас недоступен."); return true; }
        Location origin = target.equals(source) ? player.getLocation() : null;
        if (origin != null && (Math.abs(origin.getX()) > MAX_RADIUS * 30 || Math.abs(origin.getZ()) > MAX_RADIUS * 30)) { player.sendMessage("§cСлишком далеко от центра мира для /rtp."); return true; }
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
            double radius = origin == null ? ThreadLocalRandom.current().nextDouble(MIN_DISTANCE, MAX_RADIUS) : ThreadLocalRandom.current().nextDouble(MIN_DISTANCE, MAX_RADIUS);
            int x = (int) Math.round((origin == null ? 0 : origin.getX()) + Math.cos(angle) * radius);
            int z = (int) Math.round((origin == null ? 0 : origin.getZ()) + Math.sin(angle) * radius);
            Location safe = safeSurface(target, x, z);
            if (safe == null) continue;
            if (origin != null && safe.distanceSquared(origin) < (long) MIN_DISTANCE * MIN_DISTANCE) continue;
            if (player.teleport(safe, PlayerTeleportEvent.TeleportCause.COMMAND)) {
                player.sendMessage("§aВы телепортированы на случайные координаты: §e" + x + ", " + z + "§a.");
                return true;
            }
            player.sendMessage("§cТелепортация отменена другим плагином.");
            return true;
        }
        player.sendMessage("§cНе удалось найти безопасное место. Попробуйте ещё раз.");
        return true;
    }

    private World ordinaryWorldForSpawn(World source) {
        String instance = plugin.gameModes().instanceOf(source.getName());
        World base = instance == null ? null : org.bukkit.Bukkit.getWorld(instance);
        return base != null && isOrdinary(base) ? base : ordinaryWorld(source);
    }

    private World ordinaryWorld(World source) {
        if (isOrdinary(source)) return source;
        World named = org.bukkit.Bukkit.getWorld("world");
        if (isOrdinary(named)) return named;
        return org.bukkit.Bukkit.getWorlds().stream().filter(this::isOrdinary).findFirst().orElse(null);
    }

    private boolean isOrdinary(World world) {
        return world != null && world.getEnvironment() == World.Environment.NORMAL && !plugin.gameModes().isSpawnWorld(world);
    }

    private Location safeSurface(World world, int x, int z) {
        if (Math.abs(x) > MAX_RADIUS * 30 || Math.abs(z) > MAX_RADIUS * 30) return null;
        if (!world.getWorldBorder().isInside(new Location(world, x, world.getSeaLevel(), z))) return null;
        int y = world.getHighestBlockYAt(x, z);
        if (y <= world.getMinHeight() || y + 2 >= world.getMaxHeight()) return null;
        Material floor = world.getBlockAt(x, y, z).getType();
        Material feet = world.getBlockAt(x, y + 1, z).getType();
        Material head = world.getBlockAt(x, y + 2, z).getType();
        if (!floor.isSolid() || floor == Material.MAGMA_BLOCK || floor == Material.CACTUS || floor == Material.CAMPFIRE || floor == Material.SOUL_CAMPFIRE) return null;
        if (!feet.isAir() || !head.isAir()) return null;
        return new Location(world, x + 0.5, y + 1, z + 0.5);
    }
}
