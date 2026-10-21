# PryanikPlugin 1.0.4

## Changes

- Add a 30-second PvP combat tag with a red boss bar countdown; hits from either combatant refresh both timers. Prevent combat-tagged players from escaping through common teleport, home, lobby, mode, flight, and clan commands.
- Limit selection-axe issuance to 3 per player per rolling hour, globally across griefs, using PostgreSQL; mark the axe and cancel smelting of it.
- Add `/kti_start`, a starter kit with chainmail helmet, leggings and boots, iron chestplate, stone tools, 16 oak logs, furnace, 32 stone, 16 torches, 2 golden apples, and 8 cooked beef. Cooldown is 24 hours per player per grief.
- Set rank order to DEFAULT < VIP < ADMIN < HELPER < CSRV; permit `/van` for HELPER/CSRV, and `/world` and `/setrank` for CSRV (or server OP).

## Compatibility

- Minecraft 1.20.4
- Paper
- Java 17
- PostgreSQL, WorldEdit, and WorldGuard required.
