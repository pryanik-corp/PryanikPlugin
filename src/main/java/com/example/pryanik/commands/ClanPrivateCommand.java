package com.example.pryanik.commands;

import com.example.pryanik.*;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import org.bukkit.Bukkit;
import org.bukkit.command.*;import org.bukkit.entity.Player;
import java.util.*;

public final class ClanPrivateCommand implements CommandExecutor {
 private final PryanikPlugin plugin;private final boolean delete;
 public ClanPrivateCommand(PryanikPlugin plugin,boolean delete){this.plugin=plugin;this.delete=delete;}
 @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){if(!(s instanceof Player p)){s.sendMessage("Только игрок.");return true;}if(!plugin.authed(p.getUniqueId())){p.sendMessage("§cСначала войдите.");return true;}try{if(plugin.gameModes().isSpawnWorld(p.getWorld())||plugin.clanScope(p).startsWith("anarchy_")){p.sendMessage("§cКлановые приваты здесь отключены.");return true;}Clan clan=plugin.db().clan(p.getUniqueId(),plugin.clanScope(p));if(clan==null){p.sendMessage("§cСначала вступите в клан.");return true;}if(!clan.leader().equals(p.getUniqueId())){p.sendMessage("§cУправлять приватами клана может лидер.");return true;}
  RegionManager manager=WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(p.getWorld()));if(manager==null){p.sendMessage("§cМенеджер регионов недоступен.");return true;}
  if(delete){if(a.length!=1){p.sendMessage("§e/chprivatdelete <название>");return true;}String id=regionId(clan,a[0],plugin.clanScope(p));var existing=manager.getRegion(id);if(existing==null){p.sendMessage("§cРегион не найден.");return true;}if(!existing.getId().startsWith(("clan_"+plugin.clanScope(p)+"_"+clan.name()+"_").toLowerCase(Locale.ROOT))||!existing.getOwners().contains(clan.leader())){p.sendMessage("§cЭтот регион не принадлежит вашему клану.");return true;}manager.removeRegion(existing.getId());p.sendMessage("§aКлановый приват удалён.");return true;}
  if(a.length!=1||!a[0].matches("[A-Za-z0-9_-]{1,24}")){p.sendMessage("§eВыделите область WorldEdit и выполните /chprivat <название>");return true;}String id=regionId(clan,a[0],plugin.clanScope(p));if(manager.hasRegion(id)){p.sendMessage("§cРегион с таким именем уже существует.");return true;}
  WorldEditPlugin we=(WorldEditPlugin)Bukkit.getPluginManager().getPlugin("WorldEdit");if(we==null){p.sendMessage("§cWorldEdit не загружен.");return true;}Region selection;try{selection=we.getSession(p).getSelection(BukkitAdapter.adapt(p.getWorld()));}catch(IncompleteRegionException ex){p.sendMessage("§cСначала выделите две точки WorldEdit.");return true;}
  if(selection==null){p.sendMessage("§cСначала выделите область WorldEdit.");return true;}ProtectedCuboidRegion region=new ProtectedCuboidRegion(id,selection.getMinimumPoint(),selection.getMaximumPoint());for(UUID member:plugin.db().clanMembers(clan.id(),plugin.clanScope(p)))region.getMembers().addPlayer(member);region.getOwners().addPlayer(clan.leader());manager.addRegion(region);p.sendMessage("§aКлановый приват создан: §e"+id);
 }catch(Exception ex){plugin.getLogger().warning("Clan private: "+ex);p.sendMessage("§cНе удалось создать/удалить приват клана.");}return true;}
 private String regionId(Clan clan,String suffix,String scope){return ("clan_"+scope+"_"+clan.name()+"_"+suffix).toLowerCase(Locale.ROOT);}
}
