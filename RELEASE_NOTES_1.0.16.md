# PryanikPlugin 1.0.16

## Changes

- Record the configured action when a player first steps into a command portal.
- Cancel the resulting `PlayerPortalEvent` at its lowest priority before other handlers can open a destination UI or redirect the player.
- Execute the configured action after the portal teleport is cancelled; player movement itself is not cancelled, so inventories stay open.

## Compatibility

- Minecraft 1.20.4; Paper; Java 17
