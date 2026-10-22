# PryanikPlugin

## Modrinth summary / Краткое описание для Modrinth

A Paper 1.20.4 plugin with lobbies, Survival/Anarchy/Hardcore modes, clans, protected regions, and homes. | Плагин для Paper 1.20.4: лобби, режимы Survival/Anarchy/Hardcore, кланы, приваты и дома.

## English

A Minecraft server plugin for **Paper 1.20.4**, featuring a lobby, game worlds and modes, player authentication, clans, protected regions, and homes.

### Features

- Lobby with a compass and a GUI for choosing a game mode.
- Separate Survival grief worlds with per-world game-state storage.
- Survival, Anarchy, and Hardcore modes.
- Password-based registration and login. Passwords are stored as BCrypt hashes.
- Ranks, private messages, teleport requests, and player reports.
- Clans and clan-management tools.
- Named personal homes, scoped to the world/grief where they were created. Use `/home` to open the home menu or `/home <name>` to teleport to a home by name in the current world.
- Clan home management through `/chome`.
- Personal and clan WorldGuard regions selected with WorldEdit. Owners of personal regions can add other players.
- An optional desktop PostgreSQL data viewer (`players_viewer.py`). This is a separate utility, not part of the in-game plugin.

> The plugin also includes administrative commands. Their syntax and permissions depend on the server configuration.

### Compatibility

- **Minecraft:** 1.20.4
- **Server:** Paper (Bukkit/Spigot API)
- **Java:** 17
- **Required plugins:** [WorldEdit](https://modrinth.com/plugin/worldedit) and [WorldGuard](https://modrinth.com/plugin/worldguard)
- **PostgreSQL:** a reachable database is required
- **Multiverse-Core:** optional; only needed for related extra world-management features

### Installation

1. Install Java 17, Paper 1.20.4, WorldEdit, and WorldGuard.
2. Download the JAR from the **Versions** tab and place it in your server's `plugins` folder.
3. Start the server once to generate `plugins/PryanikPlugin/config.yml`, then stop it.
4. Configure the PostgreSQL JDBC URL, username, and password. Ensure the Minecraft server can connect to the database and that the database user has permission to create and modify tables.
5. Start the server again and check the console for successful PryanikPlugin activation and a PostgreSQL connection.

Example JDBC URL: `jdbc:postgresql://localhost:5432/pryanik`

**Do not publish your configuration file with a real database password.**

### Main commands

| Command | Description |
|---|---|
| `/reg <password> [repeat]` | Register an account |
| `/l <password>` | Log in |
| `/mode` | Open the game-mode menu |
| `/home` | Open the personal homes menu for the current world |
| `/home <name>` | Teleport to a named personal home in the current world |
| `/chome` | Open the clan home menu |
| `/privat` | Open the personal and clan region menu |
| `/clan` | Open clan commands and menu |
| `/lobby` | Return to the lobby, if permitted by the server configuration |

Command availability may depend on the current mode, authentication status, and player permissions.

## Русский

Плагин для Minecraft-серверов на **Paper 1.20.4**: лобби, игровые миры и режимы, авторизация, кланы, приваты и дома.

### Возможности

- Лобби с компасом и меню выбора игрового режима.
- Отдельные миры Survival-грифов с раздельным сохранением игрового состояния.
- Режимы Survival, Anarchy и Hardcore.
- Регистрация и вход по паролю. Пароли хранятся в виде BCrypt-хешей.
- Ранги, личные сообщения, запросы телепортации и жалобы на игроков.
- Кланы и инструменты управления ими.
- Именованные личные дома, доступные в том мире/грифе, где они созданы. Команда `/home` открывает меню, а `/home <имя>` телепортирует к дому с таким именем в текущем мире.
- Управление домом клана через `/chome`.
- Личные и клановые регионы WorldGuard, создаваемые по выделению WorldEdit. Владельцы личных приватов могут добавлять других игроков.
- Дополнительная настольная программа для просмотра данных PostgreSQL (`players_viewer.py`). Она распространяется отдельно от игрового плагина.

> В плагине также есть административные команды. Их синтаксис и права зависят от конфигурации сервера.

### Совместимость

- **Minecraft:** 1.20.4
- **Сервер:** Paper (Bukkit/Spigot API)
- **Java:** 17
- **Обязательные плагины:** [WorldEdit](https://modrinth.com/plugin/worldedit) и [WorldGuard](https://modrinth.com/plugin/worldguard)
- **PostgreSQL:** требуется доступная база данных
- **Multiverse-Core:** необязателен; нужен только для связанных дополнительных функций управления мирами

### Установка

1. Установите Java 17, Paper 1.20.4, WorldEdit и WorldGuard.
2. Скачайте JAR во вкладке **Versions** и поместите его в папку `plugins` сервера.
3. Запустите сервер один раз, чтобы создать `plugins/PryanikPlugin/config.yml`, затем остановите его.
4. Укажите в конфигурации JDBC URL, имя пользователя и пароль PostgreSQL. Убедитесь, что сервер может подключиться к базе, а у пользователя БД есть права на создание и изменение таблиц.
5. Снова запустите сервер и проверьте консоль: плагин должен успешно включиться и подключиться к PostgreSQL.

Пример JDBC URL: `jdbc:postgresql://localhost:5432/pryanik`

**Не публикуйте конфигурацию с настоящим паролем базы данных.**

### Основные команды

| Команда | Назначение |
|---|---|
| `/reg <пароль> [повтор]` | Зарегистрировать аккаунт |
| `/l <пароль>` | Войти в аккаунт |
| `/mode` | Открыть меню игровых режимов |
| `/home` | Открыть меню личных домов в текущем мире |
| `/home <имя>` | Телепортироваться к личному дому с таким именем в текущем мире |
| `/chome` | Открыть меню дома клана |
| `/privat` | Открыть меню личных и клановых приватов |
| `/clan` | Команды и меню клана |
| `/lobby` | Вернуться в лобби, если команда разрешена настройками сервера |

Доступность команд может зависеть от текущего режима, авторизации и прав игрока.

## Release / Релиз

Source code and downloadable JAR files / Исходный код и готовые JAR-файлы: [GitHub Releases](https://github.com/pryanik-corp/PryanikPlugin/releases). Current version / Текущая версия: **1.0.11**.
