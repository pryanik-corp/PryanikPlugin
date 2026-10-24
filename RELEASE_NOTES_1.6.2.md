# PryanikPlugin 1.6.2 — Paper 26.3

- Fix website load-all to enumerate both registered game instances and explicitly managed world sets.
- Return counts for world sets found, worlds loaded, already-loaded worlds, and missing/failed folders; report individual load failures in the server log.
- Increase the admin API timeout for loading many worlds.
- Deploy the matching private website PHP files separately from the public plugin JAR.
