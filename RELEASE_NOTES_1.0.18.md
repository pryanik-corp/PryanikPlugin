# PryanikPlugin 1.0.18

## Changes

- For a configured portal, cancel the vanilla portal teleport and move the player to the nearest safe block beside the portal.
- Open the configured action only after the player has left the portal block, preventing immediate re-entry and GUI closing.
- The original action is also cancelled and handled if Paper begins the portal teleport before the movement trigger.

## Compatibility

- Minecraft 1.20.4; Paper; Java 17
