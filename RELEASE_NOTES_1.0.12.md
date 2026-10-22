# PryanikPlugin 1.0.12

## Changes

- Bundle the supplied `121` Minecraft save as the spawn-world template.
- On first creation of a grief's `<grief>_spawn` world, copy the bundled map into the server world folder before Paper loads it.
- Existing Spawn folders are never overwritten. Source-world player records, server config, session lock, and UID are excluded.

## Compatibility

- Minecraft 1.20.4; Paper; Java 17
- PostgreSQL, WorldEdit, and WorldGuard required.
