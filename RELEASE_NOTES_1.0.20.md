# PryanikPlugin 1.0.20

## Changes

- For the `/mode` portal action, leave the portal and then travel to the lobby before opening the mode menu.
- Wait for the lobby arrival/configuration callback before showing the menu, avoiding a world transition that immediately replaces/closes it.
- Keep the player invulnerable in the lobby while the menu is open.

## Compatibility

- Minecraft 1.20.4; Paper; Java 17
