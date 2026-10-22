package com.example.pryanik.commands;

import com.example.pryanik.PryanikPlugin;
import com.example.pryanik.Rank;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** CSRV binds a portal block to a safe player command. */
public final class PortalCommand implements CommandExecutor, Listener {
    private static final String TITLE="§8Настройка портала";
    private static final List<String> OPTIONS=List.of("mode","lobby","kit_start","home","chome");
    private final PryanikPlugin plugin;
    private final NamespacedKey commandKey;
    public PortalCommand(PryanikPlugin plugin){this.plugin=plugin;this.commandKey=new NamespacedKey(plugin,"portal_command");}

    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!(sender instanceof Player player)){sender.sendMessage("Только игрок.");return true;}
        if(!plugin.authed(player.getUniqueId())){player.sendMessage("§cСначала войдите.");return true;}
        try{if(plugin.db().rank(player.getUniqueId())!=Rank.CSRV&&!player.isOp()){player.sendMessage("§cНастраивать порталы может только CSRV.");return true;}}
        catch(Exception ex){player.sendMessage("§cНе удалось проверить ранг.");return true;}
        Block portal=findPortal(player.getLocation());
        if(portal==null){player.sendMessage("§cВстаньте внутрь портала (портала Нижнего мира или Края) и снова выполните /portal.");return true;}
        Inventory inv=Bukkit.createInventory(null,27,TITLE);
        inv.setItem(10,option(Material.COMPASS,"mode","§aОткрыть выбор режима"));
        inv.setItem(11,option(Material.ENDER_PEARL,"lobby","§bПерейти в лобби"));
        inv.setItem(13,option(Material.CHEST,"kit_start","§eВыдать стартовый набор"));
        inv.setItem(15,option(Material.RED_BED,"home","§dОткрыть меню домов"));
        inv.setItem(16,option(Material.BEACON,"chome","§6Открыть дом клана"));
        ItemStack remove=option(Material.BARRIER,"__remove","§cУбрать команду с портала");inv.setItem(22,remove);
        player.openInventory(inv);return true;
    }
    private ItemStack option(Material material,String command,String name){ItemStack item=new ItemStack(material);ItemMeta meta=item.getItemMeta();meta.setDisplayName(name);meta.setLore(List.of("§7При входе будет выполнено: §f/"+command));meta.getPersistentDataContainer().set(commandKey,PersistentDataType.STRING,command);item.setItemMeta(meta);return item;}
    private Block findPortal(Location location){if(location.getWorld()==null)return null;Block center=location.getBlock();Block best=null;double bestDistance=Double.MAX_VALUE;for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++){Block block=center.getRelative(dx,dy,dz);if(block.getType()!=Material.NETHER_PORTAL&&block.getType()!=Material.END_PORTAL)continue;double distance=block.getLocation().add(.5,.5,.5).distanceSquared(location);if(distance<bestDistance){best=block;bestDistance=distance;}}return best;}
    @EventHandler(priority=EventPriority.HIGHEST) public void click(InventoryClickEvent event){if(!TITLE.equals(event.getView().getTitle()))return;event.setCancelled(true);if(!(event.getWhoClicked() instanceof Player admin)||!plugin.authed(admin.getUniqueId())){event.getWhoClicked().closeInventory();return;}try{if(plugin.db().rank(admin.getUniqueId())!=Rank.CSRV&&!admin.isOp()){admin.closeInventory();admin.sendMessage("§cНастраивать порталы может только CSRV.");return;}}catch(Exception ex){admin.closeInventory();return;}if(!(event.getWhoClicked() instanceof Player player)||event.getRawSlot()<0||event.getRawSlot()>=event.getView().getTopInventory().getSize())return;ItemStack clicked=event.getCurrentItem();if(clicked==null||!clicked.hasItemMeta())return;String selected=clicked.getItemMeta().getPersistentDataContainer().get(commandKey,PersistentDataType.STRING);if(selected==null)return;Block portal=findPortal(player.getLocation());if(portal==null){player.closeInventory();player.sendMessage("§cПортал не найден. Встаньте в него и выполните /portal снова.");return;}try{if("__remove".equals(selected)){plugin.db().removePortalCommand(portal.getWorld().getName(),portal.getX(),portal.getY(),portal.getZ());player.sendMessage("§aКоманда удалена с этого блока портала.");}else if(OPTIONS.contains(selected)){plugin.db().setPortalCommand(portal.getWorld().getName(),portal.getX(),portal.getY(),portal.getZ(),selected);player.sendMessage("§aПри входе в портал будет выполнена команда §e/"+selected+"§a.");}}catch(Exception ex){player.sendMessage("§cНе удалось сохранить команду портала.");plugin.getLogger().warning("Portal command assignment: "+ex);}player.closeInventory();}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void enterPortal(PlayerPortalEvent event){Player player=event.getPlayer();if(!plugin.authed(player.getUniqueId()))return;Block portal=findPortal(event.getFrom());if(portal==null)return;try{String command=plugin.db().portalCommand(portal.getWorld().getName(),portal.getX(),portal.getY(),portal.getZ());if(command==null||!OPTIONS.contains(command))return;Bukkit.getScheduler().runTask(plugin,()->{if(!player.isOnline()||!plugin.authed(player.getUniqueId()))return;plugin.runPortalPlayerCommand(player,command);});}catch(Exception ex){plugin.getLogger().warning("Portal command lookup failed: "+ex.getMessage());}}
}
