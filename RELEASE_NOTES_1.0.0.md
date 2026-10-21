# PryanikPlugin 1.0.0

Initial packaged release for Paper 1.20.4 / Java 17.

## Included

- PostgreSQL-backed registration/login with BCrypt password hashing.
- HikariCP connection pooling and shaded/relocated HikariCP, PostgreSQL JDBC, and BCrypt libraries.
- Mode selector UI and grief instance routing across Overworld, Nether, and End.
- Per-lobby/per-grief player inventory, armor, offhand, and experience scopes.
- Per-instance chat, clan and WorldGuard private-region namespaces.
- Lobby protection, selector compass, and command visibility/enforcement.
- WorldEdit/WorldGuard integration and optional Multiverse-Core support.
- PostgreSQL player data viewer utility.

## Build

Requires JDK 17 and Maven:

```sh
mvn clean package
```

The shaded plugin artifact is `target/PryanikPlugin-1.0.0.jar`.

## Configuration and security

Copy `src/main/resources/config.yml` to the plugin's config directory and set database credentials locally. Never commit runtime configs, credentials, server properties, logs, player data, worlds, or backups.

Gameplay behaviors must be validated on a test server before production use; successful compilation alone does not prove gameplay correctness.
