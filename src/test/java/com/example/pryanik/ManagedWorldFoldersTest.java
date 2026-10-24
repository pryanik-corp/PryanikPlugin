package com.example.pryanik;

import java.nio.file.Files;
import java.nio.file.Path;

/** Filesystem regression tests for Paper's actual world folder layout. */
public final class ManagedWorldFoldersTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("managed-world-folders-");
        ManagedWorldFolders folders = new ManagedWorldFolders(root);
        check(folders.worldFolder("survival_1").equals(root.resolve("survival_1")), "overworld root folder");
        check(folders.worldFolder("survival_1_nether").equals(root.resolve("survival_1_nether")), "nether root folder");
        check(folders.worldFolder("lobby").equals(root.resolve("lobby")), "lobby root folder");
        Files.createDirectories(root.resolve("survival_1"));
        Files.writeString(root.resolve("survival_1/level.dat"), "existing");
        check(folders.prepareWorldFolder("survival_1").equals(root.resolve("survival_1")), "prepare preserves canonical folder");
        check(Files.readString(root.resolve("survival_1/level.dat")).equals("existing"), "existing data unchanged");
        check(!Files.exists(root.resolve("worlds")), "does not create incompatible worlds directory");
        System.out.println("ManagedWorldFoldersTest PASSED");
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
