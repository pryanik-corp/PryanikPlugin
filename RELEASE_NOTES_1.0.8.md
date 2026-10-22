# PryanikPlugin 1.0.8

## Changes

- `/portal` opens its configuration UI before igniting an unlit Nether portal frame.
- The chosen command is saved first; the frame is then revalidated and activated. Removing an assignment does not ignite the frame.
- If the frame changed before confirmation, no command is assigned and no portal is lit.

## Compatibility

- Minecraft 1.20.4
- Paper
- Java 17
- PostgreSQL, WorldEdit, and WorldGuard required.
