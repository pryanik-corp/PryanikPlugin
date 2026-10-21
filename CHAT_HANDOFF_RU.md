# Передача контекста PryanikPlugin (для нового чата)

Прочитай этот файл в начале нового чата. Он описывает рабочее состояние проекта и текущие договорённости.

## Проект

- Рабочая папка: `C:\Users\pryanik\Documents\PryanikPlugin`
- Minecraft Paper 1.20.4, Java 17, Maven.
- Плагин `PryanikPlugin`, исходники `src/main/java`, plugin metadata `src/main/resources/plugin.yml`, сборка `mvn clean package`.
- PostgreSQL прямое JDBC соединение с HikariCP; пароль не публиковать и не включать в ответы.
- WorldEdit/WorldGuard обязательны; Multiverse-Core заявлен как optional softdepend.

## Сборка/установка

Из рабочей папки:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
$env:PATH="$env:TEMP\apache-maven-3.10.0\bin;$env:JAVA_HOME\bin;$env:PATH"
mvn clean package
Copy-Item 'target\PryanikPlugin-1.0.0.jar' 'server\plugins\PryanikPlugin-1.0.0.jar' -Force
```

После замены JAR нужен полный перезапуск Paper. Последняя известная сборка `BUILD SUCCESS`; deployed JAR был `server/plugins/PryanikPlugin-1.0.0.jar`.

## Функции плагина

- Регистрация/логин через PostgreSQL и BCrypt, ранги, fly/van/AFK, TPA, сообщения, блокировка TPA.
- `/privat <имя>` использует текущее выделение WorldEdit pos1/pos2. Не возвращать огромные фиксированные радиусы.
- Кланы с PostgreSQL: `/clan` GUI; `/clan create <имя>`, `invite`, `accept`, `leave`, `info`, `disband`; `/clanset <ник>` добавляет онлайн-игрока.
- Личный дом: `/home set`, `/home`.
- Клановый дом: `/chome set`, `/chome`.
- Приват клана: лидер делает WorldEdit selection, `/chprivat <имя>`; удалить `/chprivatdelete <имя>`.
- Multiworld `/world`: операторский GUI, list/tp/create/load/unload, `/world setjoin <мир>`, `/world setbase <имя>`, `/world delete <имя> DELETE`.
- Настроенный `join-world` в `config.yml` должен телепортировать игрока сразу при join до авторизации.
- `/world setbase` меняет `level-name` в `server.properties`; для связанного Overworld/Nether/End требуется полный серверный рестарт.
- Через `/world create` созданные и через `/world load` загруженные миры записываются в PostgreSQL `plugin_worlds` (имя + environment), затем загружаются при старте.
- Сами файлы мира не находятся в PostgreSQL: они лежат на диске в world-container Paper. Миры, созданные до регистрации `plugin_worlds`, нужно один раз загрузить `/world load <имя>`.
- Удаление мира защищено подтверждением `DELETE`, запрещены основной мир/измерения, текущий join-мир, мир с игроками. Команду удаления файлов проверять особенно тщательно.

## Viewer приложения

- `players_viewer.py`: Tkinter/PostgreSQL GUI, показывает nickname/UUID/rank/register_code, парольный хеш не читает.
- Можно удалить выбранного пользователя с подтверждением.
- Кнопка очистки всех данных требует пароль БД, точное `YES`, затем ещё одно подтверждение; удаляет только строки из `players`, `clans`, `clan_members`, `tp_blocks`.
- Установка: `python -m pip install -r requirements-viewer.txt`, запуск `python players_viewer.py`.
- Синтаксис проверялся через `py_compile`.

## PostgreSQL

- Типичный endpoint ранее: `192.168.100.8:5432`, база `Pryanik-Plugin`; точные параметры проверять в действующем серверном `plugins/PryanikPlugin/config.yml`.
- В одном из прошлых серверных конфигов пользователь `postgres` подключился успешно; для конкретного Paper важно проверить фактический пароль и настоящий `pg_hba.conf` через `SHOW hba_file`.
- Database constructor автоматически создаёт/мигрирует `players`, `tp_blocks`, `clans`, `clan_members`, home columns и теперь `plugin_worlds`.
- Viewer-role с правами на удаление: `GRANT DELETE ON TABLE public.players, public.clans, public.clan_members, public.tp_blocks TO players_viewer;` (только если владелец хочет удалить/очистить данные через GUI).

## Важные оговорки / потенциальные проблемы для следующего чата

1. **Проверить ближайший текущий серверный лог при первом обращении.** Последнее, что подтверждено — успешная сборка и копирование JAR, не запуск сервера с этой сборкой.
2. Проверь реализацию генерации миров: `/world create` вызывает `Bukkit.createWorld`; это создаёт отдельную папку мира. В PostgreSQL записывается метадата, не содержимое карты.
3. Проверить безопасное удаление мира и удаление его записи из `plugin_worlds` в текущем коде. На момент разработки запись удалялась в delete handler после выгрузки; если DB delete падает, папку удалять нельзя.
4. `setbase` редакция `server.properties` во время работы не меняет текущую сессию; обязательно нужен полный останов/запуск.
5. При следующей смене feature собирать JAR самостоятельно и подтверждать конкретный результат Maven `BUILD SUCCESS` до заявления о deploy.
6. Пользователь устал от инструкций «сделай сам»: предпочтительно самому отредактировать файлы, собрать и скопировать JAR, сообщая ограничения только если реально не удалось.
7. Не повторять и не разглашать секреты/пароли, появлявшиеся в прошлых чатах. Рекомендовать ротацию пароля, если это важно, без цитирования.

## Полезные файлы

- Java DB слой: `src/main/java/com/example/pryanik/Database.java`
- Plugin startup/events: `src/main/java/com/example/pryanik/PryanikPlugin.java`
- World management: `src/main/java/com/example/pryanik/commands/WorldCommand.java`
- Plugin commands: `src/main/resources/plugin.yml`
- Main README: `README.md`
- User DB GUI: `players_viewer.py`
