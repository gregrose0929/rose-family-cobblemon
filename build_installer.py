"""Build dist/Rose-Family-Cobblemon-Setup.exe from Setup.cs. Usage: build_installer.py MOD_FOLDER VERSION

Looks up each client mod on Modrinth by its sha512, so the installer downloads the
exact file that was tested and verifies it against that hash.
"""
import hashlib
import json
import os
import struct
import subprocess
import sys
import urllib.request as u

MOD_FOLDER, VERSION = sys.argv[1:3]
HERE = os.path.dirname(os.path.abspath(__file__))
UA = {"User-Agent": "rose-family-cobblemon-installer"}
# Mods each player needs. Server-only mods stay off players' computers.
CLIENT_MODS = (
    "Cobblemon-fabric", "fabric-api", "Cobblemon-MegaShowdown", "mega", "owo-lib", "owo", "architectury",
    "accessories", "rctmod", "rctapi", "ForgeConfigAPIPort", "CobbleFurnies", "athena",
    "cobblemonraiddens", "geckolib", "cobblemon_battle_tower", "wild", "fightorflight", "followme",
    "CobbleDollars", "xaeroworldmap", "xaerominimap",
)
SERVER_ONLY = ("flan", "essential", "luckperms", "lithium", "ferritecore", "servercore", "spark", "daycareplus", "polymer", "pokedex", "Radical Gyms")


def nbt_string(value):
    data = value.encode()
    return struct.pack(">H", len(data)) + data


def servers_dat(name, address):
    entry = b"\x08" + nbt_string("name") + nbt_string(name) + b"\x08" + nbt_string("ip") + nbt_string(address) + b"\x00"
    return b"\x0a" + nbt_string("") + b"\x09" + nbt_string("servers") + b"\x0a" + struct.pack(">i", 1) + entry + b"\x00"


mods, skipped = [], []
for filename in sorted(os.listdir(MOD_FOLDER), key=str.lower):
    if not filename.endswith(".jar"):
        continue
    lowered = filename.lower()
    if any(lowered.startswith(s.lower()) for s in SERVER_ONLY) or not any(lowered.startswith(c.lower()) for c in CLIENT_MODS):
        skipped.append(filename)
        continue
    with open(os.path.join(MOD_FOLDER, filename), "rb") as handle:
        data = handle.read()
    sha512 = hashlib.sha512(data).hexdigest()
    version = json.load(u.urlopen(u.Request(f"https://api.modrinth.com/v2/version_file/{sha512}?algorithm=sha512", headers=UA)))
    project = json.load(u.urlopen(u.Request(f"https://api.modrinth.com/v2/project/{version['project_id']}", headers=UA)))
    match = [f for f in version["files"] if f["hashes"]["sha512"] == sha512][0]
    mods.append({
        "name": project["title"], "version": version["version_number"], "file": filename,
        "url": match["url"], "sha512": sha512.upper(), "mb": round(len(data) / 1e6, 1),
        "license": (project.get("license") or {}).get("id", "unknown"), "page": f"https://modrinth.com/mod/{project['slug']}",
    })

with open(os.path.join(HERE, "mods.json"), "w", encoding="utf-8") as handle:
    json.dump(mods, handle, indent=1)

dist = os.path.join(HERE, "dist")
os.makedirs(dist, exist_ok=True)
with open(os.path.join(dist, "servers.dat"), "wb") as handle:
    handle.write(servers_dat("Rose Family Server", "mc.greg.download"))
with open(os.path.join(dist, "Version.cs"), "w", encoding="ascii") as handle:
    handle.write(f'[assembly: System.Reflection.AssemblyVersion("{VERSION}.0")]\n[assembly: System.Reflection.AssemblyFileVersion("{VERSION}.0")]\n')
output = os.path.join(dist, "Rose-Family-Cobblemon-Setup.exe")
subprocess.run([
    os.path.join(os.environ["WINDIR"], "Microsoft.NET", "Framework64", "v4.0.30319", "csc.exe"),
    "/nologo", "/target:winexe", "/optimize+", f"/out:{output}",
    "/r:System.Windows.Forms.dll", "/r:System.Drawing.dll", "/r:System.Web.Extensions.dll", "/r:System.Management.dll", "/r:Microsoft.VisualBasic.dll",
    f"/win32icon:{os.path.join(HERE, 'icon.ico')}", f"/win32manifest:{os.path.join(HERE, 'Setup.manifest')}",
    f"/resource:{os.path.join(HERE, 'icon.ico')},icon.ico",
    f"/resource:{os.path.join(HERE, 'mods.json')},mods.json",
    f"/resource:{os.path.join(dist, 'servers.dat')},servers.dat",
    os.path.join(HERE, "Setup.cs"), os.path.join(dist, "Version.cs"),
], check=True)

for mod in mods:
    print(f"{mod['name']:32} {mod['version']:32} {mod['mb']:6} MB  {mod['license']}")
print(f"{len(mods)} client mods, {round(sum(m['mb'] for m in mods), 1)} MB total")
print("left off players' computers:", skipped)
print("built", output, hashlib.sha256(open(output, "rb").read()).hexdigest())
