# PryanikPlugin 1.0.30

- Website integration release for Pryanik Server home LAN setup.
- Website administrator role policy is CSRV-only; non-CSRV requests are redirected to profile with denial message.
- Administrator can assign ranks from the website. Website role changes are revalidated by the plugin before they are accepted.
- Adds website tables for account links, online state, bans, and rank-change requests.
- Tracks player join/quit status and updates online player activity.
- Applies website bans and processes website rank requests after checking the actor still has CSRV.

Note: the private IIS website bundle and its `.env` configuration are separate files and are not included in this plugin release.
