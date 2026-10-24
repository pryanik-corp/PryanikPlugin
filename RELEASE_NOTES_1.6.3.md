# PryanikPlugin 1.6.3 — Paper 26.3

- Correct the website "load all griefs" action: now creates and loads all four worlds (overworld, nether, end, and separate spawn) for every configured instance of each enabled game mode (survival, hardcore, anarchy).
- Existing registered or unregistered world folders are preserved and loaded; non-empty invalid folders are never overwritten.
- The spawn template is installed only into an empty/missing `_spawn` folder through the existing safe template installer.
- Configure all loaded spawn worlds and register the created grief sets so normal game-mode entry can find them.
- Private website PHP changes are separate from this public plugin release.
