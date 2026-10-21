package com.example.pryanik.commands;

import com.example.pryanik.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.command.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ClanCommand implements CommandExecutor,Listener {
 private static final String TITLE="§8Управление кланом";
 private final PryanikPlugin plugin;
 private final Map<UUID,UUID> invitations=new ConcurrentHashMap<>();
 public ClanCommand(PryanikPlugin plugin){this.plugin=plugin;}
 private ItemStack item(Material m,String name,String... lore){ItemStack i=new ItemStack(m);ItemMeta meta=i.getItemMeta();meta.setDisplayName(name);if(lore.length>0)meta.setLore(Arrays.asList(lore));i.setItemMeta(meta);return i;}
 private void open(Player p){Inventory inv=Bukkit.createInventory(null,27,TITLE);Clan clan=null;try{clan=plugin.db().clan(p.getUniqueId(),plugin.clanScope(p));}catch(Exception ignored){}inv.setItem(10,item(Material.EMERALD,"§aСоздать клан","§7/clan create <имя>"));inv.setItem(12,item(Material.PAPER,"§eИнформация","§7Показать ваш клан"));inv.setItem(14,item(Material.ENDER_PEARL,"§bДом клана","§7Телепорт: /chome"));inv.setItem(16,item(Material.BARRIER,"§cПокинуть клан","§7/clan leave"));if(clan!=null){inv.setItem(4,item(Material.NAME_TAG,"§6Клан: §f"+clan.name(),"§7Лидер: "+safeName(clan.leader())));inv.setItem(22,item(Material.PLAYER_HEAD,"§aУчастники","§7Нажмите, чтобы просмотреть"));}else inv.setItem(4,item(Material.GRAY_DYE,"§7Вы не состоите в клане"));p.openInventory(inv);}
 private String safeName(UUID id){try{return plugin.db().nickname(id);}catch(Exception e){return id.toString();}}
 @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){if(!(s instanceof Player p)){s.sendMessage("Только игрок.");return true;}if(!plugin.authed(p.getUniqueId())){p.sendMessage("§cСначала войдите.");return true;}if(plugin.clanScope(p).equals("legacy")){p.sendMessage("§cКланы доступны только в lobby или игровом грифе.");return true;}if(a.length==0){open(p);return true;}try{switch(a[0].toLowerCase(Locale.ROOT)){
 case "create":if(a.length!=2||!a[1].matches("[A-Za-z0-9_]{3,16}")){p.sendMessage("§e/clan create <имя 3-16 символов>");return true;}String scope=plugin.clanScope(p);if(plugin.db().clan(p.getUniqueId(),scope)!=null){p.sendMessage("§cВы уже в клане в этом режиме/грифе.");return true;}if(plugin.db().clanNameExists(a[1],scope)){p.sendMessage("§cКлан с таким именем уже есть в этом грифе.");return true;}plugin.db().createClan(UUID.randomUUID(),a[1],p.getUniqueId(),scope);
 p.sendMessage("§aКлан создан.");open(p);return true;
 case "invite":if(a.length!=2)return usage(p);Clan clan=plugin.db().clan(p.getUniqueId(),plugin.clanScope(p));if(clan==null||!clan.leader().equals(p.getUniqueId())){p.sendMessage("§cПриглашать может только лидер.");return true;}Player target=Bukkit.getPlayerExact(a[1]);if(target==null||!plugin.authed(target.getUniqueId())){p.sendMessage("§cИгрок не найден/не авторизован.");return true;}if(plugin.db().clan(target.getUniqueId(),plugin.clanScope(p))!=null){p.sendMessage("§cИгрок уже состоит в клане этого грифа.");return true;}if(!plugin.clanScope(p).equals(plugin.clanScope(target))){p.sendMessage("§cПриглашать можно только игрока из того же грифа.");return true;}plugin.db().putClanInvite(clan.id(),target.getUniqueId());target.sendMessage("§eПриглашение в клан §f"+clan.name()+"§e. Принять: /clan accept");p.sendMessage("§aПриглашение отправлено.");return true;
 case "accept":UUID invited=plugin.db().clanInvite(p.getUniqueId(),plugin.clanScope(p));if(invited==null){p.sendMessage("§cНет приглашений.");return true;}String invitationScope=plugin.db().invitationScope(p.getUniqueId());if(invitationScope==null||!invitationScope.equals(plugin.clanScope(p))){p.sendMessage("§cПриглашение доступно только в том же грифе.");return true;}if(plugin.db().clan(p.getUniqueId(),plugin.clanScope(p))!=null){p.sendMessage("§cВы уже в клане этого грифа.");plugin.db().removeClanInvite(invited,p.getUniqueId());return true;}plugin.db().addClanMember(invited,p.getUniqueId());plugin.db().removeClanInvite(invited,p.getUniqueId());p.sendMessage("§aВы вступили в клан.");return true;
 case "leave":clan=plugin.db().clan(p.getUniqueId(),plugin.clanScope(p));if(clan==null){p.sendMessage("§cВы не в клане.");return true;}if(clan.leader().equals(p.getUniqueId())){p.sendMessage("§cЛидер не может выйти. Используйте /clan disband.");return true;}plugin.db().removeClanMember(clan.id(),p.getUniqueId());p.sendMessage("§eВы покинули клан.");return true;
 case "disband":clan=plugin.db().clan(p.getUniqueId(),plugin.clanScope(p));if(clan==null||!clan.leader().equals(p.getUniqueId())){p.sendMessage("§cТолько лидер может распустить клан.");return true;}plugin.db().deleteClan(clan.id());p.sendMessage("§cКлан распущен.");return true;
 case "info":clan=plugin.db().clan(p.getUniqueId(),plugin.clanScope(p));if(clan==null){p.sendMessage("§cВы не состоите в клане.");return true;}p.sendMessage("§6Клан §e"+clan.name()+" §7| лидер: "+safeName(clan.leader()));p.sendMessage("§7Участники: "+plugin.db().clanMembers(clan.id(),plugin.clanScope(p)).stream().map(this::safeName).reduce((x,y)->x+", "+y).orElse("—"));return true;
 case "sethome":p.performCommand("chome set");return true;
 default:return usage(p);
 }}catch(Exception ex){p.sendMessage("§cОшибка клана. Проверьте БД/аргументы.");plugin.getLogger().warning("Clan command: "+ex);return true;}}
 private boolean usage(Player p){p.sendMessage("§e/clan — меню | create <имя> | invite <ник> | accept | leave | info | disband | sethome");return true;}
 @EventHandler public void click(InventoryClickEvent e){if(!(e.getWhoClicked() instanceof Player p)||!TITLE.equals(e.getView().getTitle()))return;e.setCancelled(true);if(!plugin.authed(p.getUniqueId())){p.closeInventory();return;}switch(e.getRawSlot()){case 10:p.closeInventory();p.sendMessage("§eСоздание: /clan create <имя>");break;case 12:p.closeInventory();p.performCommand("clan info");break;case 14:p.closeInventory();p.performCommand("chome");break;case 16:p.closeInventory();p.performCommand("clan leave");break;case 22:p.closeInventory();p.performCommand("clan info");break;}}
}
