# PryanikPlugin 1.4.0 — Paper 26.3

- Expand CSRV `/ppadmin` with persistent per-world difficulty settings, per-world spawn-point storage, grief settings and command allowlists.
- Add `/ppadmin modes` to enable/disable Survival, Hardcore and Anarchy and configure instance counts and player limits.
- Game-mode selector now hides disabled modes and displays configured instance/capacity values; disabled modes reject entry.
- Website server-save/RCON button removed from the private admin page. No website files or `.env` are included in this plugin release.
- Lobby spawn coordinates can be configured under `admin-worlds.lobby.spawn` and persist across restarts.
