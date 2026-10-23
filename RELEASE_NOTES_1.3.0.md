# PryanikPlugin 1.3.0 — Paper 26.3

- Enable personal named homes in anarchy instances; each home's identity is scoped by player, name and exact world, so equal names can exist in separate worlds/griefs.
- Register `/home` and `/chome` command handlers and inventory/chat listeners; both are available from lobby command gating, while home use is limited to gameplay worlds.
- Add CSRV-only `/ppadmin` world settings menu for PvP (persisted to the grief setting and applied to linked gameplay dimensions), block placement/breaking, interaction and spawn point.
- `/ppadmin commands <all|none|command,command,...|reset>` stores an enforced per-world command allowlist; login/registration and admin recovery commands remain available.
- Add a private website admin action that sends only the fixed RCON command `/save-all`; RCON secrets are environment variables only and the website files are not included in this public plugin release.
- Server restart remains a hosting-panel operation; `save-all` is not a restart.
