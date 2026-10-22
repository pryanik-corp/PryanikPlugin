package com.example.pryanik.commands;

import com.example.pryanik.PryanikPlugin;
import com.example.pryanik.Rank;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.Orientable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

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
        if(portal==null){
            portal=igniteNetherPortal(player.getLocation());
            if(portal==null){player.sendMessage("§cНе найден портал. Встаньте в отверстие обсидиановой рамки (4×5 или больше) и выполните /portal; рамка будет зажжена автоматически.");return true;}
            player.sendMessage("§aПортал зажжён!");
        }
        openMenu(player);
        return true;
    }

    private void openMenu(Player player){
        Inventory inv=Bukkit.createInventory(null,27,TITLE);
        inv.setItem(10,option(Material.COMPASS,"mode","§aОткрыть выбор режима"));
        inv.setItem(11,option(Material.ENDER_PEARL,"lobby","§bПерейти в лобби"));
        inv.setItem(13,option(Material.CHEST,"kit_start","§eВыдать стартовый набор"));
        inv.setItem(15,option(Material.RED_BED,"home","§dОткрыть меню домов"));
        inv.setItem(16,option(Material.BEACON,"chome","§6Открыть дом клана"));
        inv.setItem(22,option(Material.BARRIER,"__remove","§cУбрать команду с портала"));
        player.openInventory(inv);
    }

    /** Locate a valid, empty obsidian frame around the command user and light it. */
    private Block igniteNetherPortal(Location location){
        World world=location.getWorld();if(world==null)return null;
        Block origin=location.getBlock();
        // Search nearby horizontal frame planes; frame may be aligned on either axis.
        Block match=null;
        for(int ox=-4;ox<=4;ox++)for(int oy=-4;oy<=4;oy++)for(int oz=-4;oz<=4;oz++){
            Block base=origin.getRelative(ox,oy,oz);
            for(boolean alongX:new boolean[]{true,false}){
                if(!isFrame(base,2,3,alongX))continue;
                Block candidate=buildPortal(base,alongX);
                if(candidate==null)continue;
                if(match!=null&&!match.equals(candidate))return null;
                match=candidate;
            }
        }
        return match;
    }

    private Block buildPortal(Block base,boolean alongX){
        // The minimum opening defines frame candidacy; extend across matching obsidian boundaries.
        if(!isFrame(base,2,3,alongX))return null;
        int width=2,height=3;
        while(width<21&&isFrame(base,width+1,height,alongX))width++;
        while(height<21&&isFrame(base,width,height+1,alongX))height++;
        int px=alongX?1:0,pz=alongX?0:1;
        for(int x=0;x<width;x++)for(int y=0;y<height;y++){
            Block inside=base.getRelative(px*x,y,pz*x);
            if(inside.getType()!=Material.AIR&&inside.getType()!=Material.FIRE)return null;
        }
        for(int x=0;x<width;x++)for(int y=0;y<height;y++){
            Block inside=base.getRelative(px*x,y,pz*x);
            inside.setType(Material.NETHER_PORTAL,false);
            if(inside.getBlockData() instanceof Orientable orientable){orientable.setAxis(alongX?org.bukkit.Axis.X:org.bukkit.Axis.Z);inside.setBlockData(orientable,false);}
        }
        return base;
    }

    private boolean isFrame(Block base,int width,int height,boolean alongX){
        int px=alongX?1:0,pz=alongX?0:1;
        for(int x=-1;x<=width;x++)for(int y=-1;y<=height;y++){
            boolean edge=x==-1||x==width||y==-1||y==height;
            Block b=base.getRelative(px*x,y,pz*x);
            if(edge){if(b.getType()!=Material.OBSIDIAN)return false;}
            else if(b.getType()!=Material.AIR&&b.getType()!=Material.FIRE)return false;
        }
        return true;
    }

    private ItemStack option(Material material,String command,String name){ItemStack item=new ItemStack(material);ItemMeta meta=item.getItemMeta();meta.setDisplayName(name);meta.setLore(List.of("§7При входе будет выполнено: §f/"+command));meta.getPersistentDataContainer().set(commandKey,PersistentDataType.STRING,command);item.setItemMeta(meta);return item;}
    private Block findPortal(Location location){if(location.getWorld()==null)return null;Block center=location.getBlock();Block best=null;double bestDistance=Double.MAX_VALUE;for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++){Block block=center.getRelative(dx,dy,dz);if(block.getType()!=Material.NETHER_PORTAL&&block.getType()!=Material.END_PORTAL)continue;double distance=block.getLocation().add(.5,.5,.5).distanceSquared(location);if(distance<bestDistance){best=block;bestDistance=distance;}}return best;}
    @EventHandler(priority=EventPriority.HIGHEST) public void click(InventoryClickEvent event){if(!TITLE.equals(event.getView().getTitle()))return;event.setCancelled(true);if(!(event.getWhoClicked() instanceof Player admin)||!plugin.authed(admin.getUniqueId())){event.getWhoClicked().closeInventory();return;}try{if(plugin.db().rank(admin.getUniqueId())!=Rank.CSRV&&!admin.isOp()){admin.closeInventory();admin.sendMessage("§cНастраивать порталы может только CSRV.");return;}}catch(Exception ex){admin.closeInventory();return;}if(event.getRawSlot()<0||event.getRawSlot()>=event.getView().getTopInventory().getSize())return;ItemStack clicked=event.getCurrentItem();if(clicked==null||!clicked.hasItemMeta())return;String selected=clicked.getItemMeta().getPersistentDataContainer().get(commandKey,PersistentDataType.STRING);if(selected==null)return;Block portal=findPortal(admin.getLocation());if(portal==null){admin.closeInventory();admin.sendMessage("§cПортал не найден. Встаньте в него и выполните /portal снова.");return;}try{if("__remove".equals(selected)){plugin.db().removePortalCommand(portal.getWorld().getName(),portal.getX(),portal.getY(),portal.getZ());admin.sendMessage("§aКоманда удалена с этого блока портала.");}else if(OPTIONS.contains(selected)){plugin.db().setPortalCommand(portal.getWorld().getName(),portal.getX(),portal.getY(),portal.getZ(),selected);admin.sendMessage("§aПри входе в портал будет выполнена команда §e/"+selected+"§a.");}}catch(Exception ex){admin.sendMessage("§cНе удалось сохранить команду портала.");plugin.getLogger().warning("Portal command assignment: "+ex);}admin.closeInventory();}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void enterPortal(PlayerPortalEvent event){Player player=event.getPlayer();if(!plugin.authed(player.getUniqueId()))return;Block portal=findPortal(event.getFrom());if(portal==null)return;try{String command=plugin.db().portalCommand(portal.getWorld().getName(),portal.getX(),portal.getY(),portal.getZ());if(command==null||!OPTIONS.contains(command))return;Bukkit.getScheduler().runTask(plugin,()->{if(!player.isOnline()||!plugin.authed(player.getUniqueId()))return;plugin.runPortalPlayerCommand(player,command);});}catch(Exception ex){plugin.getLogger().warning("Portal command lookup failed: "+ex.getMessage());}}
}
