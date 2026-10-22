# PryanikPlugin 1.0.5

## Changes

- Rename `/kti_start` to `/kit_start`.
- Add `/gm 0|1|2` for survival, creative, and spectator. Spectator mode is restricted to HELPER and CSRV.
- Clear the combat tag and boss bar on death so a defeated player's timer cannot remain active after respawn.
- Add `/pvp on|off` for CSRV to control player combat independently in each managed grief.
- Rename the 24-hour starter-kit command to `/kit_start` (the kit cooldown remains per player and grief).

## Compatibility

- Minecraft 1.20.4
- Paper
- Java 17
- PostgreSQL, WorldEdit, and WorldGuard required.
