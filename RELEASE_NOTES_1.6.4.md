# PryanikPlugin 1.6.4 — Paper 26.3

- Add a private admin website upload control for a user's own spawn map ZIP.
- Validate archive paths, allowed map data only, required `level.dat` and region `.mca`, entry count, expanded size, and upload size.
- Create/load configured grief worlds before applying the map; apply only to each configured grief's separate `_spawn` map data.
- Move online players from spawn worlds to the lobby, save/unload those worlds, and preserve rollback backups in the plugin data directory.
- Reject malformed/non-empty paths and restore backups if any replacement world fails to load.
- Website PHP changes are private and must be copied separately to IIS; do not publish `web/` publicly.
