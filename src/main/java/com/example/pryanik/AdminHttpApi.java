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
            runSync(() -> { try { apply(fields); } catch (IllegalArgumentException ex) { error[0] = ex.getMessage(); } });
            if (error[0] != null) { respond(exchange, 400, "{\"error\":\"" + json(error[0]) + "\"}"); return; }
            respond(exchange, 200, "{\"ok\":true}");
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
        if (scope.equals("world")) {
            World w = Bukkit.getWorld(name);
            if (w == null || name.length() > 64 || name.contains("..")) throw new IllegalArgumentException("unknown_world");
            String path = "admin-worlds." + name + ".";
            switch (setting) {
                case "difficulty" -> { Difficulty d; try { d = Difficulty.valueOf(value.toUpperCase()); } catch (IllegalArgumentException ex) { throw new IllegalArgumentException("invalid_difficulty"); } w.setDifficulty(d); plugin.getConfig().set(path + "difficulty", d.name()); }
                case "pvp" -> { boolean b = bool(value); String instance = plugin.gameModes().instanceOf(name); if (instance != null) { try { plugin.db().setPvpEnabled(instance, b); } catch (Exception ex) { throw new IllegalArgumentException("database_error"); } for (World linked : Bukkit.getWorlds()) if (instance.equals(plugin.gameModes().instanceOf(linked.getName())) && !plugin.gameModes().isSpawnWorld(linked)) linked.setPVP(b); } else w.setPVP(b); }
                case "block-place", "block-break", "interact" -> plugin.getConfig().set(path + setting, bool(value));
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

    private String snapshot() {
        StringBuilder out=new StringBuilder("{\"worlds\":["); boolean first=true;
        for(World w:Bukkit.getWorlds()) { if(!first)out.append(',');first=false;String path="admin-worlds."+w.getName()+".";out.append("{\"name\":\"").append(json(w.getName())).append("\",\"difficulty\":\"").append(w.getDifficulty()).append("\",\"pvp\":").append(w.getPVP()).append(",\"blockPlace\":").append(plugin.getConfig().getBoolean(path+"block-place",plugin.getConfig().getBoolean("admin-worlds.defaults.block-place",true))).append(",\"blockBreak\":").append(plugin.getConfig().getBoolean(path+"block-break",plugin.getConfig().getBoolean("admin-worlds.defaults.block-break",true))).append(",\"interact\":").append(plugin.getConfig().getBoolean(path+"interact",plugin.getConfig().getBoolean("admin-worlds.defaults.interact",true))).append(",\"commands\":\"").append(json(plugin.getConfig().getString(path+"allowed-commands",""))).append("\",\"spawn\":\"").append(w.getSpawnLocation().getX()).append(',').append(w.getSpawnLocation().getY()).append(',').append(w.getSpawnLocation().getZ()).append(',').append(w.getSpawnLocation().getYaw()).append(',').append(w.getSpawnLocation().getPitch()).append("\"}"); }
        out.append("],\"modes\":{");String[] modes={"survival","hardcore","anarchy"};for(int i=0;i<modes.length;i++){String m=modes[i],p="game-modes."+m+".";if(i>0)out.append(',');out.append('\"').append(m).append("\":{\"enabled\":").append(plugin.getConfig().getBoolean(p+"enabled",true)).append(",\"instances\":").append(plugin.getConfig().getInt(p+"instances",switch(m){case "survival"->20;case "hardcore"->5;default->15;})).append(",\"capacity\":").append(plugin.getConfig().getInt(p+"capacity",m.equals("survival")?20:15)).append('}');}return out.append("}}").toString();
    }

    private void runSync(Runnable runnable) throws InterruptedException { if(Bukkit.isPrimaryThread()){runnable.run();return;}CountDownLatch done=new CountDownLatch(1);Bukkit.getScheduler().runTask(plugin,()->{try{runnable.run();}finally{done.countDown();}});if(!done.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("main_thread_timeout"); }
    private static boolean bool(String v){if(v.equalsIgnoreCase("true"))return true;if(v.equalsIgnoreCase("false"))return false;throw new IllegalArgumentException("invalid_boolean");}
    private static int number(String v,int min,int max){try{int n=Integer.parseInt(v);if(n<min||n>max)throw new IllegalArgumentException("out_of_range");return n;}catch(NumberFormatException ex){throw new IllegalArgumentException("invalid_number");}}
    private static Map<String,String> parseForm(String body){Map<String,String> out=new LinkedHashMap<>();for(String part:body.split("&")){int i=part.indexOf('=');if(i<0)continue;out.put(URLDecoder.decode(part.substring(0,i),StandardCharsets.UTF_8),URLDecoder.decode(part.substring(i+1),StandardCharsets.UTF_8));}return out;}
    private static boolean constantEquals(String expected,String given){if(given==null)return false;return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),given.getBytes(StandardCharsets.UTF_8));}
    private static String json(String s){return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r");}
    private static void respond(HttpExchange e,int status,String body)throws IOException{byte[] bytes=body.getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");e.getResponseHeaders().set("Cache-Control","no-store");e.sendResponseHeaders(status,bytes.length);e.getResponseBody().write(bytes);}
}
