package com.example.pryanik;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;

/** Private LAN-only HTTP bridge for tightly allowlisted website world controls. */
public final class AdminHttpApi {
    private final PryanikPlugin plugin;
    private HttpServer server;
    private volatile int[] lastLoadCounts = new int[]{0,0,0,0};

    public AdminHttpApi(PryanikPlugin plugin) { this.plugin = plugin; }

    public void start() {
        if (!plugin.getConfig().getBoolean("admin-api.enabled", false)) { plugin.getLogger().info("Private admin HTTP API is disabled by config."); return; }
        String token = plugin.getConfig().getString("admin-api.token", "").trim();
        if (token.length() < 32 || token.toLowerCase().contains("replace")) {
            plugin.getLogger().warning("Admin HTTP API is disabled: set admin-api.token to a random secret of at least 32 characters.");
            return;
        }
        try {
            String bind = plugin.getConfig().getString("admin-api.bind", "127.0.0.1");
            int port = Math.max(1024, Math.min(65535, plugin.getConfig().getInt("admin-api.port", 8765)));
            server = HttpServer.create(new InetSocketAddress(bind, port), 16);
            server.createContext("/admin/settings", exchange -> handle(exchange, token));
            server.setExecutor(Executors.newFixedThreadPool(2, task -> { Thread t = new Thread(task, "Pryanik-AdminHttp"); t.setDaemon(true); return t; }));
            server.start();
            plugin.getLogger().info("Private admin HTTP API listening on " + bind + ":" + port + "/admin/settings");
        } catch (IOException ex) { plugin.getLogger().severe("Could not start private admin HTTP API: " + ex.getMessage()); }
    }

    public void stop() { if (server != null) { server.stop(1); server = null; } }

    private void handle(HttpExchange exchange, String token) throws IOException {
        try {
            String client = exchange.getRemoteAddress().getAddress().getHostAddress();
            java.util.List<String> clients = plugin.getConfig().getStringList("admin-api.allowed-clients");
            if (clients.isEmpty() || !clients.contains(client)) { respond(exchange, 403, "{\"error\":\"client_forbidden\"}"); return; }
            if (!constantEquals(token, exchange.getRequestHeaders().getFirst("X-Pryanik-Token"))) { respond(exchange, 403, "{\"error\":\"forbidden\"}"); return; }
            if ("GET".equals(exchange.getRequestMethod())) {
                final String[] body = new String[1]; runSync(() -> body[0] = snapshot());
                respond(exchange, 200, body[0]); return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) { exchange.getResponseHeaders().set("Allow", "GET, POST"); respond(exchange, 405, "{\"error\":\"method\"}"); return; }
            byte[] requestBody = exchange.getRequestBody().readNBytes(4097);
            if (requestBody.length > 4096) { respond(exchange, 413, "{\"error\":\"too_large\"}"); return; }
            String body = new String(requestBody, StandardCharsets.UTF_8);
            Map<String,String> fields = parseForm(body);
            final String[] error = new String[1];
            runSync(() -> { try { apply(fields); } catch (IllegalArgumentException ex) { error[0] = ex.getMessage(); } }, "load-griefs".equals(fields.get("setting")) ? 120_000L : 5_000L);
            if (error[0] != null) { int status="no_registered_griefs".equals(error[0])?404:400;respond(exchange,status,"{\"error\":\"" + json(error[0]) + "\"}"); return; }
            String loadResult=fields.getOrDefault("_load_result","");if(!loadResult.isEmpty()){String[] counts=loadResult.split(",");respond(exchange,200,"{\"ok\":true,\"sets\":"+counts[0]+",\"loaded\":"+counts[1]+",\"alreadyLoaded\":"+counts[2]+",\"missing\":"+counts[3]+"}");}else respond(exchange, 200, "{\"ok\":true}");
        } catch (Exception ex) {
            plugin.getLogger().warning("Admin API request failed: " + ex.getMessage());
            respond(exchange, 500, "{\"error\":\"internal\"}");
        } finally { exchange.close(); }
    }

    private void apply(Map<String,String> f) {
        String scope = f.getOrDefault("scope", "");
        String name = f.getOrDefault("name", "");
        String setting = f.getOrDefault("setting", "");
        String value = f.getOrDefault("value", "");
        if (scope.equals("server")) { if(!setting.equals("load-griefs")&&!setting.equals("save-worlds"))throw new IllegalArgumentException("unsupported_setting");if(setting.equals("save-worlds")){for(World world:Bukkit.getWorlds())world.save();return;}int[] result=loadRegisteredGriefWorlds();lastLoadCounts=result;f.put("_load_result",result[0]+","+result[1]+","+result[2]+","+result[3]);return; }
        if (scope.equals("world")) {
            World w = Bukkit.getWorld(name);
            if (w == null || name.length() > 64 || name.contains("..")) throw new IllegalArgumentException("unknown_world");
            String path = "admin-worlds." + name + ".";
            switch (setting) {
                case "difficulty" -> { Difficulty d; try { d = Difficulty.valueOf(value.toUpperCase()); } catch (IllegalArgumentException ex) { throw new IllegalArgumentException("invalid_difficulty"); } w.setDifficulty(d); plugin.getConfig().set(path + "difficulty", d.name()); }
                case "game-mode" -> { org.bukkit.GameMode mode; try { mode = org.bukkit.GameMode.valueOf(value.toUpperCase()); } catch (IllegalArgumentException ex) { throw new IllegalArgumentException("invalid_game_mode"); } if (plugin.gameModes().isSpawnWorld(w) && mode != org.bukkit.GameMode.ADVENTURE) throw new IllegalArgumentException("protected_world_requires_adventure"); plugin.getConfig().set(path + "game-mode", mode.name()); for (org.bukkit.entity.Player player : w.getPlayers()) player.setGameMode(mode); }
                case "pvp" -> { boolean b = bool(value); String instance = plugin.gameModes().instanceOf(name); if (instance != null) { try { plugin.db().setPvpEnabled(instance, b); } catch (Exception ex) { throw new IllegalArgumentException("database_error"); } for (World linked : Bukkit.getWorlds()) if (instance.equals(plugin.gameModes().instanceOf(linked.getName())) && !plugin.gameModes().isSpawnWorld(linked)) linked.setPVP(b); } else w.setPVP(b); }
                case "block-place", "block-break", "interact", "mob-spawning", "monster-spawning", "passive-spawning" -> { boolean enabled=bool(value); plugin.getConfig().set(path + setting, enabled); if(plugin.gameModes().isLobby(w))plugin.gameModes().configureLobbyWorld(w); if(setting.equals("mob-spawning")) w.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING,enabled); if(setting.equals("monster-spawning")) w.setGameRule(org.bukkit.GameRule.SPAWN_MONSTERS,enabled); plugin.gameModes().applyConfiguredMobSettings(w); }
                case "monster-cap", "passive-cap" -> { int limit=number(value,-1,1000); plugin.getConfig().set(path+setting,limit); plugin.gameModes().applyConfiguredMobSettings(w); }
                case "allowed-commands" -> { String v=value.toLowerCase(); if(v.length()>512 || !(v.isEmpty() || v.equals("all") || v.equals("none") || v.matches("[a-z0-9_, -]+"))) throw new IllegalArgumentException("invalid_commands"); plugin.getConfig().set(path+setting,v.replace(" ","")); }
                case "spawn" -> { String[] parts=value.split(",",-1); if(parts.length!=5) throw new IllegalArgumentException("invalid_spawn"); try { double x=Double.parseDouble(parts[0]),y=Double.parseDouble(parts[1]),z=Double.parseDouble(parts[2]); float yaw=Float.parseFloat(parts[3]),pitch=Float.parseFloat(parts[4]); if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||!Float.isFinite(yaw)||!Float.isFinite(pitch)||y<w.getMinHeight()||y>w.getMaxHeight()) throw new IllegalArgumentException("invalid_spawn"); w.setSpawnLocation(new org.bukkit.Location(w,x,y,z,yaw,pitch)); plugin.getConfig().set(path+"spawn.x",x);plugin.getConfig().set(path+"spawn.y",y);plugin.getConfig().set(path+"spawn.z",z);plugin.getConfig().set(path+"spawn.yaw",yaw);plugin.getConfig().set(path+"spawn.pitch",pitch); if(name.equalsIgnoreCase("lobby")){plugin.getConfig().set("admin-worlds.lobby-spawn.x",x);plugin.getConfig().set("admin-worlds.lobby-spawn.y",y);plugin.getConfig().set("admin-worlds.lobby-spawn.z",z);} } catch(NumberFormatException ex){throw new IllegalArgumentException("invalid_spawn");} catch(IllegalArgumentException ex){if("invalid_spawn".equals(ex.getMessage()))throw ex;throw new IllegalArgumentException("invalid_spawn");} }
                default -> throw new IllegalArgumentException("unsupported_setting");
            }
        } else if (scope.equals("mode")) {
            if (!java.util.Set.of("survival","hardcore","anarchy").contains(name)) throw new IllegalArgumentException("invalid_mode");
            String path="game-modes."+name+".";
            switch(setting){ case "enabled" -> plugin.getConfig().set(path+"enabled",bool(value)); case "instances" -> plugin.getConfig().set(path+"instances",number(value,0,1000)); case "capacity" -> plugin.getConfig().set(path+"capacity",number(value,1,1000)); default -> throw new IllegalArgumentException("unsupported_setting"); }
        } else throw new IllegalArgumentException("invalid_scope");
        plugin.saveConfig();
    }

    private int[] loadRegisteredGriefWorlds(){int loaded=0,already=0,missing=0;try{Map<String,String[]> sets=new LinkedHashMap<>();for(String[] set:plugin.db().managedWorldSets())sets.put(set[0],set);for(String[] row:plugin.db().gameInstances()){String base=row[2];sets.putIfAbsent(base,new String[]{base,base,base+"_nether",base+"_the_end",base+"_spawn"});}if(sets.isEmpty())throw new IllegalArgumentException("no_registered_griefs");int setCount=sets.size();for(String[] set:sets.values()){String base=set[0];String[] names={set[1],set[2],set[3],set.length>4&&!set[4].isBlank()?set[4]:base+"_spawn"};World.Environment[] envs={World.Environment.NORMAL,World.Environment.NETHER,World.Environment.THE_END,World.Environment.NORMAL};for(int i=0;i<names.length;i++){if(names[i]==null||names[i].isBlank())continue;if(Bukkit.getWorld(names[i])!=null){already++;continue;}java.io.File folder=new java.io.File(Bukkit.getWorldContainer(),names[i]);if(!new java.io.File(folder,"level.dat").isFile()){missing++;continue;}try{World world=Bukkit.createWorld(new org.bukkit.WorldCreator(names[i]).environment(envs[i]));if(world!=null){loaded++;plugin.getLogger().info("Loaded grief world by website request: "+names[i]);}else missing++;}catch(Exception worldError){missing++;plugin.getLogger().severe("Could not load grief world "+names[i]+": "+worldError);}}}plugin.gameModes().refreshIndex();return new int[]{setCount,loaded,already,missing};}catch(IllegalArgumentException ex){throw ex;}catch(Exception ex){plugin.getLogger().severe("Website load-all grief worlds failed: "+ex);throw new IllegalArgumentException("load_failed");}}

    private org.bukkit.GameMode effectiveGameMode(World world) { String value=plugin.getConfig().getString("admin-worlds."+world.getName()+".game-mode"); if(value!=null)try{return org.bukkit.GameMode.valueOf(value.toUpperCase());}catch(IllegalArgumentException ignored){} if(world.getName().equalsIgnoreCase("lobby")||plugin.gameModes().isSpawnWorld(world))return org.bukkit.GameMode.ADVENTURE; return org.bukkit.GameMode.SURVIVAL; }

    private String snapshot() {
        StringBuilder out=new StringBuilder("{\"worlds\":["); boolean first=true;
        for(World w:Bukkit.getWorlds()) { if(!first)out.append(',');first=false;String path="admin-worlds."+w.getName()+".";out.append("{\"name\":\"").append(json(w.getName())).append("\",\"path\":\"").append(json(w.getWorldFolder().getAbsolutePath())).append("\",\"difficulty\":\"").append(w.getDifficulty()).append("\",\"gameMode\":\"").append(effectiveGameMode(w).name()).append("\",\"pvp\":").append(w.getPVP()).append(",\"blockPlace\":").append(plugin.getConfig().getBoolean(path+"block-place",plugin.getConfig().getBoolean("admin-worlds.defaults.block-place",true))).append(",\"blockBreak\":").append(plugin.getConfig().getBoolean(path+"block-break",plugin.getConfig().getBoolean("admin-worlds.defaults.block-break",true))).append(",\"interact\":").append(plugin.getConfig().getBoolean(path+"interact",plugin.getConfig().getBoolean("admin-worlds.defaults.interact",true))).append(",\"mobSpawning\":").append(plugin.getConfig().getBoolean(path+"mob-spawning",!w.getName().equalsIgnoreCase("lobby"))).append(",\"monsterSpawning\":").append(plugin.getConfig().getBoolean(path+"monster-spawning",!w.getName().equalsIgnoreCase("lobby"))).append(",\"passiveSpawning\":").append(plugin.getConfig().getBoolean(path+"passive-spawning",!w.getName().equalsIgnoreCase("lobby"))).append(",\"monsterCap\":").append(plugin.getConfig().getInt(path+"monster-cap",-1)).append(",\"passiveCap\":").append(plugin.getConfig().getInt(path+"passive-cap",-1)).append(",\"commands\":\"").append(json(plugin.getConfig().getString(path+"allowed-commands",""))).append("\",\"spawn\":\"").append(w.getSpawnLocation().getX()).append(',').append(w.getSpawnLocation().getY()).append(',').append(w.getSpawnLocation().getZ()).append(',').append(w.getSpawnLocation().getYaw()).append(',').append(w.getSpawnLocation().getPitch()).append("\"}"); }
        out.append("],\"modes\":{");String[] modes={"survival","hardcore","anarchy"};for(int i=0;i<modes.length;i++){String m=modes[i],p="game-modes."+m+".";if(i>0)out.append(',');out.append('\"').append(m).append("\":{\"enabled\":").append(plugin.getConfig().getBoolean(p+"enabled",true)).append(",\"instances\":").append(plugin.getConfig().getInt(p+"instances",switch(m){case "survival"->20;case "hardcore"->5;default->15;})).append(",\"capacity\":").append(plugin.getConfig().getInt(p+"capacity",m.equals("survival")?20:15)).append('}');}return out.append("}}").toString();
    }

    private void runSync(Runnable runnable) throws InterruptedException {runSync(runnable,5_000L);}
    private void runSync(Runnable runnable,long timeoutMs) throws InterruptedException { if(Bukkit.isPrimaryThread()){runnable.run();return;}CountDownLatch done=new CountDownLatch(1);Bukkit.getScheduler().runTask(plugin,()->{try{runnable.run();}finally{done.countDown();}});if(!done.await(timeoutMs,java.util.concurrent.TimeUnit.MILLISECONDS))throw new IllegalStateException("main_thread_timeout"); }
    private static boolean bool(String v){if(v.equalsIgnoreCase("true"))return true;if(v.equalsIgnoreCase("false"))return false;throw new IllegalArgumentException("invalid_boolean");}
    private static int number(String v,int min,int max){try{int n=Integer.parseInt(v);if(n<min||n>max)throw new IllegalArgumentException("out_of_range");return n;}catch(NumberFormatException ex){throw new IllegalArgumentException("invalid_number");}}
    private static Map<String,String> parseForm(String body){Map<String,String> out=new LinkedHashMap<>();for(String part:body.split("&")){int i=part.indexOf('=');if(i<0)continue;out.put(URLDecoder.decode(part.substring(0,i),StandardCharsets.UTF_8),URLDecoder.decode(part.substring(i+1),StandardCharsets.UTF_8));}return out;}
    private static boolean constantEquals(String expected,String given){if(given==null)return false;return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),given.getBytes(StandardCharsets.UTF_8));}
    private static String json(String s){return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r");}
    private static void respond(HttpExchange e,int status,String body)throws IOException{byte[] bytes=body.getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");e.getResponseHeaders().set("Cache-Control","no-store");e.sendResponseHeaders(status,bytes.length);e.getResponseBody().write(bytes);}
}
