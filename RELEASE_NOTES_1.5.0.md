# PryanikPlugin 1.5.0 — Paper 26.3

- Add optional private LAN HTTP API for the CSRV website. Disabled by default; requires 32+ character secret and exact allowed IIS client IP.
- Strict allowlist for reading/updating world PvP, difficulty, block placement/break/interact, spawn coordinates, allowed commands, and Survival/Hardcore/Anarchy enablement, instance count and capacity.
- No console commands, arbitrary RCON, restart, or save endpoint. Keep the API port/firewall restricted to the website host; HTTP is cleartext and must stay LAN-only.
- Website integration files are private and intentionally excluded from this public release.
