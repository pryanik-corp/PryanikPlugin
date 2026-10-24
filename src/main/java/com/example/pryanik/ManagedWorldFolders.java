package com.example.pryanik;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Compatibility helper: managed world data remains in Paper's canonical root-level folders. */
public final class ManagedWorldFolders {
    private final Path serverRoot;
    private final Path worldsRoot;

    public ManagedWorldFolders(Path serverRoot) {
        this.serverRoot = serverRoot.toAbsolutePath().normalize();
        this.worldsRoot = this.serverRoot.resolve("worlds");
    }

    public Path worldFolder(String worldName) { return serverRoot.resolve(worldName); }

    public Path instanceFolder(String worldName) {
        String base = instanceBase(worldName);
        return base == null ? null : worldsRoot.resolve(base);
    }

    public Path managedDataFolder(String worldName) {
        String base = instanceBase(worldName);
        return base == null ? serverRoot.resolve(worldName) : worldsRoot.resolve(base).resolve(dimensionFolder(worldName));
    }

    /** Migrates root-level legacy data to worlds/<instance>/<dimension>; on conflict retains both and refuses load. */
    public Path prepareWorldFolder(String worldName) throws IOException {
        return worldFolder(worldName);
    }

    public boolean hasWorldData(String worldName) {
        return Files.exists(serverRoot.resolve(worldName)) || Files.exists(managedDataFolder(worldName));
    }

    public boolean isManagedAlias(String worldName) { return false; }

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
