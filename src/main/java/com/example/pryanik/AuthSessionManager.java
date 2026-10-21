package com.example.pryanik;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** IP-bound, password-free remember sessions. Note: offline-mode names/IPs are spoofable. */
public final class AuthSessionManager implements Listener {
    private final PryanikPlugin plugin;
    private final File sessionFile;
    private final YamlConfiguration sessions;
    private static final long DEFAULT_DURATION_MS=0L;
    private static final long[] ALLOWED={0L,900_000L,1_800_000L,3_600_000L,18_000_000L,36_000_000L,86_400_000L};
    public AuthSessionManager(PryanikPlugin plugin){this.plugin=plugin;this.sessionFile=new File(plugin.getDataFolder(),"auth-sessions.yml");this.sessions=YamlConfiguration.loadConfiguration(sessionFile);}
    private String accountKey(String name){return "accounts."+name.toLowerCase(Locale.ROOT);}
    private String field(String name,String field){return accountKey(name)+"."+field;}
    public long duration(UUID uuid){String name;Player online=Bukkit.getPlayer(uuid);if(online!=null)name=online.getName();else try{name=plugin.db().nickname(uuid);}catch(Exception e){return DEFAULT_DURATION_MS;}long value=sessions.getLong(field(name,"duration"),DEFAULT_DURATION_MS);return allowed(value)?value:DEFAULT_DURATION_MS;}
    public void setDuration(Player player,long value){if(!allowed(value))throw new IllegalArgumentException("unsupported remember duration");String key=accountKey(player.getName());sessions.set(key,null);sessions.set(field(player.getName(),"duration"),value);if(value==0L){saveFile();return;}long expires=System.currentTimeMillis()+value;String ip=clientIp(player);sessions.set(field(player.getName(),"expires"),expires);sessions.set(field(player.getName(),"ip"),ip);sessions.set(field(player.getName(),"session_hash"),sessionHash(player.getName(),expires,ip));saveFile();}
    private String clientIp(Player player){return player.getAddress()==null||player.getAddress().getAddress()==null?"":player.getAddress().getAddress().getHostAddress();}
    private String sessionHash(String name,long expires,String ip){try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest((name.toLowerCase(Locale.ROOT)+":"+expires+":"+ip+":"+plugin.getConfig().getString("database.password","")).getBytes(StandardCharsets.UTF_8));return Base64.getEncoder().encodeToString(bytes);}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private boolean allowed(long value){for(long option:ALLOWED)if(option==value)return true;return false;}
    public void save(){saveFile();}
    private void saveFile(){try{sessions.save(sessionFile);}catch(IOException e){plugin.getLogger().severe("Cannot save remember sessions: "+e.getMessage());}}
    @EventHandler public void join(PlayerJoinEvent event){Player player=event.getPlayer();String name=player.getName();if(duration(player.getUniqueId())==0L)return;String key=accountKey(name);long expires=sessions.getLong(field(name,"expires"),0L);String storedIp=sessions.getString(field(name,"ip"),"");String currentIp=clientIp(player);String expected=sessions.getString(field(name,"session_hash"),"");if(expires<=System.currentTimeMillis()||storedIp.isEmpty()||!storedIp.equals(currentIp)||expected.isEmpty()||!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),sessionHash(name,expires,storedIp).getBytes(StandardCharsets.UTF_8))){sessions.set(key,null);saveFile();return;}Bukkit.getScheduler().runTask(plugin,()->{if(!player.isOnline())return;try{if(!plugin.db().nicknameExists(name)){sessions.set(key,null);saveFile();return;}plugin.authenticate(player);player.sendMessage("§7Вход выполнен автоматически по никнейму и IP.");}catch(Exception e){plugin.getLogger().warning("Cannot restore remembered auth session for "+player.getName()+": "+e.getMessage());}});}
    @EventHandler public void quit(PlayerQuitEvent event){Player player=event.getPlayer();if(!plugin.authed(player.getUniqueId()))return;String name=player.getName();if(sessions.getLong(field(name,"expires"),0L)<=System.currentTimeMillis()){sessions.set(accountKey(name),null);saveFile();}}
}
