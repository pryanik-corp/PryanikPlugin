# PryanikPlugin 1.5.4 — Paper 26.3

- Store managed grief world data under `worlds/<grief>/world`, `nether`, `the_end`, and `spawn`.
- Bukkit-compatible root-level directory symlinks allow Paper to load worlds by their existing names.
- Existing root-level grief data moves into the new layout on next startup; conflicting old/new folders are left untouched and reported instead of merged or overwritten.
- Lobby and unrelated custom worlds remain in their existing locations.
