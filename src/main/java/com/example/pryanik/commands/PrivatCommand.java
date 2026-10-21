package com.example.pryanik.commands;

import com.example.pryanik.PryanikPlugin;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public final class PrivatCommand implements CommandExecutor,TabCompleter {
 private final PryanikPlugin plugin;
 public PrivatCommand(PryanikPlugin plugin){this.plugin=plugin;}
 @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
  if(!(sender instanceof Player p)){sender.sendMessage("Только игрок может использовать эту команду.");return true;}
  if(!plugin.authed(p.getUniqueId())){p.sendMessage("§cСначала войдите.");return true;}
  try {
   if(args.length==2&&args[0].equalsIgnoreCase("delete"))return delete(p,args[1]);
   if(args.length!=1){p.sendMessage("§eВыделите область WorldEdit инструментом (pos1/pos2), затем: /privat <название>. Удаление: /privat delete <название>");return true;}
   String scope=plugin.clanScope(p);if(scope.equals("legacy")){p.sendMessage("§cПриваты доступны только в lobby или игровом грифе.");return true;}if(scope.startsWith("anarchy_")){p.sendMessage("§cВ анархии приваты отключены.");return true;}String id=(scope.equals("legacy")?"":scope+"_")+args[0].toLowerCase(Locale.ROOT);
   if(!id.matches("[a-z0-9_-]{1,32}")){p.sendMessage("§cНазвание: a-z, 0-9, _ или -, до 32 символов.");return true;}
   WorldEditPlugin worldEdit=(WorldEditPlugin)plugin.getServer().getPluginManager().getPlugin("WorldEdit");
   if(worldEdit==null){p.sendMessage("§cWorldEdit не загружен.");return true;}
   Region selection;
   try { selection=worldEdit.getSession(p).getSelection(BukkitAdapter.adapt(p.getWorld())); }
   catch(IncompleteRegionException ex){p.sendMessage("§cСначала выделите две точки через WorldEdit: //pos1 и //pos2.");return true;}
   if(selection==null){p.sendMessage("§cСначала выделите две точки через WorldEdit: //pos1 и //pos2.");return true;}
   var min=selection.getMinimumPoint();var max=selection.getMaximumPoint();
   ProtectedCuboidRegion region=new ProtectedCuboidRegion(id,min,max);
   region.getOwners().addPlayer(p.getUniqueId());
   region.setFlag(Flags.BUILD,StateFlag.State.ALLOW);region.setFlag(Flags.USE,StateFlag.State.ALLOW);region.setFlag(Flags.INTERACT,StateFlag.State.ALLOW);
   RegionManager manager=WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(p.getWorld()));
   if(manager==null){p.sendMessage("§cМенеджер регионов недоступен.");return true;}
   if(manager.hasRegion(id)){p.sendMessage("§cРегион с таким именем уже существует.");return true;}
   manager.addRegion(region);
   p.sendMessage("§aПриват §e"+id+"§a создан по выделению WorldEdit: §7"+min+" §8— §7"+max);
  }catch(Exception e){plugin.getLogger().warning("Ошибка WorldGuard privat: "+e);p.sendMessage("§cНе удалось выполнить операцию с регионом.");}
  return true;
 }
 private boolean delete(Player p,String id){String scope=plugin.clanScope(p);if(scope.equals("legacy")){p.sendMessage("§cПриваты доступны только в lobby или игровом грифе.");return true;}if(scope.startsWith("anarchy_")){p.sendMessage("§cВ анархии приваты отключены.");return true;}if(!scope.equals("legacy"))id=scope+"_"+id.toLowerCase(Locale.ROOT);
  RegionManager manager=WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(p.getWorld()));
  if(manager==null||!manager.hasRegion(id)){p.sendMessage("§cРегион не найден.");return true;}
  var region=manager.getRegion(id);
  if(region==null||!region.getOwners().contains(p.getUniqueId())){p.sendMessage("§cУдалить приват может только его владелец.");return true;}
  manager.removeRegion(id);p.sendMessage("§aПриват удалён.");return true;
 }
 @Override public List<String> onTabComplete(CommandSender s,Command c,String alias,String[] args){return args.length==1&&"delete".startsWith(args[0].toLowerCase(Locale.ROOT))?List.of("delete"):Collections.emptyList();}
}
