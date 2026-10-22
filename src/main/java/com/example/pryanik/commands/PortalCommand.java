package com.example.pryanik.commands;

import com.example.pryanik.PryanikPlugin;
import com.example.pryanik.Rank;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
    private final Map<UUID,PendingPortal> pending=new ConcurrentHashMap<>();
    private final Map<UUID,PortalTrigger> portalTriggers=new ConcurrentHashMap<>();
    private record PortalTrigger(String world,int x,int y,int z){}
    private record PendingPortal(String world,int x,int y,int z,boolean unlit,boolean alongX,int width,int height){}
    public PortalCommand(PryanikPlugin plugin){this.plugin=plugin;this.commandKey=new NamespacedKey(plugin,"portal_command");}

    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!(sender instanceof Player player)){sender.sendMessage("Только игрок.");return true;}
        if(!plugin.authed(player.getUniqueId())){player.sendMessage("§cСначала войдите.");return true;}
        try{if(plugin.db().rank(player.getUniqueId())!=Rank.CSRV&&!player.isOp()){player.sendMessage("§cНастраивать порталы может только CSRV.");return true;}}
        catch(Exception ex){player.sendMessage("§cНе удалось проверить ранг.");return true;}
        Block portal=findPortal(player.getLocation());
        PendingPortal target=null;
        if(portal==null){
            target=findUnlitFrame(player.getLocation());
            if(target==null){player.sendMessage("§cНе найден портал. Встаньте в проём полной обсидиановой рамки и выполните /portal.");return true;}
            portal=player.getWorld().getBlockAt(target.x(),target.y(),target.z());
        }
        pending.put(player.getUniqueId(),new PendingPortal(portal.getWorld().getName(),portal.getX(),portal.getY(),portal.getZ(),target!=null,target!=null&&target.alongX(),target==null?0:target.width(),target==null?0:target.height()));
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

    /** Find a complete unlit obsidian frame, leaving it untouched until the UI choice is confirmed. */
    private PendingPortal findUnlitFrame(Location location){
        World world=location.getWorld();if(world==null)return null;
        Block origin=location.getBlock();PendingPortal match=null;
        for(int ox=-4;ox<=4;ox++)for(int oy=-4;oy<=4;oy++)for(int oz=-4;oz<=4;oz++){
            Block base=origin.getRelative(ox,oy,oz);
            for(boolean alongX:new boolean[]{true,false}){
                PendingPortal candidate=inspectFrame(base,alongX);
                if(candidate==null)continue;
                if(match!=null&&!match.equals(candidate))return null;
                match=candidate;
            }
        }
        return match;
    }

    private PendingPortal inspectFrame(Block base,boolean alongX){
        if(!isFrame(base,2,3,alongX))return null;
        int width=2,height=3;
        while(width<21&&isFrame(base,width+1,height,alongX))width++;
        while(height<21&&isFrame(base,width,height+1,alongX))height++;
        int px=alongX?1:0,pz=alongX?0:1;
        for(int x=0;x<width;x++)for(int y=0;y<height;y++){
            Material type=base.getRelative(px*x,y,pz*x).getType();
            if(type!=Material.AIR&&type!=Material.FIRE)return null;
        }
        return new PendingPortal(base.getWorld().getName(),base.getX(),base.getY(),base.getZ(),true,alongX,width,height);
    }

    private boolean igniteFrame(PendingPortal frame){
        World world=Bukkit.getWorld(frame.world());if(world==null)return false;
        Block base=world.getBlockAt(frame.x(),frame.y(),frame.z());
        if(!isFrame(base,frame.width(),frame.height(),frame.alongX()))return false;
        int px=frame.alongX()?1:0,pz=frame.alongX()?0:1;
        for(int x=0;x<frame.width();x++)for(int y=0;y<frame.height();y++){
            Material type=base.getRelative(px*x,y,pz*x).getType();
            if(type!=Material.AIR&&type!=Material.FIRE&&type!=Material.NETHER_PORTAL)return false;
        }
        for(int x=0;x<frame.width();x++)for(int y=0;y<frame.height();y++){
            Block inside=base.getRelative(px*x,y,pz*x);
            inside.setType(Material.NETHER_PORTAL,false);
            if(inside.getBlockData() instanceof Orientable orientable){orientable.setAxis(frame.alongX()?org.bukkit.Axis.X:org.bukkit.Axis.Z);inside.setBlockData(orientable,false);}
        }
        return true;
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
    @EventHandler(priority=EventPriority.HIGHEST) public void click(InventoryClickEvent event){if(!TITLE.equals(event.getView().getTitle()))return;event.setCancelled(true);if(!(event.getWhoClicked() instanceof Player admin)||!plugin.authed(admin.getUniqueId())){event.getWhoClicked().closeInventory();return;}try{if(plugin.db().rank(admin.getUniqueId())!=Rank.CSRV&&!admin.isOp()){admin.closeInventory();admin.sendMessage("§cНастраивать порталы может только CSRV.");return;}}catch(Exception ex){admin.closeInventory();return;}if(event.getRawSlot()<0||event.getRawSlot()>=event.getView().getTopInventory().getSize())return;ItemStack clicked=event.getCurrentItem();if(clicked==null||!clicked.hasItemMeta())return;String selected=clicked.getItemMeta().getPersistentDataContainer().get(commandKey,PersistentDataType.STRING);if(selected==null)return;PendingPortal target=pending.remove(admin.getUniqueId());if(target==null){admin.closeInventory();admin.sendMessage("§cНастройка устарела. Повторите /portal.");return;}try{if("__remove".equals(selected)){plugin.db().removePortalCommand(target.world(),target.x(),target.y(),target.z());admin.sendMessage("§aКоманда удалена с этого портала.");}else if(OPTIONS.contains(selected)){if(target.unlit()&&!igniteFrame(target)){admin.sendMessage("§cРамка изменилась; команда не назначена. Выполните /portal снова.");admin.closeInventory();return;}plugin.db().setPortalCommand(target.world(),target.x(),target.y(),target.z(),selected);admin.sendMessage("§aНазначена команда §e/"+selected+"§a.");if(target.unlit())admin.sendMessage("§aПортал зажжён!");}}catch(Exception ex){admin.sendMessage("§cНе удалось сохранить команду портала.");plugin.getLogger().warning("Portal command assignment: "+ex);}admin.closeInventory();}
    private boolean isPortal(Location location){if(location.getWorld()==null)return false;Material type=location.getBlock().getType();return type==Material.NETHER_PORTAL||type==Material.END_PORTAL;}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void touchPortal(PlayerMoveEvent event){Player player=event.getPlayer();UUID id=player.getUniqueId();Location from=event.getFrom(),to=event.getTo();if(to==null)return;boolean wasInside=isPortal(from),inside=isPortal(to);if(!inside){portalTriggers.remove(id);return;}if(wasInside||!plugin.authed(id)||portalTriggers.containsKey(id))return;Block portal=findPortal(to);if(portal==null)return;PortalTrigger trigger=new PortalTrigger(portal.getWorld().getName(),portal.getX(),portal.getY(),portal.getZ());try{String command=plugin.db().portalCommand(trigger.world(),trigger.x(),trigger.y(),trigger.z());if(command==null||!OPTIONS.contains(command))return;portalTriggers.put(id,trigger);event.setCancelled(true);plugin.runPortalPlayerCommand(player,command);}catch(Exception ex){plugin.getLogger().warning("Portal command lookup failed: "+ex.getMessage());}}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void enterPortal(PlayerPortalEvent event){Player player=event.getPlayer();if(!plugin.authed(player.getUniqueId()))return;Block portal=findPortal(event.getFrom());if(portal==null)return;try{String command=plugin.db().portalCommand(portal.getWorld().getName(),portal.getX(),portal.getY(),portal.getZ());if(command==null||!OPTIONS.contains(command))return;event.setCancelled(true);if(!portalTriggers.containsKey(player.getUniqueId())){portalTriggers.put(player.getUniqueId(),new PortalTrigger(portal.getWorld().getName(),portal.getX(),portal.getY(),portal.getZ()));plugin.runPortalPlayerCommand(player,command);}}catch(Exception ex){plugin.getLogger().warning("Portal command lookup failed: "+ex.getMessage());}}
    @EventHandler public void clearPortalTrigger(PlayerQuitEvent event){portalTriggers.remove(event.getPlayer().getUniqueId());}
}
