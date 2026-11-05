// Windows setup program for the Rose Family Server. build_installer.py compiles this with the C# 5
// compiler that ships with Windows, and embeds mods.json, servers.dat, and icon.ico.
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Management;
using System.Net;
using System.Reflection;
using System.Security.Cryptography;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Web.Script.Serialization;
using System.Windows.Forms;
using Microsoft.Win32;

[assembly: AssemblyTitle("Rose Family Server Cobblemon Setup")]
[assembly: AssemblyProduct("Rose Family Server Cobblemon")]
[assembly: AssemblyCompany("Rose Family")]

public sealed class Mod
{
    public string name, version, file, url, sha512;
    public double mb;
}

public static class Installer
{
    const string MinecraftVersion = "1.21.1";
    const string LoaderVersion = "0.20.1";
    const string VersionId = "fabric-loader-" + LoaderVersion + "-" + MinecraftVersion;
    const string ProfileId = "cobblemon-1.21.1";
    public const string ProfileName = "Cobblemon 1.21.1";
    public const string ServerName = "Rose Family Server";
    const string ServerAddress = "mc.greg.download";
    const string UninstallKey = @"Software\Microsoft\Windows\CurrentVersion\Uninstall\RoseFamilyCobblemon";
    const string UserAgent = "rose-family-cobblemon-installer";
    const string IconsFile = "rose-family-icons.jar"; // Built by make_phone_icons.py and embedded in this program.

    // name, initials, x, y, z, color, dimension folder. The y is a safe standing spot, which teleporting needs.
    static readonly object[][] Places =
    {
        new object[] { "Rose Town", "R", 324, 65, -178, 12, "dim%0" },
        new object[] { "Pallet Town", "P", -225, 65, -910, 12, "dim%0" },
        new object[] { "Lavender Town", "L", 320, 69, -1310, 13, "dim%0" },
        new object[] { "Old Spawn", "O", 0, 75, 0, 2, "dim%0" },
        new object[] { "Cerulean City", "C", 624, 63, 128, 9, "dim%0" },
        new object[] { "Vermilion City", "V", 576, 64, -480, 9, "dim%0" },
        new object[] { "Viridian City", "V", -752, 103, -816, 9, "dim%0" },
        new object[] { "Saffron City", "S", -320, 65, -1304, 9, "dim%0" },
        new object[] { "Cinnabar Island", "C", 632, 113, 1504, 9, "dim%0" },
        new object[] { "Fuchsia City", "F", 80, 62, -1904, 9, "dim%0" },
        new object[] { "Pewter City", "P", 1984, 100, -2784, 9, "dim%0" },
        new object[] { "Celadon City", "C", -2256, 69, -3216, 9, "dim%0" },
        new object[] { "Cerulean Gym", "G", 752, 63, 208, 14, "dim%0" },
        new object[] { "Vermilion Gym", "G", 144, 85, -456, 14, "dim%0" },
        new object[] { "Celadon Gym", "G", -872, 103, 88, 14, "dim%0" },
        new object[] { "Fuchsia Gym", "G", -832, 63, 840, 14, "dim%0" },
        new object[] { "Saffron Gym", "G", -1400, 71, -320, 14, "dim%0" },
        new object[] { "Pewter Gym", "G", 1328, 79, -2200, 14, "dim%0" },
        new object[] { "Cinnabar Gym", "G", -272, 86, -160, 14, "dim%-1" },
        new object[] { "Blackthorn Gym", "G", 1184, 43, -464, 14, "dim%1" },
        new object[] { "Kanto League", "L", -400, 34, 1904, 12, "dim%1" },
    };

    static readonly Encoding Utf8NoBom = new UTF8Encoding(false);
    static readonly JavaScriptSerializer Json = new JavaScriptSerializer { MaxJsonLength = int.MaxValue };
    public static readonly List<Mod> Mods = Json.Deserialize<List<Mod>>(Encoding.UTF8.GetString(Resource("mods.json")));

    static string AppData { get { return Environment.GetEnvironmentVariable("APPDATA"); } }
    static string MinecraftDir { get { return Path.Combine(AppData, ".minecraft"); } }
    static string GameDir { get { return Path.Combine(AppData, ".minecraft-cobblemon"); } }
    static string ModsDir { get { return Path.Combine(GameDir, "mods"); } }
    static string InstallDir { get { return Path.Combine(Environment.GetEnvironmentVariable("LOCALAPPDATA"), "Programs", "Rose Family Cobblemon"); } }

    public static byte[] Resource(string name)
    {
        using (var stream = Assembly.GetExecutingAssembly().GetManifestResourceStream(name))
        using (var copy = new MemoryStream())
        {
            stream.CopyTo(copy);
            return copy.ToArray();
        }
    }

    public static bool MinecraftFound() { return Directory.Exists(MinecraftDir); }

    public static bool AlreadyInstalled() { return Directory.Exists(ModsDir) && Directory.GetFiles(ModsDir, "*.jar").Length > 0; }

    public static bool MinecraftRunning()
    {
        if (Process.GetProcessesByName("Minecraft").Length > 0 || Process.GetProcessesByName("MinecraftLauncher").Length > 0)
            return true;
        using (var search = new ManagementObjectSearcher("SELECT CommandLine FROM Win32_Process WHERE Name='javaw.exe' OR Name='java.exe'"))
        {
            foreach (var process in search.Get())
            {
                var line = process["CommandLine"] as string;
                if (line != null && line.Contains(".minecraft"))
                    return true;
            }
        }
        return false;
    }

    public static void Install(Action<string> log, Action<double> progress, CancellationToken token)
    {
        InstallMods(log, progress, token);
        log("Setting up Fabric...");
        InstallFabric(token);
        log("Adding the launcher profile, server, and map markers...");
        SetLauncherProfile();
        AddServerToList();
        AddMapWaypoints();
        MoveSummaryKey();
        MoveMinimap();
        InstallBackpackDesigns(log, token);
        File.WriteAllBytes(Path.Combine(GameDir, "config", "cobblemon_smartphone.json"), Resource("phone-settings.json")); // The phone reads which apps to show from each PC.
        File.WriteAllBytes(Path.Combine(GameDir, "config", "cobblemonextendedbattleui.json"), Resource("battle-info-settings.json")); // Keeps battle info from covering the screen.
        RegisterUninstaller();
        progress(1);
        log("Finished.");
    }

    static void InstallMods(Action<string> log, Action<double> progress, CancellationToken token)
    {
        Directory.CreateDirectory(ModsDir);
        var wanted = Mods.Select(m => m.file).ToList();
        wanted.Add(IconsFile);
        File.WriteAllBytes(Path.Combine(ModsDir, IconsFile), Resource(IconsFile));
        foreach (var jar in Directory.GetFiles(ModsDir, "*.jar"))
        {
            if (!wanted.Contains(Path.GetFileName(jar)))
            {
                log("Removing old mod: " + Path.GetFileName(jar));
                File.Delete(jar);
            }
        }
        log("Checking what you already have...");
        var needed = new List<Mod>();
        foreach (var mod in Mods)
        {
            token.ThrowIfCancellationRequested();
            var target = Path.Combine(ModsDir, mod.file);
            if (File.Exists(target) && Sha512(target) == mod.sha512)
                log("Already installed: " + mod.name);
            else
                needed.Add(mod);
        }
        double total = Math.Max(needed.Sum(m => m.mb) * 1e6, 1);
        long received = 0;
        foreach (var mod in needed)
        {
            log(string.Format("Downloading {0} ({1} MB)", mod.name, mod.mb));
            var target = Path.Combine(ModsDir, mod.file);
            var temp = target + ".download";
            try
            {
                Download(mod.url, temp, token, bytes =>
                {
                    received += bytes;
                    progress(Math.Min(received / total, 1) * 0.98);
                });
                if (Sha512(temp) != mod.sha512)
                    throw new InvalidDataException(mod.name + " failed its safety check. Nothing was installed for this mod.");
                Replace(temp, target);
            }
            finally
            {
                if (File.Exists(temp))
                    File.Delete(temp);
            }
        }
    }

    static string Sha512(string path)
    {
        using (var hash = SHA512.Create())
        using (var stream = File.OpenRead(path))
            return BitConverter.ToString(hash.ComputeHash(stream)).Replace("-", "");
    }

    static void Download(string url, string path, CancellationToken token, Action<long> onBytes)
    {
        for (var attempt = 1; ; attempt++)
        {
            long written = 0;
            try
            {
                var request = (HttpWebRequest)WebRequest.Create(url);
                request.UserAgent = UserAgent;
                request.Timeout = 60000;
                request.ReadWriteTimeout = 60000;
                using (var response = request.GetResponse())
                using (var source = response.GetResponseStream())
                using (var file = File.Create(path))
                {
                    var buffer = new byte[81920];
                    int count;
                    while ((count = source.Read(buffer, 0, buffer.Length)) > 0)
                    {
                        token.ThrowIfCancellationRequested();
                        file.Write(buffer, 0, count);
                        written += count;
                        onBytes(count);
                    }
                }
                return;
            }
            catch (Exception error)
            {
                onBytes(-written);
                if (attempt == 3 || !(error is WebException || error is IOException))
                    throw;
            }
            Thread.Sleep(3000);
        }
    }

    static void Replace(string temp, string target)
    {
        if (File.Exists(target))
            File.Delete(target);
        File.Move(temp, target);
    }

    static void InstallFabric(CancellationToken token)
    {
        var folder = Path.Combine(MinecraftDir, "versions", VersionId);
        Directory.CreateDirectory(folder);
        var target = Path.Combine(folder, VersionId + ".json");
        var temp = target + ".download";
        Download(string.Format("https://meta.fabricmc.net/v2/versions/loader/{0}/{1}/profile/json", MinecraftVersion, LoaderVersion), temp, token, bytes => { });
        if (!File.ReadAllText(temp).Contains(VersionId))
            throw new InvalidDataException("Fabric's download didn't look right.");
        Replace(temp, target);
    }

    // The Microsoft Store launcher keeps its profiles in a different file, so update whichever ones exist.
    static List<string> ProfileFiles()
    {
        var files = new[] { "launcher_profiles.json", "launcher_profiles_microsoft_store.json" }.Select(name => Path.Combine(MinecraftDir, name));
        return files.Where(File.Exists).ToList();
    }

    static void WriteAtomically(string file, string text)
    {
        File.WriteAllText(file + ".new", text, Utf8NoBom);
        Replace(file + ".new", file);
    }

    static void SetLauncherProfile()
    {
        var files = ProfileFiles();
        if (files.Count == 0)
            files.Add(Path.Combine(MinecraftDir, "launcher_profiles.json"));
        var memoryGb = Math.Round(new Microsoft.VisualBasic.Devices.ComputerInfo().TotalPhysicalMemory / (double)(1L << 30));
        var heap = memoryGb >= 12 ? "-Xmx6G -Xms2G" : memoryGb >= 8 ? "-Xmx4G -Xms2G" : "-Xmx3G -Xms2G";
        var now = DateTime.UtcNow.ToString("yyyy-MM-ddTHH:mm:ss.000Z");
        foreach (var file in files)
        {
            var data = new Dictionary<string, object>();
            if (File.Exists(file))
            {
                if (!File.Exists(file + ".before-cobblemon"))
                    File.Copy(file, file + ".before-cobblemon");
                data = (Dictionary<string, object>)Json.DeserializeObject(File.ReadAllText(file));
            }
            if (!data.ContainsKey("profiles"))
                data["profiles"] = new Dictionary<string, object>();
            ((Dictionary<string, object>)data["profiles"])[ProfileId] = new Dictionary<string, object>
            {
                { "name", ProfileName }, { "type", "custom" }, { "created", now }, { "lastUsed", now }, { "icon", "Grass" },
                { "lastVersionId", VersionId }, { "gameDir", GameDir }, { "javaArgs", heap },
            };
            WriteAtomically(file, Json.Serialize(data));
        }
    }

    static void AddServerToList()
    {
        Directory.CreateDirectory(GameDir);
        var file = Path.Combine(GameDir, "servers.dat");
        if (File.Exists(file))
        {
            if (Encoding.ASCII.GetString(File.ReadAllBytes(file)).Contains(ServerAddress))
                return;
            File.Copy(file, file + ".old", true);
        }
        File.WriteAllBytes(file, Resource("servers.dat"));
    }

    // Places start hidden, so they only appear in the world once someone enables one from the map.
    static void AddMapWaypoints()
    {
        var root = Path.Combine(GameDir, "xaero", "minimap", "Multiplayer_" + ServerAddress);
        foreach (var group in Places.GroupBy(p => (string)p[6]))
        {
            var folder = Path.Combine(root, group.Key);
            Directory.CreateDirectory(folder);
            var file = Path.Combine(folder, "mw$default_1.txt");
            var text = File.Exists(file) ? File.ReadAllText(file)
                : "#\n#waypoint:name:initials:x:y:z:color:disabled:type:set:rotate_on_tp:tp_yaw:visibility_type:destination\n#\nsets:gui.xaero_default\n#\n";
            var lines = text.TrimEnd('\n').Split('\n').ToList();
            lines.RemoveAll(l => l.StartsWith("waypoint:Spawn:S:0:75:0:")); // Renamed to Old Spawn when Rose Town became the spawn point.
            foreach (var p in group)
            {
                var line = string.Format("waypoint:{0}:{1}:{2}:{3}:{4}:{5}:true:0:gui.xaero_default:false:0:0:false", p[0], p[1], p[2], p[3], p[4], p[5]);
                var existing = lines.FindIndex(l => l.StartsWith("waypoint:" + p[0] + ":"));
                if (existing < 0)
                    lines.Add(line);
                else if (lines[existing].Contains(":~:"))
                    lines[existing] = line; // From an older version, always visible with no height. Later choices are left alone.
            }
            File.WriteAllText(file, string.Join("\n", lines) + "\n", Utf8NoBom);
        }
        // The map has to list hidden places too, or nobody could pick one.
        var settings = Path.Combine(GameDir, "config", "xaero", "world-map", "client.cfg");
        Directory.CreateDirectory(Path.GetDirectoryName(settings));
        File.WriteAllText(settings, File.Exists(settings)
            ? File.ReadAllText(settings).Replace("display_disabled_waypoints = false", "display_disabled_waypoints = true")
            : "display_disabled_waypoints = true\n", Utf8NoBom);
    }

    // Cobblemon's Pokemon summary defaults to M, which the map uses. K, used by an earlier version, opens the phone.
    static void MoveSummaryKey()
    {
        const string binding = "key_key.cobblemon.summary:key.keyboard.";
        var file = Path.Combine(GameDir, "options.txt");
        // A new file needs the data version, or the game discards the key setting when it first loads.
        var text = File.Exists(file) ? File.ReadAllText(file) : "version:3955\n";
        if (text.Contains(binding + "m") || text.Contains(binding + "k"))
            text = text.Replace(binding + "m", binding + "j").Replace(binding + "k", binding + "j");
        else if (!text.Contains(binding))
            text += binding + "j\n";
        text = ChangeDefaultKey(text, "key_key.companion_bonds.open_contest_tracker:key.keyboard.", "k", "i"); // K opens the phone.
        text = ChangeDefaultKey(text, "key_key.companion_bonds.pet_interaction:key.keyboard.", "unknown", "v"); // Unbound by default.
        text = ChangeDefaultKey(text, "key_key.ridetraining.toggle_quest_hud:key.keyboard.", "j", "unknown"); // J shows Pokemon details.
        text = ChangeDefaultKey(text, "key_key.ridetraining.move_quest_hud:key.keyboard.", "k", "unknown"); // K opens the phone.
        File.WriteAllText(file, text, Utf8NoBom);
    }

    // These designs are downloaded from their own page and turned on for the player.
    const string BackpackDesignsFile = "TB Textures Cobblemon-fabric-1.5.zip";
    const string BackpackDesignsUrl = "https://cdn.modrinth.com/data/YvzBlCGS/versions/gokAEeae/TB%20Textures%20Cobblemon-fabric-1.5.zip";
    const string BackpackDesignsSha512 = "406E0C80842C19F6106DAA5E00E6106872FEA423A9EE40081E4FB7B1F6ACB74A39E909CC1BAEFD1E09C079061B660266D3A6FED71B12CC6E3D8959487EAC7971";

    static void InstallBackpackDesigns(Action<string> log, CancellationToken token)
    {
        var folder = Path.Combine(GameDir, "resourcepacks");
        Directory.CreateDirectory(folder);
        var target = Path.Combine(folder, BackpackDesignsFile);
        if (!File.Exists(target) || Sha512(target) != BackpackDesignsSha512)
        {
            log("Downloading backpack designs");
            var temp = target + ".download";
            try
            {
                Download(BackpackDesignsUrl, temp, token, bytes => { });
                if (Sha512(temp) != BackpackDesignsSha512)
                    throw new InvalidDataException("The backpack designs failed their safety check.");
                Replace(temp, target);
            }
            finally
            {
                if (File.Exists(temp))
                    File.Delete(temp);
            }
        }
        var file = Path.Combine(GameDir, "options.txt");
        var entry = "\"file/" + BackpackDesignsFile + "\"";
        var lines = File.ReadAllLines(file).ToList();
        var index = lines.FindIndex(line => line.StartsWith("resourcePacks:"));
        if (index < 0)
            lines.Add("resourcePacks:[\"fabric\"," + entry + "]");
        else if (!lines[index].Contains(entry))
            lines[index] = lines[index].TrimEnd(']') + "," + entry + "]";
        File.WriteAllLines(file, lines, Utf8NoBom);
    }

    // The team list sits in the top left, so the minimap goes in the top right. It hides itself during battles.
    static void MoveMinimap()
    {
        const string module = "module;id=xaerominimap:minimap;";
        var file = Path.Combine(GameDir, "config", "xaerohud.txt");
        var text = File.Exists(file) ? File.ReadAllText(file) : "";
        if (text.Contains(module))
            text = string.Join("\n", text.Split('\n').Select(line => line.StartsWith(module) ? line.Replace("fromRight=false", "fromRight=true").Replace(";y=52;", ";y=0;") : line));
        else
            text += module + "x=0;y=0;centered=false;fromRight=true;fromBottom=false;flippedVer=false;flippedHor=false;\n";
        Directory.CreateDirectory(Path.GetDirectoryName(file));
        File.WriteAllText(file, text, Utf8NoBom);
    }

    // Leaves a key alone if the player already chose their own.
    static string ChangeDefaultKey(string text, string binding, string defaultKey, string newKey)
    {
        if (text.Contains(binding + defaultKey))
            return text.Replace(binding + defaultKey, binding + newKey);
        return text.Contains(binding) ? text : text + binding + newKey + "\n";
    }

    // Lists the game under Settings > Apps, using a copy of this program as the uninstaller.
    static void RegisterUninstaller()
    {
        var me = Assembly.GetExecutingAssembly().Location;
        var copy = Path.Combine(InstallDir, "Rose Family Cobblemon Setup.exe");
        Directory.CreateDirectory(InstallDir);
        if (!string.Equals(me, copy, StringComparison.OrdinalIgnoreCase))
        {
            try
            {
                File.Copy(me, copy, true);
            }
            catch (IOException)
            {
                // Antivirus can hold the file briefly. An older uninstaller still works, so only a missing one is a problem.
                if (!File.Exists(copy))
                    throw;
            }
        }
        using (var key = Registry.CurrentUser.CreateSubKey(UninstallKey))
        {
            key.SetValue("DisplayName", "Rose Family Server: Cobblemon");
            key.SetValue("DisplayVersion", Assembly.GetExecutingAssembly().GetName().Version.ToString(3));
            key.SetValue("Publisher", "Rose Family");
            key.SetValue("DisplayIcon", copy);
            key.SetValue("InstallLocation", GameDir);
            key.SetValue("UninstallString", "\"" + copy + "\" --uninstall");
            key.SetValue("NoModify", 1);
            key.SetValue("NoRepair", 1);
            key.SetValue("EstimatedSize", (int)(Mods.Sum(m => m.mb) * 1000));
        }
    }

    public static int WorldCount()
    {
        var saves = Path.Combine(GameDir, "saves");
        return Directory.Exists(saves) ? Directory.GetDirectories(saves).Length : 0;
    }

    public static void Uninstall(bool keepWorlds)
    {
        foreach (var file in ProfileFiles())
        {
            var data = (Dictionary<string, object>)Json.DeserializeObject(File.ReadAllText(file));
            object profiles;
            if (data.TryGetValue("profiles", out profiles) && ((Dictionary<string, object>)profiles).Remove(ProfileId))
                WriteAtomically(file, Json.Serialize(data));
        }
        if (Directory.Exists(GameDir))
        {
            var kept = keepWorlds ? new[] { "saves", "screenshots" } : new string[0];
            foreach (var folder in Directory.GetDirectories(GameDir).Where(f => !kept.Contains(Path.GetFileName(f))))
                Directory.Delete(folder, true);
            foreach (var file in Directory.GetFiles(GameDir))
                File.Delete(file);
            if (Directory.GetFileSystemEntries(GameDir).Length == 0)
                Directory.Delete(GameDir);
        }
        Registry.CurrentUser.DeleteSubKeyTree(UninstallKey, false);
        if (!Directory.Exists(InstallDir))
            return;
        var me = Assembly.GetExecutingAssembly().Location;
        if (me.StartsWith(InstallDir + "\\", StringComparison.OrdinalIgnoreCase))
        {
            // A running program can't delete itself, so remove the folder just after this one exits.
            var cleanup = string.Format("/c ping -n 3 127.0.0.1 >nul & rmdir /s /q \"{0}\"", InstallDir);
            Process.Start(new ProcessStartInfo("cmd.exe", cleanup) { CreateNoWindow = true, UseShellExecute = false, WorkingDirectory = Path.GetTempPath() });
        }
        else
        {
            try
            {
                Directory.Delete(InstallDir, true);
            }
            catch (IOException)
            {
                // Antivirus can hold the file briefly. Everything that matters is already gone, and a leftover copy does no harm.
            }
        }
    }
}

public sealed class SetupForm : Form
{
    static readonly Color Red = Color.FromArgb(200, 40, 40);
    static readonly float DisplayScale = GetDisplayScale();
    readonly Panel body = new Panel { Dock = DockStyle.Fill, Padding = new Padding(S(24), S(18), S(24), S(12)) };
    readonly FlowLayoutPanel footer = new FlowLayoutPanel
    {
        Dock = DockStyle.Bottom, Height = S(54), FlowDirection = FlowDirection.RightToLeft, Padding = new Padding(S(10)), BackColor = Color.FromArgb(240, 240, 240),
    };
    readonly CancellationTokenSource cancel = new CancellationTokenSource();
    bool busy, closeWhenStopped;

    static float GetDisplayScale()
    {
        using (var graphics = Graphics.FromHwnd(IntPtr.Zero))
            return graphics.DpiX / 96F;
    }

    // Fonts follow Windows' display scaling on their own, so sizes in pixels have to follow it too.
    static int S(int pixels) { return (int)Math.Round(pixels * DisplayScale); }

    public SetupForm()
    {
        AutoScaleMode = AutoScaleMode.None;
        Text = "Rose Family Server Setup";
        Font = new Font("Segoe UI", 9.75F);
        ClientSize = new Size(S(580), S(420));
        FormBorderStyle = FormBorderStyle.FixedSingle;
        MaximizeBox = false;
        StartPosition = FormStartPosition.CenterScreen;
        BackColor = Color.White;
        Icon = LoadIcon(32);

        var header = new Panel { Dock = DockStyle.Top, Height = S(78), BackColor = Red };
        header.Controls.Add(new Label
        {
            Dock = DockStyle.Fill, ForeColor = Color.White, TextAlign = ContentAlignment.MiddleLeft, Padding = new Padding(S(4), 0, 0, 0),
            Font = new Font("Segoe UI Semibold", 15F), Text = "Rose Family Server\nCobblemon Setup",
        });
        header.Controls.Add(new PictureBox { Dock = DockStyle.Left, Width = S(78), SizeMode = PictureBoxSizeMode.CenterImage, Image = new Bitmap(LoadIcon(48).ToBitmap(), S(48), S(48)) });

        Controls.Add(body);
        Controls.Add(footer);
        Controls.Add(header);
        FormClosing += OnClosing;

        if (Installer.MinecraftFound())
            ShowWelcome();
        else
            ShowPage("Minecraft: Java Edition wasn't found on this computer.\n\nInstall it, open it once, close it, and run this setup again.", MakeButton("Close", Close));
    }

    static Icon LoadIcon(int size)
    {
        using (var stream = new MemoryStream(Installer.Resource("icon.ico")))
            return new Icon(stream, size, size);
    }

    Button MakeButton(string text, Action onClick)
    {
        var button = new Button { Text = text, AutoSize = true, MinimumSize = new Size(S(96), S(32)), UseVisualStyleBackColor = true };
        button.Click += (sender, e) => onClick();
        return button;
    }

    void ShowPage(string text, params Button[] buttons)
    {
        body.Controls.Clear();
        body.Controls.Add(new Label { Dock = DockStyle.Fill, Text = text });
        SetButtons(buttons);
    }

    void SetButtons(params Button[] buttons)
    {
        footer.Controls.Clear();
        footer.Controls.AddRange(buttons);
        if (buttons.Length > 0)
            AcceptButton = buttons[0];
    }

    void ShowWelcome()
    {
        var update = Installer.AlreadyInstalled();
        var size = Math.Round(Installer.Mods.Sum(m => m.mb));
        var start = update ? "You already have Cobblemon set up. This brings it up to date with the server, and only downloads what changed."
            : string.Format("This sets up your game to play Cobblemon on the Rose Family Server. It downloads about {0} MB and takes a few minutes.", size);
        ShowPage(start + "\n\n"
            + "What it adds:\n"
            + string.Format("  •  {0} mods, checked against the exact files tested with the server\n", Installer.Mods.Count)
            + string.Format("  •  A \"{0}\" profile in the Minecraft Launcher\n", Installer.ProfileName)
            + "  •  The server in your multiplayer list\n"
            + "  •  Pokémon towns and gyms on the map, ready to teleport to\n\n"
            + "Your other Minecraft worlds and mods aren't changed. You can remove this any time from Settings > Apps.",
            MakeButton(update ? "Update" : "Install", StartInstall), MakeButton("Cancel", Close));
    }

    async void StartInstall()
    {
        var status = new Label { Dock = DockStyle.Top, Height = S(28), Text = "Getting ready..." };
        var bar = new ProgressBar { Dock = DockStyle.Top, Height = S(20), Maximum = 1000 };
        var details = new TextBox { Dock = DockStyle.Fill, Multiline = true, ReadOnly = true, ScrollBars = ScrollBars.Vertical, BackColor = Color.White };
        body.Controls.Clear();
        body.Controls.Add(details);
        body.Controls.Add(new Label { Dock = DockStyle.Top, Height = S(12) });
        body.Controls.Add(bar);
        body.Controls.Add(status);
        SetButtons(MakeButton("Cancel", Close));

        Action<string> log = line => BeginInvoke((Action)(() =>
        {
            status.Text = line;
            details.AppendText(line + Environment.NewLine);
        }));
        Action<double> progress = value => BeginInvoke((Action)(() => bar.Value = (int)(value * 1000)));

        busy = true;
        string failure = null;
        try
        {
            while (await Task.Run(() => Installer.MinecraftRunning()))
            {
                status.Text = "Please close Minecraft and the Minecraft Launcher. This continues by itself.";
                await Task.Delay(2000);
                cancel.Token.ThrowIfCancellationRequested();
            }
            await Task.Run(() => Installer.Install(log, progress, cancel.Token));
        }
        catch (OperationCanceledException) { failure = "Cancelled"; }
        catch (Exception error) { failure = error.Message; }
        busy = false;

        if (closeWhenStopped)
            Close();
        else if (failure != null)
            ShowPage("Something went wrong:\n\n" + failure + "\n\nNothing is broken. Check your internet connection and try again. If it keeps happening, send a photo of this window to Greg.",
                MakeButton("Try again", StartInstall), MakeButton("Close", Close));
        else
            ShowPage("All done!\n\n"
                + "1.  Open the Minecraft Launcher.\n"
                + string.Format("2.  Choose the \"{0}\" profile and press Play.\n", Installer.ProfileName)
                + string.Format("3.  Go to Multiplayer and join \"{0}\".\n", Installer.ServerName)
                + "4.  Press K to open your phone. It changes your team, opens the shop, and more.\n"
                + "5.  Press M for the map, and right-click any town to teleport there.\n\n"
                + "The first launch takes a few minutes, because there's a lot to load.",
                MakeButton("Finish", Close));
    }

    void OnClosing(object sender, FormClosingEventArgs e)
    {
        if (!busy)
            return;
        e.Cancel = true;
        if (closeWhenStopped)
            return;
        if (MessageBox.Show(this, "Stop setting up? You can run this again any time to finish.", Text, MessageBoxButtons.YesNo, MessageBoxIcon.Question, MessageBoxDefaultButton.Button2) == DialogResult.Yes)
        {
            closeWhenStopped = true;
            cancel.Cancel();
        }
    }
}

public static class Program
{
    const string Title = "Rose Family Server Setup";

    static void RunUninstall()
    {
        if (Installer.MinecraftRunning())
        {
            MessageBox.Show("Please close Minecraft and the Minecraft Launcher, then try again.", Title, MessageBoxButtons.OK, MessageBoxIcon.Information);
            return;
        }
        if (MessageBox.Show("Remove Cobblemon and the Rose Family Server setup from this computer?", Title, MessageBoxButtons.YesNo, MessageBoxIcon.Question, MessageBoxDefaultButton.Button2) != DialogResult.Yes)
            return;
        var keepWorlds = true;
        var worlds = Installer.WorldCount();
        if (worlds > 0)
            keepWorlds = MessageBox.Show(string.Format("You have {0} single-player world(s) in this profile. Keep them and your screenshots?\n\nThe server's world is safe either way.", worlds),
                Title, MessageBoxButtons.YesNo, MessageBoxIcon.Question) == DialogResult.Yes;
        try
        {
            Installer.Uninstall(keepWorlds);
            MessageBox.Show("Cobblemon has been removed.", Title, MessageBoxButtons.OK, MessageBoxIcon.Information);
        }
        catch (Exception error)
        {
            MessageBox.Show("Something went wrong:\n\n" + error.Message, Title, MessageBoxButtons.OK, MessageBoxIcon.Error);
        }
    }

    [STAThread]
    static void Main(string[] args)
    {
        ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls12;
        Application.EnableVisualStyles();
        Application.SetCompatibleTextRenderingDefault(false);
        if (args.Contains("--uninstall"))
            RunUninstall();
        else
            Application.Run(new SetupForm());
    }
}
