# PryanikPlugin 1.2.0 — Paper 26.3

- `/portal` opens the existing command-selection UI for a complete unlit obsidian frame; it no longer lights/changes a frame by itself.
- When `/portal` is run inside an already configured portal, the barrier action removes its saved command mapping; the barrier on an unlit frame has no effect.
- Selecting an action grants a one-use, named flint-and-steel item bound to the frame nearest the player when `/portal` was run.
- Right-clicking the chosen obsidian frame with that item lights the portal and saves the selected action to all portal blocks. Other frames cannot be lit/configured with that item.
- The special flint is consumed only after a successful ignition and database save. Regular flint-and-steel is unchanged.
- Paper 26.3, Java 25; WorldEdit 7.4.6 and WorldGuard 7.0.20.
