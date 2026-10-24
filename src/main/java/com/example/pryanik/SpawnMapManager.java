package com.example.pryanik;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

/** Validates a user-supplied zipped Paper world and installs its map data into registered grief spawn worlds. */
public final class SpawnMapManager {
    private static final long MAX_EXPANDED = 512L * 1024 * 1024;
    private static final long MAX_BACKUPS = 16L * 1024 * 1024 * 1024;
    private static final java.util.concurrent.atomic.AtomicBoolean uploadInProgress = new java.util.concurrent.atomic.AtomicBoolean();
    private static final int MAX_ENTRIES = 20_000;
    private static final List<String> MAP_ROOTS = List.of("region", "entities", "poi", "data", "level.dat", "level.dat_old");
    private final PryanikPlugin plugin;
    public SpawnMapManager(PryanikPlugin plugin) { this.plugin = plugin; }

    public String[] uploadAndApply(byte[] archive) throws Exception {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("must_run_on_server_thread");
        if(!uploadInProgress.compareAndSet(false,true))throw new IOException("spawn_map_upload_already_running");
        try {
        if (archive == null || archive.length == 0 || archive.length > 64 * 1024 * 1024) throw new IOException("upload_too_large_or_empty");
        Path work = plugin.getDataFolder().toPath().resolve("spawn-upload-work-" + UUID.randomUUID()).normalize();
        Path source = work.resolve("source");
        Path backups = work.resolve("backups");
        Files.createDirectories(source); Files.createDirectories(backups);
        List<String> relativeFiles;
        try { relativeFiles=extractAndValidate(archive, source); } catch(Exception ex) { deleteTree(work); throw ex; }
        List<String> completed = new ArrayList<>();
        try {
            int[] ensured=plugin.adminHttpApi().createAllConfiguredGriefWorldsForUpload();
            if(ensured[3]!=0)throw new IOException("some_configured_griefs_failed_to_create");
            Set<String> bases = new TreeSet<>();
            for (String[] row : plugin.db().gameInstances()) { String mode=row[0]; int number=Integer.parseInt(row[1]); String base=row[2]; if(Set.of("survival","hardcore","anarchy").contains(mode)&&number>0&&number<=configuredCount(mode)&&base.equals(mode+"_"+number))bases.add(base); }
            for (String[] row : plugin.db().managedWorldSets()) if(row[0].matches("(?:survival|hardcore|anarchy)_[1-9][0-9]*")){String mode=row[0].substring(0,row[0].indexOf('_'));int number=Integer.parseInt(row[0].substring(row[0].indexOf('_')+1));if(number<=configuredCount(mode))bases.add(row[0]);}
            Path worldContainer=Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
            try(var entries=Files.list(worldContainer)){for(Path folder:entries.filter(Files::isDirectory).toList()){String name=folder.getFileName().toString();if(name.matches("(?:survival|hardcore|anarchy)_[1-9][0-9]*_spawn")){String base=name.substring(0,name.length()-6);String mode=base.substring(0,base.indexOf('_'));int number=Integer.parseInt(base.substring(base.indexOf('_')+1));if(number<=configuredCount(mode))bases.add(base);}}}
            Set<String> targets=new TreeSet<>();for(String base:bases)targets.add(base+"_spawn");
            if (targets.isEmpty()) throw new IOException("no_grief_worlds_found");
            long backupBytes=0;for(String name:targets){Path worldPath=Bukkit.getWorldContainer().toPath().resolve(name);rejectSymlinks(worldPath);for(String root:MAP_ROOTS){Path path=worldPath.resolve(root);rejectSymlinks(path);if(Files.isRegularFile(path))backupBytes+=Files.size(path);else if(Files.isDirectory(path))try(var walk=Files.walk(path)){for(Path file:walk.filter(Files::isRegularFile).toList())backupBytes+=Files.size(file);}}if(backupBytes>MAX_BACKUPS)throw new IOException("spawn_backup_would_exceed_16_gib");}
            World lobby = plugin.gameModes().lobby();
            for (String name : targets) {
                if(!Files.isRegularFile(worldContainer.resolve(name).resolve("level.dat"))){if(!Files.exists(worldContainer.resolve(name))){com.example.pryanik.SpawnTemplate.install(plugin,name,worldContainer.resolve(name));}else throw new IOException("spawn_world_missing_level_dat_"+name);}
                World world = Bukkit.getWorld(name);
                if (world != null) {
                    for (Player player : new ArrayList<>(world.getPlayers())) if (lobby == null || !player.teleport(lobby.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN)) throw new IOException("cannot_move_players_from_" + name);
                    if (!Bukkit.unloadWorld(world, true)) throw new IOException("cannot_unload_" + name);
                }
            }
            List<String> backedUp = new ArrayList<>();
            try {
                for (String name : targets) {
                    Path dest = Bukkit.getWorldContainer().toPath().resolve(name).toAbsolutePath().normalize();
                    rejectSymlinks(dest);
                    if (!dest.startsWith(Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize()) || !name.matches("(?:survival|hardcore|anarchy)_[1-9][0-9]*_spawn")) throw new IOException("unsafe_spawn_target");
                    Path backup = backups.resolve(name); Files.createDirectories(backup);
                    backupMapFiles(dest, backup);
                    backedUp.add(name);
                    replaceMapFiles(source, dest, relativeFiles);
                    World loaded = Bukkit.createWorld(new WorldCreator(name).environment(World.Environment.NORMAL));
                    if (loaded == null) throw new IOException("cannot_load_after_install_" + name);
                    plugin.gameModes().configureSpawnWorld(loaded);
                    completed.add(name);
                }
            } catch (Exception failure) {
                for (String name : backedUp) {
                    World current = Bukkit.getWorld(name); if (current != null) Bukkit.unloadWorld(current, true);
                    Path dest = Bukkit.getWorldContainer().toPath().resolve(name).toAbsolutePath().normalize();
                    Path backup = backups.resolve(name);
                    if (Files.isDirectory(backup)) restoreMapFiles(backup, dest);
                    if (Files.isRegularFile(dest.resolve("level.dat"))) Bukkit.createWorld(new WorldCreator(name).environment(World.Environment.NORMAL));
                }
                throw failure;
            }
            Path backupRoot=plugin.getDataFolder().toPath().resolve("spawn-map-backups").resolve(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())+"-"+UUID.randomUUID());Files.createDirectories(backupRoot.getParent());try{Files.move(backups,backupRoot,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException ex){Files.move(backups,backupRoot);}deleteTree(work);
            plugin.getLogger().info("Applied uploaded spawn map to " + completed.size() + " registered grief spawn worlds; safety backups at " + backupRoot);
            return new String[]{String.valueOf(completed.size()),String.valueOf(relativeFiles.size()),backupRoot.toString()};
        } finally { if(Files.exists(work.resolve("source")))deleteTree(work.resolve("source")); if(Files.exists(work))try(var entries=Files.list(work)){if(entries.findAny().isEmpty())Files.deleteIfExists(work);} }
        } finally { uploadInProgress.set(false); }
    }

    private int configuredCount(String mode){if(!plugin.getConfig().getBoolean("game-modes."+mode+".enabled",true))return 0;int defaultCount=switch(mode){case "survival"->20;case "hardcore"->5;case "anarchy"->15;default->0;};return Math.max(0,plugin.getConfig().getInt("game-modes."+mode+".instances",defaultCount));}

    private List<String> extractAndValidate(byte[] archive, Path target) throws IOException {
        List<String> files = new ArrayList<>(); long expanded = 0; int entries = 0; boolean level = false, region = false;
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ENTRIES) throw new IOException("too_many_zip_entries");
                String name = entry.getName().replace('\\', '/');
                if (entry.isDirectory()) continue;
                if (name.startsWith("/") || name.contains(":") || Arrays.asList(name.split("/", -1)).contains("..") || Arrays.asList(name.split("/", -1)).contains(".")) throw new IOException("unsafe_zip_path");
                boolean allowed = name.equals("level.dat") || name.equals("level.dat_old") || MAP_ROOTS.stream().anyMatch(root -> name.startsWith(root + "/"));
                if (!allowed || name.endsWith("/") || name.contains("//")) throw new IOException("unsupported_world_file");
                Path output = target.resolve(name).normalize(); if (!output.startsWith(target)) throw new IOException("unsafe_zip_path");
                Files.createDirectories(output.getParent()); long written = 0;
                try (var out = Files.newOutputStream(output, StandardOpenOption.CREATE_NEW)) {
                    byte[] buffer = new byte[8192]; int read;
                    while ((read = zip.read(buffer)) != -1) { written += read; expanded += read; if (expanded > MAX_EXPANDED) throw new IOException("expanded_archive_too_large"); out.write(buffer, 0, read); }
                }
                files.add(name); if (name.equals("level.dat")) level = written > 0;
                if (name.matches("region/r\\.-?[0-9]+\\.-?[0-9]+\\.mca")) region = true;
                zip.closeEntry();
            }
        } catch (java.util.zip.ZipException ex) { throw new IOException("invalid_zip_archive", ex); }
        if (!level || !region) throw new IOException("zip_must_contain_level_dat_and_region_mca");
        return files;
    }

    private static void rejectSymlinks(Path path) throws IOException { if(Files.exists(path,LinkOption.NOFOLLOW_LINKS)&&Files.isSymbolicLink(path))throw new IOException("symlink_in_world_path"); }

    private void backupMapFiles(Path world, Path backup) throws IOException {
        for (String root : MAP_ROOTS) {
            Path from = world.resolve(root); rejectSymlinks(from); if (Files.isDirectory(from))try(var paths=Files.walk(from)){for(Path child:paths.toList())rejectSymlinks(child);} if (Files.exists(from)) copyTree(from, backup.resolve(root));
        }
    }
    private void replaceMapFiles(Path source, Path world, List<String> files) throws IOException {
        rejectSymlinks(world);Files.createDirectories(world);
        for (String root : MAP_ROOTS){rejectSymlinks(world.resolve(root));deleteTree(world.resolve(root));}
        for (String name : files) copyTree(source.resolve(name), world.resolve(name));
        Files.deleteIfExists(world.resolve("session.lock"));
    }
    private void restoreMapFiles(Path backup, Path world) throws IOException {
        for (String root : MAP_ROOTS) {
            rejectSymlinks(world.resolve(root));deleteTree(world.resolve(root)); Path from = backup.resolve(root); if (Files.exists(from)) copyTree(from, world.resolve(root));
        }
    }
    private static void copyTree(Path from, Path to) throws IOException {
        if (Files.isDirectory(from)) { Files.createDirectories(to); try (var paths = Files.list(from)) { for (Path child : paths.toList()) copyTree(child, to.resolve(child.getFileName())); } }
        else if (Files.isRegularFile(from)) { Files.createDirectories(to.getParent()); Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES); }
    }
    private static void deleteTree(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (var paths = Files.walk(path)) { for (Path item : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(item); }
    }
}
