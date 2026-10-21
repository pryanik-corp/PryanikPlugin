package com.example.pryanik.commands;

import com.example.pryanik.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.Objects;

public final class HomeCommand implements CommandExecutor {
 private final PryanikPlugin plugin;private final String command;
 public HomeCommand(PryanikPlugin plugin,String command){this.plugin=plugin;this.command=command;}
 @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){if(!(s instanceof Player p)){s.sendMessage("Только игрок.");return true;}if(!plugin.authed(p.getUniqueId())){p.sendMessage("§cСначала войдите.");return true;}try{
  if(command.equals("home")){if(plugin.gameModes().isModeWorld(p.getWorld())){p.sendMessage("§cЛичные дома можно использовать только в обычном режиме сервера.");return true;}if(a.length==1&&a[0].equalsIgnoreCase("set")){Location x=p.getLocation();plugin.db().setHome(p.getUniqueId(),x.getWorld().getName(),x.getX(),x.getY(),x.getZ(),x.getYaw(),x.getPitch());p.sendMessage("§aЛичный дом сохранён. Телепорт: /home");return true;}String w=plugin.db().homeWorld(p.getUniqueId());double[] h=plugin.db().home(p.getUniqueId());if(w==null||h==null){p.sendMessage("§cДом не задан. Используйте /home set.");return true;}return teleport(p,w,h,"дом");}
  Clan clan=plugin.db().clan(p.getUniqueId(),plugin.clanScope(p));if(clan==null){p.sendMessage("§cВы не состоите в клане.");return true;}if(a.length==1&&a[0].equalsIgnoreCase("set")){if(!clan.leader().equals(p.getUniqueId())){p.sendMessage("§cУстановить клановый дом может только лидер.");return true;}Location x=p.getLocation();plugin.db().setClanHome(clan.id(),x.getWorld().getName(),x.getX(),x.getY(),x.getZ(),x.getYaw(),x.getPitch());p.sendMessage("§aДом клана сохранён.");return true;}if(!clan.hasHome()){p.sendMessage("§cДом клана ещё не задан.");return true;}return teleport(p,clan.homeWorld(),new double[]{clan.homeX(),clan.homeY(),clan.homeZ(),clan.homeYaw(),clan.homePitch()},"дом клана");
 }catch(Exception e){p.sendMessage("§cОшибка БД/телепортации.");plugin.getLogger().warning("Home command: "+e);}return true;}
 private boolean teleport(Player p,String world,double[] h,String label){World w=Bukkit.getWorld(world);if(plugin.gameModes().isModeWorld(p.getWorld())&&(w==null||!Objects.equals(plugin.instanceOf(p.getWorld().getName()),plugin.instanceOf(w.getName())))){p.sendMessage("§cНельзя телепортироваться за пределы текущего грифа.");return true;}if(w==null){p.sendMessage("§cМир дома не загружен: "+world);return true;}p.teleport(new Location(w,h[0],h[1],h[2],(float)h[3],(float)h[4]));p.sendMessage("§aТелепортация: "+label+".");return true;}
}
