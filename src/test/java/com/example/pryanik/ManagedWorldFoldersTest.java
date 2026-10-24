package com.example.pryanik;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Lightweight filesystem regression tests; run with assertions enabled. */
public final class ManagedWorldFoldersTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("managed-world-folders-");
        try {
            ManagedWorldFolders folders = new ManagedWorldFolders(root);
            check(folders.managedDataFolder("survival_1").equals(root.resolve("worlds/survival_1/world")), "overworld path");
            check(folders.managedDataFolder("survival_1_nether").equals(root.resolve("worlds/survival_1/nether")), "nether path");
            check(folders.managedDataFolder("survival_1_the_end").equals(root.resolve("worlds/survival_1/the_end")), "end path");
            check(folders.managedDataFolder("survival_1_spawn").equals(root.resolve("worlds/survival_1/spawn")), "spawn path");
            check(folders.managedDataFolder("lobby").equals(root.resolve("lobby")), "lobby remains in root");
            Path legacy = root.resolve("survival_3");
            Files.createDirectories(legacy);
            Files.writeString(legacy.resolve("level.dat"), "worlddata");
            folders.prepareWorldFolder("survival_3");
            check(Files.exists(legacy.resolve("level.dat")), "Bukkit alias reaches migrated world");
            check(Files.readString(root.resolve("worlds/survival_3/world/level.dat")).equals("worlddata"), "legacy world moved intact");
            check(folders.hasWorldData("survival_3"), "world recognized after migration");
            Files.createDirectories(root.resolve("survival_2"));
            Files.createDirectories(root.resolve("worlds/survival_2/world"));
            boolean refused = false;
            try { folders.prepareWorldFolder("survival_2"); } catch (IOException expected) { refused = true; }
            check(refused, "conflicting folders are not merged");
            System.out.println("ManagedWorldFoldersTest PASSED");
        } finally {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
}
