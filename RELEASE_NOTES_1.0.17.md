# PryanikPlugin 1.0.17

## Changes

- Track entry into a configured portal without cancelling the player's movement (which could close inventories and retrigger vanilla portal handling).
- On the actual Bukkit portal teleport event, cancel the teleport at high priority and open the configured action.
- Look up the action from nearby portal blocks as a fallback when the portal event arrives before the move trigger.
- Add a long portal cooldown to prevent repeated activations.

## Compatibility

- Minecraft 1.20.4; Paper; Java 17
