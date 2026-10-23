# PryanikPlugin 1.1.1 — Paper 26.3

- Fix CSRV portal setup for valid 4×5 and other supported Nether portal frames: the code previously accepted interior sizes up to 64×64, while Minecraft only creates portals with interior width 2–21 and height 3–21.
- Refuse frames that would place the top/bottom obsidian outside the world's build-height range.
- Validate supported interior dimensions before ignition and retain the complete obsidian-frame/interior checks.
- Built against Paper API 26.3 stable build 218, Java 25; WorldEdit 7.4.6 and WorldGuard 7.0.20.
