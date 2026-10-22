# PryanikPlugin 1.0.14

## Changes

- A configured command portal now fires as soon as the player first steps into the portal block, without waiting for the vanilla portal warm-up timer.
- The portal transition is cancelled so the player stays in place while the assigned action runs.
- Portal actions trigger once per entry; walking out and stepping back in triggers them again.

## Compatibility

- Minecraft 1.20.4; Paper; Java 17
