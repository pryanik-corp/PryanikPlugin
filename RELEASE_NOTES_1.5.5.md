# PryanikPlugin 1.5.5 — Paper 26.3

- Revert the incompatible nested `worlds/<grief>/<dimension>` storage experiment. Paper 26.3 creates and loads worlds by server-root level name; the prior layout logged missing `level.dat` and caused new empty worlds to be generated.
- Restore use of Paper's canonical root-level folders (`survival_1`, `survival_1_nether`, etc.) so existing grief data loads and persists across restarts.
- Do not move, delete, or overwrite existing world folders automatically.
