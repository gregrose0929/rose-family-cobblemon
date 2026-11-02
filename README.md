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

**Press `M` in the game to open the map.** Pokémon towns and gyms are already marked on it. The land itself fills in as people explore.

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
