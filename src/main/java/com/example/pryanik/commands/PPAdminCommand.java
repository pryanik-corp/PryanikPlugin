package com.example.pryanik.commands;

import com.example.pryanik.PryanikPlugin;
import com.example.pryanik.Rank;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

/** CSRV-only in-game controls for grief, lobby and spawn world settings. */
public final class PPAdminCommand implements CommandExecutor, Listener {
    private static final String TITLE="§8Админ: настройки мира";
    private static final String SELECT_TITLE="§8Админ: выберите мир";
    private final PryanikPlugin plugin;
    private final NamespacedKey worldKey,settingKey;
    public PPAdminCommand(PryanikPlugin plugin){this.plugin=plugin;worldKey=new NamespacedKey(plugin,"ppadmin_world");settingKey=new NamespacedKey(plugin,"ppadmin_setting");}
    private boolean allowed(Player p){try{return p.isOp()||plugin.db().rank(p.getUniqueId())==Rank.CSRV;}catch(Exception e){return false;}}
    @Override public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args){if(!(sender instanceof Player p)){sender.sendMessage("Только игрок.");return true;}if(!allowed(p)){p.sendMessage("§cДоступ только CSRV.");return true;}if(args.length==2&&args[0].equalsIgnoreCase("world")){World w=Bukkit.getWorld(args[1]);if(w==null){p.sendMessage("§cМир не загружен.");return true;}openSettings(p,w);return true;}if(args.length==2&&args[0].equalsIgnoreCase("commands")){String world=p.getWorld().getName();String value=args[1].equalsIgnoreCase("reset")?"":args[1].toLowerCase(Locale.ROOT);if(!value.isEmpty()&&!value.equals("all")&&!value.equals("none")&&!value.matches("[a-z0-9_,-]{1,512}")){p.sendMessage("§cФормат: /ppadmin commands <all|none|команда,команда,...>");return true;}plugin.getConfig().set("admin-worlds."+world+".allowed-commands",value);plugin.saveConfig();p.sendMessage("§aНастройка списка команд сохранена для мира §e"+world+"§a.");p.sendMessage("§7Список применяется в мире автоматически. Обязательные команды авторизации и /ppadmin остаются доступны.");return true;}openWorlds(p);return true;}
    private void openWorlds(Player p){List<World> worlds=new ArrayList<>(Bukkit.getWorlds());Inventory inv=Bukkit.createInventory(null,Math.min(54,Math.max(9,((worlds.size()+8)/9)*9)),SELECT_TITLE);int slot=0;for(World w:worlds){if(slot>=54)break;ItemStack i=new ItemStack(Material.GRASS_BLOCK);ItemMeta m=i.getItemMeta();m.setDisplayName("§a"+w.getName());m.setLore(List.of("§7Игроков: "+w.getPlayers().size(),"§eНажмите для настроек"));m.getPersistentDataContainer().set(worldKey,PersistentDataType.STRING,w.getName());i.setItemMeta(m);inv.setItem(slot++,i);}p.openInventory(inv);}
    private void openSettings(Player p,World w){Inventory inv=Bukkit.createInventory(null,27,TITLE);set(inv,10,Material.IRON_SWORD,"pvp",w.getPVP()?"§aPvP: включено":"§cPvP: выключено");set(inv,11,Material.OBSIDIAN,"block-place",flagLabel(w,"block-place"));set(inv,12,Material.IRON_PICKAXE,"block-break",flagLabel(w,"block-break"));set(inv,13,Material.CHEST,"interact",flagLabel(w,"interact"));set(inv,14,Material.COMPASS,"spawn","§eУстановить точку спавна здесь");set(inv,15,Material.PAPER,"commands","§eРазрешённые команды — /ppadmin commands");set(inv,22,Material.ARROW,"back","§7Назад к мирам");for(int slot=0;slot<inv.getSize();slot++){ItemStack entry=inv.getItem(slot);if(entry==null||!entry.hasItemMeta())continue;ItemMeta entryMeta=entry.getItemMeta();entryMeta.getPersistentDataContainer().set(worldKey,PersistentDataType.STRING,w.getName());entry.setItemMeta(entryMeta);}p.openInventory(inv);}
    private String flagLabel(World w,String flag){String label=switch(flag){case "block-place"->"Строительство";case "block-break"->"Ломать блоки";default->"Взаимодействие";};return "§e"+label+": "+(enabled(w,flag)?"§aразрешено":"§cзапрещено");}
    private void set(Inventory inv,int slot,Material mat,String key,String label){ItemStack i=new ItemStack(mat);ItemMeta m=i.getItemMeta();m.setDisplayName(label);m.getPersistentDataContainer().set(settingKey,PersistentDataType.STRING,key);i.setItemMeta(m);inv.setItem(slot,i);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void allowedCommands(PlayerCommandPreprocessEvent e){String path="admin-worlds."+e.getPlayer().getWorld().getName()+".allowed-commands";String config=plugin.getConfig().getString(path,"").trim();if(config.isEmpty()||config.equalsIgnoreCase("all")||allowed(e.getPlayer())||e.getPlayer().hasPermission("pryanik.admin.bypass-world-commands"))return;String root=e.getMessage().substring(1).split("\\s+",2)[0].toLowerCase(Locale.ROOT);if(root.contains(":"))root=root.substring(root.indexOf(':')+1);if(root.equals("ppadmin")||root.equals("l")||root.equals("login")||root.equals("auth"))return;if(config.equalsIgnoreCase("none")||!Arrays.stream(config.split(",")).map(String::trim).anyMatch(root::equals) ){e.setCancelled(true);e.getPlayer().sendMessage("§cКоманда /"+root+" не разрешена в этом мире.");}}
    private boolean enabled(World w,String flag){return plugin.getConfig().getBoolean("admin-worlds."+w.getName()+"."+flag,plugin.getConfig().getBoolean("admin-worlds.defaults."+flag,true));}
    private boolean protectedAdminRegion(World w){return "lobby".equalsIgnoreCase(w.getName())||plugin.gameModes().isSpawnWorld(w);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void blockPlace(BlockPlaceEvent e){World w=e.getBlock().getWorld();if(!enabled(w,"block-place")&&!protectedAdminRegion(w))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void blockBreak(BlockBreakEvent e){World w=e.getBlock().getWorld();if(!enabled(w,"block-break")&&!protectedAdminRegion(w))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void interact(PlayerInteractEvent e){if(e.getClickedBlock()!=null){World w=e.getClickedBlock().getWorld();if(!enabled(w,"interact")&&!protectedAdminRegion(w))e.setCancelled(true);}}
    @EventHandler public void click(InventoryClickEvent e){String title=e.getView().getTitle();if(!TITLE.equals(title)&&!SELECT_TITLE.equals(title))return;e.setCancelled(true);if(!(e.getWhoClicked() instanceof Player p)||!allowed(p)){e.getWhoClicked().closeInventory();return;}if(e.getRawSlot()<0||e.getRawSlot()>=e.getView().getTopInventory().getSize())return;ItemStack item=e.getCurrentItem();if(item==null||!item.hasItemMeta())return;var data=item.getItemMeta().getPersistentDataContainer();try{if(SELECT_TITLE.equals(title)){String name=data.get(worldKey,PersistentDataType.STRING);World w=name==null?null:Bukkit.getWorld(name);if(w!=null)openSettings(p,w);return;}String name=data.get(worldKey,PersistentDataType.STRING);String setting=data.get(settingKey,PersistentDataType.STRING);World w=name==null?null:Bukkit.getWorld(name);if(w==null)return;if("back".equals(setting)){openWorlds(p);return;}switch(setting==null?"":setting){case "pvp"->{String scope=plugin.gameModes().instanceOf(w.getName());boolean enabled=!w.getPVP();if(scope!=null)try{plugin.db().setPvpEnabled(scope,enabled);}catch(Exception saveError){p.sendMessage("§cСохранить PvP в базе не удалось.");break;}for(World linked:Bukkit.getWorlds())if(scope!=null?scope.equals(plugin.gameModes().instanceOf(linked.getName()))&&!plugin.gameModes().isSpawnWorld(linked):linked.equals(w))linked.setPVP(enabled);}case "block-place","block-break","interact"->{String path="admin-worlds."+w.getName()+"."+setting;plugin.getConfig().set(path,!plugin.getConfig().getBoolean(path,plugin.getConfig().getBoolean("admin-worlds.defaults."+setting,true)));plugin.saveConfig();}case "spawn"->{Location loc=p.getLocation();if(loc.getWorld().equals(w)){w.setSpawnLocation(loc);String path="admin-worlds."+w.getName()+".spawn.";plugin.getConfig().set(path+"x",loc.getX());plugin.getConfig().set(path+"y",loc.getY());plugin.getConfig().set(path+"z",loc.getZ());plugin.getConfig().set(path+"yaw",loc.getYaw());plugin.getConfig().set(path+"pitch",loc.getPitch());if("lobby".equalsIgnoreCase(w.getName())){String old="admin-worlds.lobby-spawn.";plugin.getConfig().set(old+"x",loc.getX());plugin.getConfig().set(old+"y",loc.getY());plugin.getConfig().set(old+"z",loc.getZ());}plugin.saveConfig();p.sendMessage("§aТочка спавна мира §e"+w.getName()+"§a обновлена.");}else p.sendMessage("§cВстаньте в выбранном мире, чтобы установить его спавн.");}default->{}}openSettings(p,w);}catch(Exception ex){p.sendMessage("§cНе удалось применить настройку.");plugin.getLogger().warning("PPAdmin setting failed: "+ex.getMessage());}}
}
