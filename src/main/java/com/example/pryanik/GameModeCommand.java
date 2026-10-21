package com.example.pryanik;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public final class GameModeCommand implements CommandExecutor, Listener {
    public static final String MENU="§8Выбор режима", INSTANCES="§8Выбор грифа", KEY="pryanik_mode", SELECTOR="mode_compass";
    private final PryanikPlugin plugin;
    public GameModeCommand(PryanikPlugin plugin){this.plugin=plugin;plugin.gameModes().setCompassKey(new NamespacedKey(plugin,"mode_compass"));}
    private ItemStack icon(Material mat,String name,String... lore){ItemStack i=new ItemStack(mat);ItemMeta m=i.getItemMeta();m.setDisplayName(name);m.setLore(Arrays.asList(lore));i.setItemMeta(m);return i;}
    public static ItemStack compass(){return makeCompass(null);}
    public static ItemStack compass(NamespacedKey key){return makeCompass(key);}
    public static boolean isCompass(ItemStack item,NamespacedKey key){if(item==null||item.getType()!=Material.COMPASS||!item.hasItemMeta())return false;ItemMeta meta=item.getItemMeta();return key!=null&&meta.getPersistentDataContainer().has(key,org.bukkit.persistence.PersistentDataType.BYTE);}
    private static ItemStack makeCompass(NamespacedKey key){ItemStack i=new ItemStack(Material.COMPASS);ItemMeta m=i.getItemMeta();m.setDisplayName("§aВыбор режима");m.setLore(List.of("§7Нажмите ПКМ, чтобы выбрать игровой режим"));if(key!=null)m.getPersistentDataContainer().set(key,org.bukkit.persistence.PersistentDataType.BYTE,(byte)1);i.setItemMeta(m);return i;}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){if(!(sender instanceof Player p)){sender.sendMessage("Только игрок.");return true;}if(!plugin.authed(p.getUniqueId())){p.sendMessage("§cСначала войдите.");return true;}openModes(p);return true;}
    public void openModes(Player p){if(!plugin.authed(p.getUniqueId())||!plugin.gameModes().isLobby(p.getWorld())){p.sendMessage("§cВыбор режима доступен только после входа в аккаунт и в лобби.");return;}Inventory inv=Bukkit.createInventory(null,27,MENU);inv.setItem(11,icon(Material.GRASS_BLOCK,"§aВыживание","§720 грифов, до 20 игроков","§7Приваты включены"));inv.setItem(13,icon(Material.IRON_SWORD,"§cХардкор","§75 грифов, до 15 игроков","§7Одна жизнь на гриф"));inv.setItem(15,icon(Material.TNT,"§6Анархия","§715 грифов, до 15 игроков","§7Без приватов"));p.openInventory(inv);}
    private void openInstances(Player p,String mode){Inventory inv=Bukkit.createInventory(null,54,INSTANCES);int count=plugin.modeCount(mode),cap=plugin.modeCapacity(mode);for(int n=1;n<=count;n++){String id=mode+"_"+n;int online=plugin.instanceOnline(id);inv.setItem(n-1,icon(online>=cap?Material.BARRIER:Material.GRASS_BLOCK,"§eГриф "+n,"§7Игроков: "+online+"/"+cap,online>=cap?"§cЗаполнен":"§aНажмите для входа"));}p.getPersistentDataContainer().set(new NamespacedKey(plugin,KEY),org.bukkit.persistence.PersistentDataType.STRING,mode);p.openInventory(inv);}
    @EventHandler public void click(InventoryClickEvent e){if(!(e.getWhoClicked() instanceof Player p))return;String title=e.getView().getTitle();if(!title.equals(MENU)&&!title.equals(INSTANCES))return;e.setCancelled(true);if(!plugin.authed(p.getUniqueId()))return;ItemStack item=e.getCurrentItem();if(item==null||!item.hasItemMeta())return;if(title.equals(MENU)){String n=ChatColor.stripColor(item.getItemMeta().getDisplayName());String mode=n.contains("Выживание")?"survival":n.contains("Хардкор")?"hardcore":n.contains("Анархия")?"anarchy":null;if(mode!=null)openInstances(p,mode);return;}String mode=p.getPersistentDataContainer().get(new NamespacedKey(plugin,KEY),org.bukkit.persistence.PersistentDataType.STRING);String stripped=ChatColor.stripColor(item.getItemMeta().getDisplayName());if(mode==null||stripped==null||!stripped.startsWith("Гриф "))return;try{int n=Integer.parseInt(stripped.substring(5));p.closeInventory();p.getPersistentDataContainer().remove(new NamespacedKey(plugin,KEY));plugin.enterInstance(p,mode,n);}catch(NumberFormatException ignored){}}
    @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=false) public void useCompass(PlayerInteractEvent e){Player p=e.getPlayer();NamespacedKey key=new NamespacedKey(plugin,"mode_compass");if(!isCompass(e.getItem(),key))return;e.setCancelled(true);e.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);e.setUseItemInHand(org.bukkit.event.Event.Result.DENY);if(!e.getAction().isRightClick())return;if(!plugin.authed(p.getUniqueId()))return;if(!plugin.gameModes().isLobby(p.getWorld())){p.sendMessage("§cКомпас выбора работает только в лобби.");return;}openModes(p);}
}
