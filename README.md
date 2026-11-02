# Rose Family Server: Cobblemon setup

This sets up your game to play Cobblemon on the Rose Family Server. It takes about 5 minutes.

**You need:** a Windows PC and Minecraft: Java Edition, opened at least once.

## Install

1. **[Download the installer](https://github.com/gregrose0929/rose-family-cobblemon/releases/latest/download/Install-Cobblemon.bat)**.
2. Double-click `Install-Cobblemon.bat` in your Downloads folder.
   - If Windows says it protected your PC, click **More info**, then **Run anyway**.
3. When it says **All done!**, open the Minecraft Launcher.
4. Choose the **Cobblemon 1.21.1** profile and press **Play**.
5. Go to **Multiplayer** and join **Rose Family Server**.

The first launch takes a few minutes, because there's a lot to load.

**Press `M` in the game to open the map.** Pokémon towns and gyms are already marked on it. The land itself fills in as people explore.

## Good to know

- **Your other worlds and mods are safe.** Cobblemon gets its own game folder, `%APPDATA%\.minecraft-cobblemon`.
- **Updating:** if Greg says the server changed, download the installer again and run it. It only downloads what changed.
- **Can't join?** Your account has to be on the server's whitelist, so ask Greg to check.
- **Game running slowly?** The mods need a good amount of memory. Close other programs, or ask Greg for help.

## Removing it

1. In the Minecraft Launcher, go to **Installations** and delete **Cobblemon 1.21.1**.
2. Delete the folder `%APPDATA%\.minecraft-cobblemon`. Any single-player worlds you made in this profile are deleted with it.

## What it installs

The installer downloads each mod directly from [Modrinth](https://modrinth.com) and checks it against a saved checksum, so you get the exact files tested with the server. See [mods.json](mods.json) for the full list with versions and licenses. It also adds Fabric Loader 0.20.1 and a launcher profile.

No mods are stored in this repository. They belong to their authors, and each one keeps its own license.

## For Greg: releasing an update

1. Put the mods players need in a folder.
2. Run this, which regenerates `Install-Cobblemon.bat` and `mods.json`:

   ```bash
   python build_installer.py path/to/mods
   ```

3. Commit the changes and upload the new `Install-Cobblemon.bat` to a new GitHub release.
