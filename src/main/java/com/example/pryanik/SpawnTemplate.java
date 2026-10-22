package com.example.pryanik;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Installs the bundled, server-neutral spawn map into a newly-created grief world folder. */
public final class SpawnTemplate {
    private static final String RESOURCE_PREFIX = "spawn-template/";
    private SpawnTemplate() {}

    public static boolean install(PryanikPlugin plugin, String worldName, Path target) throws IOException {
        if (!worldName.endsWith("_spawn")) throw new IOException("Refusing to install Spawn map into non-Spawn world: " + worldName);
        Path normalized = target.toAbsolutePath().normalize();
        if (Files.exists(normalized.resolve("level.dat"))) return false;
        if (Files.exists(normalized)) {
            try (var files = Files.list(normalized)) {
                if (files.findAny().isPresent())
                    throw new IOException("Spawn world folder is non-empty but has no level.dat; refusing to overwrite: " + normalized);
            }
        }
        Files.createDirectories(normalized);
        Path pluginJar;
        try {
            pluginJar = Path.of(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().normalize();
        } catch (java.net.URISyntaxException ex) {
            throw new IOException("Invalid plugin JAR location", ex);
        }
        if (!Files.isRegularFile(pluginJar)) throw new IOException("Plugin code source is not a JAR: " + pluginJar);
        List<String> names = new ArrayList<>();
        try (JarFile jar = new JarFile(pluginJar.toFile())) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().startsWith(RESOURCE_PREFIX)) names.add(entry.getName());
            }
            if (names.stream().noneMatch(n -> n.equals(RESOURCE_PREFIX + "level.dat")))
                throw new IOException("Bundled spawn template is missing level.dat");
            for (String name : names) {
                String relative = name.substring(RESOURCE_PREFIX.length());
                Path output = normalized.resolve(relative).normalize();
                if (!output.startsWith(normalized)) throw new IOException("Invalid template path: " + name);
                Files.createDirectories(output.getParent());
                try (InputStream in = jar.getInputStream(jar.getJarEntry(name))) {
                    Files.copy(in, output);
                }
            }
        }
        Path sessionLock = normalized.resolve("session.lock");
        Files.deleteIfExists(sessionLock);
        plugin.getLogger().info("Installed bundled Spawn map into " + normalized + " (" + names.size() + " files).");
        return true;
    }
}
