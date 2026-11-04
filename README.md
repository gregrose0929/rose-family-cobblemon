# Rose Family Server: Cobblemon setup

This sets up your game to play Cobblemon on the Rose Family Server. It takes about 5 minutes.

**You need:** a Windows PC and Minecraft: Java Edition, opened at least once.

## Install

1. **[Download the setup program](https://github.com/gregrose0929/rose-family-cobblemon/releases/latest/download/Rose-Family-Cobblemon-Setup.exe)**.
2. Open `Rose-Family-Cobblemon-Setup.exe` from your Downloads folder.
   - If Windows says it protected your PC, click **More info**, then **Run anyway**. See [why Windows warns](#why-windows-warns).
3. Click **Install**, and wait for **All done!**
4. Open the Minecraft Launcher, choose the **Cobblemon 1.21.1** profile, and press **Play**.
5. Go to **Multiplayer** and join **Rose Family Server**.

The first launch takes a few minutes, because there's a lot to load.

**Press `M` in the game to open the map.** Pokémon towns and gyms are already on it, and the land fills in as people explore. Right-click any of them:

- **Teleport to Waypoint** takes you there right away.
- **Enable Waypoint** shows it around you as you travel. Choose **Disable Waypoint** when you're done, so your screen stays clear.

**New here?** You'll start in Rose Town with a red phone, a Trainer's Guide book, Poké Balls, and potions. Follow the signs along the Trainer's Trail.

**Press `K` to open your phone.** Use it to change your team in the PC, open the Pokédex and the shop, look up Pokémon, message family, go back to Rose Town or your home, see the time and what spawns nearby, check the Rose Cup, start hunts, and open the guide. If you lose it, a new one appears in your backpack.

**To heal your team, visit a Pokémon Center.** Every town has one with Healing Machines to right-click, and Nurse Joy works at the one in Rose Town. Potions and Revives work anywhere.

**You can fly!** The server is survival, but everyone can fly. Tap the jump key twice to take off. Falling can still hurt.

Press `J` to see your Pokémon's details. When a Rose Cup tournament opens, click the message in chat to join.

## Good to know

- **Your other worlds and mods are safe.** Cobblemon gets its own game folder, `%APPDATA%\.minecraft-cobblemon`.
- **It doesn't need administrator access.** It only changes your own Minecraft files.
- **Updating:** if Greg says the server changed, download the setup program again and run it. It only downloads what changed.
- **Can't join?** Your account has to be on the server's whitelist, so ask Greg to check.
- **Game running slowly?** The mods need a good amount of memory. Close other programs, or ask Greg for help.

## Removing it

Go to **Settings > Apps > Installed apps**, find **Rose Family Server: Cobblemon**, and choose **Uninstall**. It asks before deleting any single-player worlds you made in this profile.

If you installed before the setup program existed, run it once first so it shows up there.

## Why Windows warns

Windows warns about programs from publishers it doesn't recognize. Removing the warning takes a paid code signing certificate, which this family project doesn't have, so the warning will say **Unknown publisher**.

You can check what you're running:

- All of the setup program's code is in [Setup.cs](Setup.cs), and each release lists the SHA-256 checksum of the exact file.
- It downloads each mod directly from [Modrinth](https://modrinth.com) and checks it against a saved checksum, so you get the exact files tested with the server. See [mods.json](mods.json) for the full list with versions and licenses.
- It also downloads Fabric Loader 0.20.1 from [Fabric](https://fabricmc.net) and adds a launcher profile.

No mods are stored in this repository. They belong to their authors, and each one keeps its own license.

## For Greg: releasing an update

1. Put the mods players need in a folder.
2. Run this, which regenerates `mods.json` and builds `dist/Rose-Family-Cobblemon-Setup.exe`. It uses the C# compiler built into Windows, so there's nothing else to install:

   ```bash
   python build_installer.py path/to/mods 1.3.0
   ```

3. Commit the changes and upload the new `.exe` to a new GitHub release, with its SHA-256 in the notes.

**New in 1.9:** trainer outfits and Pokémon hats (craft a Tailoring Station, or look in the crafting book), and Pokémon helpers that work from a Pasture block.

**New in 1.10:** hold `V` while looking at your Pokémon to pet it. Press `I` to see friendship, ribbons, and Contest progress.

**New in 1.12:** a Daily Gift on your phone, and Rose Town's people have their own looks.

**New in 1.13:** every app has its name on it, PokeNav shows who lives nearby, the minimap moved to the top right (turn it off with the Minimap app), hints tell you when you can right-click something, and the Shop's Sell button buys your spare items.

**New in 1.14:** walk during battles without pressing R, and right-clicking with a potion always opens your team (sneak to place it as a decoration).
