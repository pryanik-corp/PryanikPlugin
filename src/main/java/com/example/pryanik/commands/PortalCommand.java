package com.example.pryanik.commands;

import com.example.pryanik.PryanikPlugin;
import com.example.pryanik.Rank;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** CSRV binds a portal block to a safe player command. */
public final class PortalCommand implements CommandExecutor, Listener {
    private static final String TITLE="§8Настройка портала";
    private static final List<String> OPTIONS=List.of("mode","lobby","kit_start","home","chome","rtp");
    private static final int MAX_FRAME_SIZE=64;
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
        inv.setItem(20,option(Material.ENDER_EYE,"rtp","§cСлучайная телепортация"));
        inv.setItem(22,option(Material.BARRIER,"__remove","§cУбрать команду с портала"));
        player.openInventory(inv);
    }

    /** Find a complete unlit obsidian frame, leaving it untouched until the UI choice is confirmed. */
    private PendingPortal findUnlitFrame(Location location){
        World world=location.getWorld();if(world==null)return null;
        Block origin=location.getBlock();PendingPortal match=null;
        for(int ox=-16;ox<=16;ox++)for(int oy=-8;oy<=8;oy++)for(int oz=-16;oz<=16;oz++){
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
        while(width<64&&isFrame(base,width+1,height,alongX))width++;
        while(height<64&&isFrame(base,width,height+1,alongX))height++;
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

    private List<Block> configuredPortalBlocks(PendingPortal portal){World world=Bukkit.getWorld(portal.world());if(world==null)return List.of();Block origin=world.getBlockAt(portal.x(),portal.y(),portal.z());if(origin.getType()!=Material.NETHER_PORTAL&&origin.getType()!=Material.END_PORTAL)return List.of(origin);Material type=origin.getType();List<Block> found=new java.util.ArrayList<>();java.util.ArrayDeque<Block> queue=new java.util.ArrayDeque<>();Set<String> seen=new java.util.HashSet<>();queue.add(origin);while(!queue.isEmpty()&&found.size()<4096){Block b=queue.removeFirst();String key=b.getX()+":"+b.getY()+":"+b.getZ();if(!seen.add(key)||b.getType()!=type)continue;found.add(b);for(int[] d:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}}){Block next=b.getRelative(d[0],d[1],d[2]);if(!seen.contains(next.getX()+":"+next.getY()+":"+next.getZ())&&next.getType()==type)queue.add(next);}}return found.isEmpty()?List.of(origin):found;}
    private List<Block> framePortalBlocks(PendingPortal frame){World world=Bukkit.getWorld(frame.world());if(world==null)return List.of();Block base=world.getBlockAt(frame.x(),frame.y(),frame.z());int px=frame.alongX()?1:0,pz=frame.alongX()?0:1;List<Block> blocks=new java.util.ArrayList<>();for(int x=0;x<frame.width();x++)for(int y=0;y<frame.height();y++)blocks.add(base.getRelative(px*x,y,pz*x));return blocks;}

    private ItemStack option(Material material,String command,String name){ItemStack item=new ItemStack(material);ItemMeta meta=item.getItemMeta();meta.setDisplayName(name);meta.setLore(List.of("§7При входе будет выполнено: §f/"+command));meta.getPersistentDataContainer().set(commandKey,PersistentDataType.STRING,command);item.setItemMeta(meta);return item;}
    private Block findPortal(Location location){if(location.getWorld()==null)return null;Block center=location.getBlock();Block best=null;double bestDistance=Double.MAX_VALUE;for(int dx=-8;dx<=8;dx++)for(int dy=-8;dy<=8;dy++)for(int dz=-8;dz<=8;dz++){Block block=center.getRelative(dx,dy,dz);if(block.getType()!=Material.NETHER_PORTAL&&block.getType()!=Material.END_PORTAL)continue;double distance=block.getLocation().add(.5,.5,.5).distanceSquared(location);if(distance<bestDistance){best=block;bestDistance=distance;}}return best;}
    @EventHandler(priority=EventPriority.HIGHEST) public void click(InventoryClickEvent event){if(!TITLE.equals(event.getView().getTitle()))return;event.setCancelled(true);if(!(event.getWhoClicked() instanceof Player admin)||!plugin.authed(admin.getUniqueId())){event.getWhoClicked().closeInventory();return;}try{if(plugin.db().rank(admin.getUniqueId())!=Rank.CSRV&&!admin.isOp()){admin.closeInventory();admin.sendMessage("§cНастраивать порталы может только CSRV.");return;}}catch(Exception ex){admin.closeInventory();return;}if(event.getRawSlot()<0||event.getRawSlot()>=event.getView().getTopInventory().getSize())return;ItemStack clicked=event.getCurrentItem();if(clicked==null||!clicked.hasItemMeta())return;String selected=clicked.getItemMeta().getPersistentDataContainer().get(commandKey,PersistentDataType.STRING);if(selected==null)return;PendingPortal target=pending.remove(admin.getUniqueId());if(target==null){admin.closeInventory();admin.sendMessage("§cНастройка устарела. Повторите /portal.");return;}try{if("__remove".equals(selected)){for(Block block:configuredPortalBlocks(target))plugin.db().removePortalCommand(target.world(),block.getX(),block.getY(),block.getZ());admin.sendMessage("§aКоманда удалена с этого портала.");}else if(OPTIONS.contains(selected)){if(target.unlit()&&!igniteFrame(target)){admin.sendMessage("§cРамка изменилась; команда не назначена. Выполните /portal снова.");admin.closeInventory();return;}List<Block> portalBlocks=target.unlit()?framePortalBlocks(target):configuredPortalBlocks(target);for(Block block:portalBlocks)plugin.db().setPortalCommand(target.world(),block.getX(),block.getY(),block.getZ(),selected);admin.sendMessage("§aНазначена команда §e/"+selected+"§a для портала ("+portalBlocks.size()+" блоков).");if(target.unlit())admin.sendMessage("§aПортал зажжён!");}}catch(Exception ex){admin.sendMessage("§cНе удалось сохранить команду портала.");plugin.getLogger().warning("Portal command assignment: "+ex);}admin.closeInventory();}
    private boolean isPortal(Location location){if(location.getWorld()==null)return false;Material type=location.getBlock().getType();return type==Material.NETHER_PORTAL||type==Material.END_PORTAL;}
    private PortalAction findPortalAction(Location location)throws Exception{Block portal=findPortal(location);if(portal==null)return null;PortalTrigger trigger=new PortalTrigger(portal.getWorld().getName(),portal.getX(),portal.getY(),portal.getZ());String command=plugin.db().portalCommand(trigger.world(),trigger.x(),trigger.y(),trigger.z());if(command==null||!OPTIONS.contains(command)){for(Block block:configuredPortalBlocks(new PendingPortal(trigger.world(),trigger.x(),trigger.y(),trigger.z(),false,false,0,0))){command=plugin.db().portalCommand(trigger.world(),block.getX(),block.getY(),block.getZ());if(command!=null&&OPTIONS.contains(command))break;}}return command==null||!OPTIONS.contains(command)?null:new PortalAction(trigger,command);}
    private void completePortalAction(Player player,PortalAction action){if(!player.isOnline()||!plugin.authed(player.getUniqueId()))return;Location exit=safePortalExit(player,action.trigger());if(exit==null){player.sendMessage("§cНе найдено безопасное место рядом с порталом.");return;}boolean wasInvulnerable=player.isInvulnerable();player.setInvulnerable(true);player.setPortalCooldown(1200);if(!player.teleport(exit,PlayerTeleportEvent.TeleportCause.PLUGIN)){player.setInvulnerable(wasInvulnerable);return;}Bukkit.getScheduler().runTaskLater(plugin,()->{if(!player.isOnline()||!plugin.authed(player.getUniqueId())){if(player.isOnline())player.setInvulnerable(wasInvulnerable);return;}if("mode".equals(action.command())&&!plugin.gameModes().isLobby(player.getWorld())){plugin.gameModes().goHub(player,()->{Bukkit.getScheduler().runTaskLater(plugin,()->{if(player.isOnline()){player.setInvulnerable(true);new com.example.pryanik.GameModeCommand(plugin).openModes(player);}},1L);});return;}plugin.runPortalPlayerCommand(player,action.command());if(!"mode".equals(action.command()))player.setInvulnerable(wasInvulnerable);},1L);}
    private Location safePortalExit(Player player,PortalTrigger trigger){World world=Bukkit.getWorld(trigger.world());if(world==null)return null;Block origin=world.getBlockAt(trigger.x(),trigger.y(),trigger.z());int[][] offsets={{1,0,0},{-1,0,0},{0,0,1},{0,0,-1},{1,1,0},{-1,1,0},{0,1,1},{0,1,-1},{1,0,1},{-1,0,1},{1,0,-1},{-1,0,-1},{0,1,0},{0,2,0}};for(int[] offset:offsets){Block feet=origin.getRelative(offset[0],offset[1],offset[2]);if(feet.getType()==Material.NETHER_PORTAL||feet.getType()==Material.END_PORTAL||feet.getType().isSolid())continue;Block head=feet.getRelative(0,1,0);if(head.getType()==Material.NETHER_PORTAL||head.getType()==Material.END_PORTAL||head.getType().isSolid())continue;Location candidate=feet.getLocation().add(.5,0,.5);candidate.setYaw(player.getLocation().getYaw());candidate.setPitch(player.getLocation().getPitch());return candidate;}return null;}
    private record PortalAction(PortalTrigger trigger,String command){}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void touchPortal(PlayerMoveEvent event){Player player=event.getPlayer();UUID id=player.getUniqueId();Location from=event.getFrom(),to=event.getTo();if(to==null)return;if(!isPortal(to)){portalTriggers.remove(id);return;}if(isPortal(from)||!plugin.authed(id)||portalTriggers.containsKey(id))return;try{PortalAction action=findPortalAction(to);if(action==null)return;portalTriggers.put(id,action.trigger());player.setPortalCooldown(1200);Bukkit.getScheduler().runTask(plugin,()->completePortalAction(player,action));}catch(Exception ex){plugin.getLogger().warning("Portal command lookup failed: "+ex.getMessage());}}
    @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=true) public void runPortalAction(PlayerTeleportEvent event){if(event.getCause()!=PlayerTeleportEvent.TeleportCause.NETHER_PORTAL&&event.getCause()!=PlayerTeleportEvent.TeleportCause.END_PORTAL)return;Player player=event.getPlayer();PortalTrigger trigger=portalTriggers.remove(player.getUniqueId());PortalAction action=null;try{if(trigger!=null){String command=plugin.db().portalCommand(trigger.world(),trigger.x(),trigger.y(),trigger.z());if(command!=null&&OPTIONS.contains(command))action=new PortalAction(trigger,command);}if(action==null)action=findPortalAction(event.getFrom());}catch(Exception ex){plugin.getLogger().warning("Portal command lookup failed: "+ex.getMessage());return;}if(action==null)return;event.setCancelled(true);player.setPortalCooldown(1200);PortalAction selectedAction=action;Bukkit.getScheduler().runTask(plugin,()->completePortalAction(player,selectedAction));}

    @EventHandler public void clearPortalTrigger(PlayerQuitEvent event){portalTriggers.remove(event.getPlayer().getUniqueId());}
}
