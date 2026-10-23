package com.example.pryanik.commands;

import com.example.pryanik.PryanikPlugin;
import com.example.pryanik.Rank;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Orientable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** CSRV binds a portal frame to a safe player command. */
public final class PortalCommand implements CommandExecutor, Listener {
    private static final String TITLE="§8Настройка портала";
    private static final List<String> OPTIONS=List.of("mode","lobby","kit_start","home","chome","rtp");
    private static final int MIN_FRAME_WIDTH=1,MAX_FRAME_WIDTH=21,MIN_FRAME_HEIGHT=2,MAX_FRAME_HEIGHT=21;
    private final PryanikPlugin plugin;
    private final NamespacedKey commandKey,ignitionKey;
    private final Map<UUID,PendingPortal> pending=new ConcurrentHashMap<>();
    private final Map<UUID,PortalTrigger> portalTriggers=new ConcurrentHashMap<>();
    private record PortalTrigger(String world,int x,int y,int z){}
    private record PendingPortal(String world,int x,int y,int z,boolean unlit,boolean alongX,int width,int height){}
    private record PortalAction(PortalTrigger trigger,String command){}

    public PortalCommand(PryanikPlugin plugin){
        this.plugin=plugin;commandKey=new NamespacedKey(plugin,"portal_command");ignitionKey=new NamespacedKey(plugin,"portal_ignition_command");
    }

    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!(sender instanceof Player player)){sender.sendMessage("Только игрок.");return true;}
        if(!plugin.authed(player.getUniqueId())){player.sendMessage("§cСначала войдите.");return true;}
        if(!hasPortalSetupPermission(player)){player.sendMessage("§cНастраивать порталы может только CSRV.");return true;}
        pending.remove(player.getUniqueId());
        openMenu(player);return true;
    }

    private boolean hasPortalSetupPermission(Player player){try{return player.isOp()||plugin.db().rank(player.getUniqueId())==Rank.CSRV;}catch(Exception ex){return false;}}
    private void openMenu(Player player){Inventory inv=Bukkit.createInventory(null,27,TITLE);inv.setItem(10,option(Material.COMPASS,"mode","§aОткрыть выбор режима"));inv.setItem(11,option(Material.ENDER_PEARL,"lobby","§bПерейти в лобби"));inv.setItem(13,option(Material.CHEST,"kit_start","§eВыдать стартовый набор"));inv.setItem(15,option(Material.RED_BED,"home","§dОткрыть меню домов"));inv.setItem(16,option(Material.BEACON,"chome","§6Открыть дом клана"));inv.setItem(20,option(Material.ENDER_EYE,"rtp","§cСлучайная телепортация"));inv.setItem(22,option(Material.BARRIER,"__remove","§cУбрать команду с портала"));player.openInventory(inv);}

    private PendingPortal inspectFrame(Block base,boolean alongX){if(!isFrame(base,1,2,alongX,true))return null;int width=1,height=2;while(width<MAX_FRAME_WIDTH&&isFrame(base,width+1,height,alongX,true))width++;while(height<MAX_FRAME_HEIGHT&&isFrame(base,width,height+1,alongX,true))height++;int px=alongX?1:0,pz=alongX?0:1;boolean lit=false;for(int x=0;x<width;x++)for(int y=0;y<height;y++){Material t=base.getRelative(px*x,y,pz*x).getType();if(t==Material.NETHER_PORTAL)lit=true;else if(t!=Material.AIR&&t!=Material.FIRE)return null;}return new PendingPortal(base.getWorld().getName(),base.getX(),base.getY(),base.getZ(),!lit,alongX,width,height);}
    private boolean isFrame(Block base,int width,int height,boolean alongX){return isFrame(base,width,height,alongX,true);}
    private boolean isFrame(Block base,int width,int height,boolean alongX,boolean allowLit){if(width<MIN_FRAME_WIDTH||width>MAX_FRAME_WIDTH||height<MIN_FRAME_HEIGHT||height>MAX_FRAME_HEIGHT)return false;int px=alongX?1:0,pz=alongX?0:1;for(int x=-1;x<=width;x++)for(int y=-1;y<=height;y++){int by=base.getY()+y;if(by<base.getWorld().getMinHeight()||by>=base.getWorld().getMaxHeight())return false;boolean edge=x==-1||x==width||y==-1||y==height;Block b=base.getRelative(px*x,y,pz*x);if(edge){if(b.getType()!=Material.OBSIDIAN)return false;}else if(b.getType()!=Material.AIR&&b.getType()!=Material.FIRE&&(!allowLit||b.getType()!=Material.NETHER_PORTAL))return false;}return true;}

    private boolean igniteFrame(PendingPortal frame){World world=Bukkit.getWorld(frame.world());if(world==null)return false;Block base=world.getBlockAt(frame.x(),frame.y(),frame.z());if(!frame.unlit()||!isFrame(base,frame.width(),frame.height(),frame.alongX()))return false;int px=frame.alongX()?1:0,pz=frame.alongX()?0:1;for(int x=0;x<frame.width();x++)for(int y=0;y<frame.height();y++){Material t=base.getRelative(px*x,y,pz*x).getType();if(t!=Material.AIR&&t!=Material.FIRE)return false;}for(int x=0;x<frame.width();x++)for(int y=0;y<frame.height();y++){Block b=base.getRelative(px*x,y,pz*x);b.setType(Material.NETHER_PORTAL,false);if(b.getBlockData() instanceof Orientable o){o.setAxis(frame.alongX()?org.bukkit.Axis.X:org.bukkit.Axis.Z);b.setBlockData(o,false);}}for(int x=0;x<frame.width();x++)for(int y=0;y<frame.height();y++){Block b=base.getRelative(px*x,y,pz*x);if(b.getType()!=Material.NETHER_PORTAL)return false;}return true;}
    private List<Block> framePortalBlocks(PendingPortal frame){World w=Bukkit.getWorld(frame.world());if(w==null)return List.of();Block base=w.getBlockAt(frame.x(),frame.y(),frame.z());int px=frame.alongX()?1:0,pz=frame.alongX()?0:1;List<Block> out=new ArrayList<>();for(int x=0;x<frame.width();x++)for(int y=0;y<frame.height();y++)out.add(base.getRelative(px*x,y,pz*x));return out;}

    private ItemStack option(Material material,String action,String name){ItemStack i=new ItemStack(material);ItemMeta m=i.getItemMeta();m.setDisplayName(name);m.setLore(List.of("§7Действие портала: §f/"+action,"§eВыдать одноразовое огниво настройки"));m.getPersistentDataContainer().set(commandKey,PersistentDataType.STRING,action);i.setItemMeta(m);return i;}
    private ItemStack ignitionItem(String action){ItemStack i=new ItemStack(Material.FLINT_AND_STEEL);ItemMeta m=i.getItemMeta();m.setDisplayName("§6Одноразовое огниво портала: §e/"+action);m.setLore(List.of("§7Выбранная команда для портала","§eЩёлкните по любой обсидиановой рамке"));m.getPersistentDataContainer().set(ignitionKey,PersistentDataType.STRING,action);i.setItemMeta(m);return i;}
    private void giveIgnition(Player p,String action){for(ItemStack extra:p.getInventory().addItem(ignitionItem(action)).values())p.getWorld().dropItemNaturally(p.getLocation(),extra);}
    private String ignitionCommand(ItemStack i){return i==null||i.getType()!=Material.FLINT_AND_STEEL||!i.hasItemMeta()?null:i.getItemMeta().getPersistentDataContainer().get(ignitionKey,PersistentDataType.STRING);}
    private PendingPortal findFrameAt(Block clicked){if(clicked.getType()!=Material.OBSIDIAN)return null;PendingPortal match=null;for(boolean alongX:new boolean[]{true,false})for(int along=-1;along<=MAX_FRAME_WIDTH;along++)for(int up=-MAX_FRAME_HEIGHT;up<=1;up++){int dx=alongX?along:0,dz=alongX?0:along;PendingPortal candidate=inspectFrame(clicked.getRelative(dx,up,dz),alongX);if(candidate==null)continue;if(!isFrameBlock(candidate,clicked))continue;if(match!=null&&!match.equals(candidate))return null;match=candidate;}return match;}
    private boolean isFrameBlock(PendingPortal f,Block clicked){World w=Bukkit.getWorld(f.world());if(w==null)return false;Block base=w.getBlockAt(f.x(),f.y(),f.z());int px=f.alongX()?1:0,pz=f.alongX()?0:1;for(int x=-1;x<=f.width();x++)for(int y=-1;y<=f.height();y++)if(base.getRelative(px*x,y,pz*x).equals(clicked))return true;return false;}
    private void consumeIgnition(Player p,EquipmentSlot hand,ItemStack held){if(held.getAmount()>1){held.setAmount(held.getAmount()-1);if(hand==EquipmentSlot.HAND)p.getInventory().setItemInMainHand(held);else p.getInventory().setItemInOffHand(held);}else if(hand==EquipmentSlot.HAND)p.getInventory().setItemInMainHand(null);else p.getInventory().setItemInOffHand(null);}

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false) public void igniteWithSelectedCommand(PlayerInteractEvent e){if(e.getAction()!=Action.RIGHT_CLICK_BLOCK||e.getClickedBlock()==null||e.getHand()!=EquipmentSlot.HAND)return;Player p=e.getPlayer();ItemStack held=e.getItem();String action=ignitionCommand(held);if(action==null)return;e.setCancelled(true);e.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);e.setUseItemInHand(org.bukkit.event.Event.Result.DENY);if((!OPTIONS.contains(action)&&!"__remove".equals(action))||!plugin.authed(p.getUniqueId())||!hasPortalSetupPermission(p))return;Block clicked=e.getClickedBlock();if("__remove".equals(action)){if(clicked.getType()!=Material.NETHER_PORTAL){p.sendMessage("§cОгнивом удаления нужно нажать по блоку портала.");return;}PendingPortal removeTarget=new PendingPortal(clicked.getWorld().getName(),clicked.getX(),clicked.getY(),clicked.getZ(),false,false,0,0);try{for(Block b:configuredPortalBlocks(removeTarget))plugin.db().removePortalCommand(removeTarget.world(),b.getX(),b.getY(),b.getZ());consumeIgnition(p,e.getHand(),held);p.sendMessage("§aНазначение снято с портала. Одноразовое огниво использовано.");}catch(Exception ex){p.sendMessage("§cНе удалось снять команду с портала.");plugin.getLogger().warning("Portal mapping removal failed: "+ex);}return;}PendingPortal f;if(clicked.getType()==Material.NETHER_PORTAL){f=new PendingPortal(clicked.getWorld().getName(),clicked.getX(),clicked.getY(),clicked.getZ(),false,false,0,0);}else if(clicked.getType()==Material.OBSIDIAN){f=findFrameAt(clicked);if(f==null){p.sendMessage("§cРядом с этим обсидианом не найдена подходящая рамка портала.");return;}}else{p.sendMessage("§cЩёлкните им по обсидиановой рамке или блоку существующего портала.");return;}List<Block> portalBlocks=f.unlit()?framePortalBlocks(f):configuredPortalBlocks(f);try{for(Block b:portalBlocks)plugin.db().setPortalCommand(f.world(),b.getX(),b.getY(),b.getZ(),action);}catch(Exception ex){for(Block b:portalBlocks)try{plugin.db().removePortalCommand(f.world(),b.getX(),b.getY(),b.getZ());}catch(Exception rollback){plugin.getLogger().warning("Portal mapping rollback failed: "+rollback);}p.sendMessage("§cНе удалось сохранить команду в БД. Портал не изменён, огниво сохранено.");plugin.getLogger().warning("Portal action save failed: "+ex);return;}if(f.unlit()&&!igniteFrame(f)){for(Block b:portalBlocks)try{plugin.db().removePortalCommand(f.world(),b.getX(),b.getY(),b.getZ());}catch(Exception rollback){plugin.getLogger().warning("Portal mapping rollback failed: "+rollback);}p.sendMessage("§cНе удалось зажечь рамку: проверьте пустой проём и целый обсидиан.");return;}consumeIgnition(p,e.getHand(),held);p.sendMessage(f.unlit()?"§aПортал зажжён: §e/"+action+"§a. Одноразовое огниво использовано.":"§aКоманда §e/"+action+"§a назначена порталу. Одноразовое огниво использовано.");}

    private List<Block> configuredPortalBlocks(PendingPortal f){World w=Bukkit.getWorld(f.world());if(w==null)return List.of();Block origin=w.getBlockAt(f.x(),f.y(),f.z());if(origin.getType()!=Material.NETHER_PORTAL&&origin.getType()!=Material.END_PORTAL)return List.of(origin);Material type=origin.getType();List<Block> found=new ArrayList<>();java.util.ArrayDeque<Block> q=new java.util.ArrayDeque<>();Set<String> seen=new HashSet<>();q.add(origin);while(!q.isEmpty()&&found.size()<4096){Block b=q.removeFirst();String key=b.getX()+":"+b.getY()+":"+b.getZ();if(!seen.add(key)||b.getType()!=type)continue;found.add(b);for(int[] d:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}}){Block n=b.getRelative(d[0],d[1],d[2]);if(!seen.contains(n.getX()+":"+n.getY()+":"+n.getZ())&&n.getType()==type)q.add(n);}}return found.isEmpty()?List.of(origin):found;}
    private Block findPortal(Location l){if(l.getWorld()==null)return null;Block center=l.getBlock(),best=null;double dist=Double.MAX_VALUE;for(int dx=-8;dx<=8;dx++)for(int dy=-8;dy<=8;dy++)for(int dz=-8;dz<=8;dz++){Block b=center.getRelative(dx,dy,dz);if(b.getType()!=Material.NETHER_PORTAL&&b.getType()!=Material.END_PORTAL)continue;double d=b.getLocation().add(.5,.5,.5).distanceSquared(l);if(d<dist){best=b;dist=d;}}return best;}

    @EventHandler(priority=EventPriority.HIGHEST) public void click(InventoryClickEvent e){if(!TITLE.equals(e.getView().getTitle()))return;e.setCancelled(true);if(!(e.getWhoClicked() instanceof Player p)||!plugin.authed(p.getUniqueId())||!hasPortalSetupPermission(p)){e.getWhoClicked().closeInventory();return;}if(e.getRawSlot()<0||e.getRawSlot()>=e.getView().getTopInventory().getSize())return;ItemStack item=e.getCurrentItem();if(item==null||!item.hasItemMeta())return;String action=item.getItemMeta().getPersistentDataContainer().get(commandKey,PersistentDataType.STRING);if(action==null)return;p.closeInventory();if(!OPTIONS.contains(action)&&!"__remove".equals(action))return;giveIgnition(p,action);p.sendMessage("§aПолучено одноразовое огниво для §e"+("__remove".equals(action)?"снятия команды":"/"+action)+"§a. Щёлкните им по обсидиану любой подходящей рамки или по блоку портала.");}

    private boolean isPortal(Location l){return l.getWorld()!=null&&(l.getBlock().getType()==Material.NETHER_PORTAL||l.getBlock().getType()==Material.END_PORTAL);}
    private PortalAction findPortalAction(Location l)throws Exception{Block b=findPortal(l);if(b==null)return null;PortalTrigger trigger=new PortalTrigger(b.getWorld().getName(),b.getX(),b.getY(),b.getZ());String action=plugin.db().portalCommand(trigger.world(),trigger.x(),trigger.y(),trigger.z());if(action==null||!OPTIONS.contains(action))for(Block part:configuredPortalBlocks(new PendingPortal(trigger.world(),trigger.x(),trigger.y(),trigger.z(),false,false,0,0))){action=plugin.db().portalCommand(trigger.world(),part.getX(),part.getY(),part.getZ());if(action!=null&&OPTIONS.contains(action))break;}return action==null||!OPTIONS.contains(action)?null:new PortalAction(trigger,action);}
    private void completePortalAction(Player p,PortalAction a){if(!p.isOnline()||!plugin.authed(p.getUniqueId()))return;Location exit=safePortalExit(p,a.trigger());if(exit==null){p.sendMessage("§cНе найдено безопасное место рядом с порталом.");return;}boolean invulnerable=p.isInvulnerable();p.setInvulnerable(true);p.setPortalCooldown(1200);if(!p.teleport(exit,PlayerTeleportEvent.TeleportCause.PLUGIN)){p.setInvulnerable(invulnerable);return;}Bukkit.getScheduler().runTaskLater(plugin,()->{if(!p.isOnline()||!plugin.authed(p.getUniqueId())){if(p.isOnline())p.setInvulnerable(invulnerable);return;}if("mode".equals(a.command())&&!plugin.gameModes().isLobby(p.getWorld())){plugin.gameModes().goHub(p,()->Bukkit.getScheduler().runTaskLater(plugin,()->{if(p.isOnline()){p.setInvulnerable(true);new com.example.pryanik.GameModeCommand(plugin).openModes(p);}},1L));return;}plugin.runPortalPlayerCommand(p,a.command());if(!"mode".equals(a.command()))p.setInvulnerable(invulnerable);},1L);}
    private Location safePortalExit(Player p,PortalTrigger trigger){World w=Bukkit.getWorld(trigger.world());if(w==null)return null;Block o=w.getBlockAt(trigger.x(),trigger.y(),trigger.z());int[][] offsets={{1,0,0},{-1,0,0},{0,0,1},{0,0,-1},{1,1,0},{-1,1,0},{0,1,1},{0,1,-1},{1,0,1},{-1,0,1},{1,0,-1},{-1,0,-1},{0,1,0},{0,2,0}};for(int[] d:offsets){Block feet=o.getRelative(d[0],d[1],d[2]);if(feet.getType()==Material.NETHER_PORTAL||feet.getType()==Material.END_PORTAL||feet.getType().isSolid())continue;Block head=feet.getRelative(0,1,0);if(head.getType()==Material.NETHER_PORTAL||head.getType()==Material.END_PORTAL||head.getType().isSolid())continue;Location c=feet.getLocation().add(.5,0,.5);c.setYaw(p.getLocation().getYaw());c.setPitch(p.getLocation().getPitch());return c;}return null;}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void touchPortal(PlayerMoveEvent e){Player p=e.getPlayer();Location from=e.getFrom(),to=e.getTo();if(to==null)return;if(!isPortal(to)){portalTriggers.remove(p.getUniqueId());return;}if(isPortal(from)||!plugin.authed(p.getUniqueId())||portalTriggers.containsKey(p.getUniqueId()))return;try{PortalAction a=findPortalAction(to);if(a==null)return;portalTriggers.put(p.getUniqueId(),a.trigger());p.setPortalCooldown(1200);Bukkit.getScheduler().runTask(plugin,()->completePortalAction(p,a));}catch(Exception ex){plugin.getLogger().warning("Portal command lookup failed: "+ex.getMessage());}}
    @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=true) public void runPortalAction(PlayerTeleportEvent e){if(e.getCause()!=PlayerTeleportEvent.TeleportCause.NETHER_PORTAL&&e.getCause()!=PlayerTeleportEvent.TeleportCause.END_PORTAL)return;Player p=e.getPlayer();PortalTrigger trigger=portalTriggers.remove(p.getUniqueId());PortalAction a=null;try{if(trigger!=null){String action=plugin.db().portalCommand(trigger.world(),trigger.x(),trigger.y(),trigger.z());if(action!=null&&OPTIONS.contains(action))a=new PortalAction(trigger,action);}if(a==null)a=findPortalAction(e.getFrom());}catch(Exception ex){plugin.getLogger().warning("Portal command lookup failed: "+ex.getMessage());return;}if(a==null)return;e.setCancelled(true);p.setPortalCooldown(1200);PortalAction selected=a;Bukkit.getScheduler().runTask(plugin,()->completePortalAction(p,selected));}
    @EventHandler public void clearPortalTrigger(PlayerQuitEvent e){portalTriggers.remove(e.getPlayer().getUniqueId());}
}
