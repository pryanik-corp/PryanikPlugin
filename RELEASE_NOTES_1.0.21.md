# PryanikPlugin 1.0.21

## Changes

- Fix `/mode` portal action so lobby travel uses the normal lobby transition and mode menu opens only from the confirmed lobby-arrival callback.
- Remove the competing direct mode-menu teleport path that could return the player or close the inventory during world changes.
- Keep the lobby's Adventure/invulnerability configuration in effect after the menu opens.

## Compatibility

- Minecraft 1.20.4; Paper; Java 17
