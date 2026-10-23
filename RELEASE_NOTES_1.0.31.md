# PryanikPlugin 1.0.31

- Fix online tracking: the normal player-join handler now sets `player_activity.is_online = TRUE` instead of marking the player offline.
- Quit handling continues to set online status to false; website homepage and CSRV admin dashboard read this shared PostgreSQL status.
- No website or secret configuration is included in this plugin build.
