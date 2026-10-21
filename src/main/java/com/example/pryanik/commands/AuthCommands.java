package com.example.pryanik.commands;

import com.example.pryanik.*;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.security.SecureRandom;
import java.util.*;

public final class AuthCommands implements CommandExecutor,TabCompleter {
 private static final SecureRandom RANDOM=new SecureRandom();
 private final PryanikPlugin p;private final String name;
 public AuthCommands(PryanikPlugin p,String name){this.p=p;this.name=name;}
 @Override public boolean onCommand(CommandSender s,Command c,String label,String[] a){
  if(!(s instanceof Player x)){s.sendMessage("Эта команда доступна только игроку.");return true;}UUID id=x.getUniqueId();
  try{
   switch(name){
    case "reg":case "register":
     if(p.authed(id)){x.sendMessage("§aВы уже вошли.");return true;}if(p.db().exists(id)){x.sendMessage("§cАккаунт уже зарегистрирован. Используйте /login <пароль>.");return true;}
     if(a.length<1||a.length>2){x.sendMessage("§e/reg <пароль> [повтор]");return true;}if(a[0].length()<6){x.sendMessage("§cПароль должен содержать минимум 6 символов.");return true;}if(a.length==2&&!a[0].equals(a[1])){x.sendMessage("§cПароли не совпадают.");return true;}
     String code=String.format(Locale.ROOT,"%06d",RANDOM.nextInt(1_000_000));p.db().register(id,x.getName(),a[0],code);p.authenticate(x);return true;
    case "l":case "login":
     if(p.authed(id)){x.sendMessage("§aВы уже вошли.");return true;}if(a.length!=1){x.sendMessage("§e/login <пароль>");return true;}String hash=p.db().hash(id);if(hash!=null&&org.mindrot.jbcrypt.BCrypt.checkpw(a[0],hash))p.authenticate(x);else x.sendMessage("§cНеверный пароль или аккаунт не зарегистрирован.");return true;
    case "setrank":
     if(!x.isOp()){x.sendMessage("§cТолько OP сервера может назначать ранги.");return true;}if(a.length!=2){x.sendMessage("§e/setrank <игрок> <CSRV|ADMIN|HELPER|VIP|DEFAULT>");return true;}Player t=Bukkit.getPlayerExact(a[0]);if(t==null){x.sendMessage("§cИгрок не в сети.");return true;}Rank rank=Rank.parse(a[1]);p.db().setRank(t.getUniqueId(),t.getName(),rank);p.updateRank(t);x.sendMessage("§aРанг обновлён.");return true;
   }
   if(!p.authed(id)){x.sendMessage("§cСначала войдите.");return true;}
   switch(name){
    case "fly":if(!p.db().rank(id).atLeast(Rank.VIP)){x.sendMessage("§cТребуется VIP или выше.");return true;}if(a.length>1||(a.length==1&&!a[0].matches("(?i)on|off"))){x.sendMessage("§e/fly [on|off]");return true;}boolean f=a.length==1?a[0].equalsIgnoreCase("on"):!x.getAllowFlight();x.setAllowFlight(f);if(f)x.setFlying(true);else x.setFlying(false);x.sendMessage("§aFly: "+(f?"on":"off"));return true;
    case "van":if(!p.db().rank(id).atLeast(Rank.ADMIN)){x.sendMessage("§cТребуется ADMIN или выше.");return true;}if(a.length>1||(a.length==1&&!a[0].matches("(?i)on|off"))){x.sendMessage("§e/van [on|off]");return true;}boolean v=a.length==1?a[0].equalsIgnoreCase("on"):!p.van(id);p.toggleVan(x,v);x.sendMessage("§aVan: "+(v?"on":"off"));return true;
    case "afk":if(p.van(id)){x.sendMessage("§cПри активном /van режим AFK недоступен.");return true;}boolean af=p.toggleAfk(x);x.sendMessage(af?"§eAFK включён.":"§aAFK выключен.");return true;
    case "tpa":if(a.length!=1){x.sendMessage("§e/tpa <ник>");return true;}Player to=Bukkit.getPlayerExact(a[0]);if(to==null||to==x||!p.authed(to.getUniqueId())){x.sendMessage("§cИгрок не найден или не авторизован.");return true;}if(p.db().blocked(to.getUniqueId(),id)){x.sendMessage("§cИгрок блокирует ваши запросы.");return true;}p.putRequest(to.getUniqueId(),id);to.sendMessage("§e"+x.getName()+" просит телепортироваться к вам: /tpaap или /tapde");x.sendMessage("§aЗапрос отправлен.");return true;
    case "tpaap":case "tapde":UUID from=p.takeRequest(id);if(from==null){x.sendMessage("§cНет ожидающих запросов.");return true;}Player requester=Bukkit.getPlayer(from);if(name.equals("tpaap")&&requester!=null){requester.teleport(x.getLocation());requester.sendMessage("§aЗапрос принят.");}else if(requester!=null)requester.sendMessage("§cЗапрос отклонён.");x.sendMessage(name.equals("tpaap")?"§aЗапрос принят.":"§eЗапрос отклонён.");return true;
    case "tpablock":if(a.length!=1){x.sendMessage("§e/tpablock <ник>");return true;}Player blocked=Bukkit.getPlayerExact(a[0]);if(blocked==null||blocked==x){x.sendMessage("§cИгрок не найден.");return true;}x.sendMessage(p.db().toggleBlock(id,blocked.getUniqueId())?"§aЗапросы игрока заблокированы.":"§aИгрок разблокирован.");return true;
    case "msg":if(a.length<2){x.sendMessage("§e/msg <ник> <сообщение>");return true;}Player recipient=Bukkit.getPlayerExact(a[0]);if(recipient==null||!p.authed(recipient.getUniqueId())){x.sendMessage("§cИгрок не найден.");return true;}String text=String.join(" ",Arrays.copyOfRange(a,1,a.length));recipient.sendMessage("§d[ЛС] "+x.getName()+": §f"+text);x.sendMessage("§d[ЛС] Вы -> "+recipient.getName()+": §f"+text);p.setReply(id,recipient.getUniqueId());p.setReply(recipient.getUniqueId(),id);return true;
    case "r":UUID other=p.replyTarget(id);Player rp=other==null?null:Bukkit.getPlayer(other);if(rp==null||a.length==0){x.sendMessage("§cНет собеседника или пустое сообщение.");return true;}String reply=String.join(" ",a);rp.sendMessage("§d[ЛС] "+x.getName()+": §f"+reply);x.sendMessage("§d[ЛС] Вы -> "+rp.getName()+": §f"+reply);return true;
   }
  }catch(IllegalArgumentException ex){x.sendMessage("§cНеизвестный ранг. Допустимые: CSRV, ADMIN, HELPER, VIP, DEFAULT.");}catch(Exception ex){x.sendMessage("§cОшибка выполнения команды. Проверьте настройки БД.");p.getLogger().warning("Command "+name+": "+ex);}
  return true;
 }
 @Override public List<String> onTabComplete(CommandSender s,Command c,String alias,String[] args){if((name.equals("fly")||name.equals("van"))&&args.length==1)return List.of("on","off").stream().filter(v->v.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();if((name.equals("tpa")||name.equals("tpablock"))&&args.length==1)return Bukkit.getOnlinePlayers().stream().filter(player -> p.authed(player.getUniqueId())).map(Player::getName).filter(v->v.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))).toList();if(name.equals("setrank")&&args.length==2)return Arrays.stream(Rank.values()).map(Enum::name).filter(v->v.startsWith(args[1].toUpperCase(Locale.ROOT))).toList();return Collections.emptyList();}
}
