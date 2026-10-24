package com.example.pryanik;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Stores managed grief worlds below worlds/<grief>/ while keeping Bukkit's root-level world paths as links. */
public final class ManagedWorldFolders {
    private final Path serverRoot;
    private final Path worldsRoot;

    public ManagedWorldFolders(Path serverRoot) {
        this.serverRoot = serverRoot.toAbsolutePath().normalize();
        this.worldsRoot = this.serverRoot.resolve("worlds");
    }

    /** Returns Bukkit's expected folder after moving legacy data and creating a directory symlink. */
    public Path prepareWorldFolder(String worldName) throws IOException {
        String base = instanceBase(worldName);
        Path alias = serverRoot.resolve(worldName).normalize();
        if (base == null) return alias;
        Path instance = worldsRoot.resolve(base).normalize();
        Path destination = instance.resolve(dimensionFolder(worldName)).normalize();
        if (!alias.startsWith(serverRoot) || !instance.startsWith(worldsRoot) || !destination.startsWith(instance))
            throw new IOException("Unsafe managed world path: " + worldName);
        if (Files.isSymbolicLink(alias)) {
            if (!Files.exists(alias)) throw new IOException("Managed world link is dangling: " + alias);
            return alias;
        }
        Files.createDirectories(instance);
        if (Files.exists(alias)) {
            if (Files.exists(destination)) throw new IOException("Both legacy and worlds/ folders exist; refusing to merge: " + worldName);
            try {
                Files.move(alias, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(alias, destination);
            }
        } else if (Files.exists(destination)) {
            // Existing data in the new layout: recreate only the Bukkit alias.
        } else {
            Files.createDirectories(destination);
        }
        createDirectoryLink(alias, destination, worldName);
        return alias;
    }

    private static void createDirectoryLink(Path alias, Path destination, String worldName) throws IOException {
        try {
            Files.createSymbolicLink(alias, destination);
            return;
        } catch (UnsupportedOperationException | SecurityException | IOException symlinkFailure) {
            if (System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win")) {
                Process process = new ProcessBuilder("cmd.exe", "/c", "mklink", "/J", alias.toString(), destination.toString())
                        .redirectErrorStream(true).start();
                try {
                    byte[] output = process.getInputStream().readAllBytes();
                    if (process.waitFor() == 0 && Files.isDirectory(alias)) return;
                    IOException junctionFailure = new IOException(new String(output, java.nio.charset.Charset.defaultCharset()).trim());
                    junctionFailure.addSuppressed(symlinkFailure);
                    throw new IOException("Cannot create a Windows junction for managed world " + worldName + "; check permissions and that the server root and worlds folder are on the same volume", junctionFailure);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted creating Windows junction for " + worldName, ex);
                }
            }
            throw new IOException("Cannot create a directory link for managed world " + worldName, symlinkFailure);
        }
    }

    public Path instanceFolder(String worldName) {
        String base = instanceBase(worldName);
        return base == null ? null : worldsRoot.resolve(base);
    }

    public Path managedDataFolder(String worldName) {
        String base = instanceBase(worldName);
        return base == null ? serverRoot.resolve(worldName) : worldsRoot.resolve(base).resolve(dimensionFolder(worldName));
    }

    public boolean hasWorldData(String worldName) { return Files.exists(serverRoot.resolve(worldName)) || Files.exists(managedDataFolder(worldName)); }

    public Path spawnTemplateFolder(String worldName) throws IOException { return prepareWorldFolder(worldName); }

    private static String dimensionFolder(String name) {
        String base = instanceBase(name);
        return name.equalsIgnoreCase(base) ? "world" : name.substring(base.length() + 1).toLowerCase(java.util.Locale.ROOT);
    }

    private static String instanceBase(String name) {
        if (name == null || !name.matches("(?i)(survival|hardcore|anarchy)_[1-9][0-9]*(?:_(?:nether|the_end|spawn))?")) return null;
        int suffix = name.indexOf('_', name.indexOf('_') + 1);
        return suffix < 0 ? name : name.substring(0, suffix);
    }
}
