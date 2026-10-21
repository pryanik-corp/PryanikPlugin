package com.example.pryanik;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

/** Keeps player items, armor and XP separated by world/grief. */
public final class GameStateManager implements Listener {
    private final PryanikPlugin plugin;
    private final Map<UUID,String> activeScope=new HashMap<>();
    private final Map<UUID,String> pendingScope=new HashMap<>();
    public GameStateManager(PryanikPlugin plugin){this.plugin=plugin;}
    private String scope(org.bukkit.World world){
        if(plugin.gameModes().isLobby(world))return "lobby";
        String instance=plugin.instanceOf(world.getName());
        return instance==null?"world:"+world.getName():"grief:"+instance;
    }
    private boolean survivalState(Player p){return p.getGameMode()==GameMode.SURVIVAL||p.getGameMode()==GameMode.ADVENTURE;}
    private void save(Player p,String key){if(key==null)return;ItemStack[] contents=p.getInventory().getContents().clone();for(int i=0;i<contents.length;i++)if(GameModeCommand.isCompass(contents[i],new NamespacedKey(plugin,"mode_compass")))contents[i]=null;ItemStack off=p.getInventory().getItemInOffHand();if(GameModeCommand.isCompass(off,new NamespacedKey(plugin,"mode_compass")))off=null;try{plugin.db().saveGameState(p.getUniqueId(),key,encode(contents),encode(p.getInventory().getArmorContents()),encode(new ItemStack[]{off}),p.getLevel(),p.getExp(),p.getTotalExperience());}catch(Exception ex){plugin.getLogger().warning("Could not save inventory for "+p.getName()+" in "+key+": "+ex.getMessage());}}
    private void load(Player p,String key){ItemStack[] cursor={p.getItemOnCursor()};p.setItemOnCursor(null);p.getInventory().clear();p.getInventory().setArmorContents(new ItemStack[4]);p.getInventory().setItemInOffHand(null);p.setLevel(0);p.setExp(0F);p.setTotalExperience(0);try{String state=plugin.db().gameState(p.getUniqueId(),key);if(state!=null){String[] f=state.split("\\n",-1);if(f.length==6){p.getInventory().setContents(decode(f[0]));p.getInventory().setArmorContents(decode(f[1]));ItemStack[] off=decode(f[2]);p.getInventory().setItemInOffHand(off.length==0?null:off[0]);p.setLevel(Integer.parseInt(f[3]));p.setExp(Float.parseFloat(f[4]));p.setTotalExperience(Integer.parseInt(f[5]));}}}catch(Exception ex){plugin.getLogger().warning("Could not load inventory for "+p.getName()+" in "+key+": "+ex.getMessage());}finally{if(!"lobby".equals(key)){for(int i=0;i<p.getInventory().getSize();i++)if(GameModeCommand.isCompass(p.getInventory().getItem(i),new NamespacedKey(plugin,"mode_compass")))p.getInventory().setItem(i,null);if(GameModeCommand.isCompass(p.getInventory().getItemInOffHand(),new NamespacedKey(plugin,"mode_compass")))p.getInventory().setItemInOffHand(null);}if(cursor[0]!=null&&!cursor[0].getType().isAir())p.getInventory().addItem(cursor[0]);if("lobby".equals(key))org.bukkit.Bukkit.getScheduler().runTask(plugin,()->{if(p.isOnline()&&plugin.gameModes().isLobby(p.getWorld()))plugin.gameModes().giveLobbyCompass(p);});}}
    private void switchScope(Player p,String next){String old=activeScope.get(p.getUniqueId());if(next==null||next.equals(old))return;save(p,old);if(old!=null)load(p,next);activeScope.put(p.getUniqueId(),next);}
    public void beginInstance(Player p,String id){pendingScope.put(p.getUniqueId(),"grief:"+id);} public void cancelTransition(Player p){pendingScope.remove(p.getUniqueId());} public void initialize(Player p,String key){if(!activeScope.containsKey(p.getUniqueId())){load(p,key);activeScope.put(p.getUniqueId(),key);}}
    @EventHandler(priority=EventPriority.MONITOR) public void join(PlayerJoinEvent e){Player p=e.getPlayer();org.bukkit.Bukkit.getScheduler().runTask(plugin,()->{if(p.isOnline()){String next=scope(p.getWorld());load(p,next);activeScope.put(p.getUniqueId(),next);}});}
    @EventHandler(priority=EventPriority.MONITOR) public void changedWorld(PlayerChangedWorldEvent e){Player p=e.getPlayer();pendingScope.remove(p.getUniqueId());switchScope(p,scope(p.getWorld()));}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void teleport(PlayerTeleportEvent e){if(e.getTo()==null||e.getTo().getWorld()==null)return;Player p=e.getPlayer();String old=activeScope.get(p.getUniqueId()),next=scope(e.getTo().getWorld());if(old!=null&&!old.equals(next)){save(p,old);pendingScope.put(p.getUniqueId(),next);}}
    @EventHandler(priority=EventPriority.MONITOR) public void respawn(PlayerRespawnEvent e){pendingScope.put(e.getPlayer().getUniqueId(),scope(e.getRespawnLocation().getWorld()));org.bukkit.Bukkit.getScheduler().runTask(plugin,()->{Player p=e.getPlayer();if(p.isOnline()){String next=scope(p.getWorld());switchScope(p,next);}});}
    @EventHandler(priority=EventPriority.MONITOR) public void quit(PlayerQuitEvent e){Player p=e.getPlayer();save(p,activeScope.remove(p.getUniqueId()));pendingScope.remove(p.getUniqueId());}
    private String encode(ItemStack[] items)throws IOException{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(bytes)){out.writeInt(items.length);for(ItemStack item:items){if(item==null||item.getType().isAir()){out.writeInt(-1);continue;}byte[] b=item.serializeAsBytes();out.writeInt(b.length);out.write(b);}}return Base64.getEncoder().encodeToString(bytes.toByteArray());}
    private ItemStack[] decode(String value)throws IOException{byte[] bytes=Base64.getDecoder().decode(value);try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){int n=in.readInt();if(n<0||n>256)throw new IOException("bad item count");ItemStack[] result=new ItemStack[n];for(int i=0;i<n;i++){int length=in.readInt();if(length<0)continue;if(length>1_000_000)throw new IOException("bad item size");byte[] item=new byte[length];in.readFully(item);result[i]=ItemStack.deserializeBytes(item);}return result;}}
}
