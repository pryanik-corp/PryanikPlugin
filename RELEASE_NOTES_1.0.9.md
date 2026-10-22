# PryanikPlugin 1.0.9

## Changes

- New griefs now include a fourth independent normal-generation world named `<grief>_spawn`, in addition to Overworld, Nether, and End.
- The Spawn world is registered with the grief, loaded after restart, isolated for visibility/game-state, and Nether/End portal travel from it returns to the grief Overworld.
- `/gm` is limited to ADMIN and higher; CSRV may use `/gm 1` in the lobby. Only CSRV may use spectator mode.

## Compatibility

- Minecraft 1.20.4
- Paper
- Java 17
- PostgreSQL, WorldEdit, and WorldGuard required.
