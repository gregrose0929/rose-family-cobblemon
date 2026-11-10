"""Draw the phone's extra app icons and package them as a small resource-only mod. Usage: make_phone_icons.py OUTPUT.jar"""
import json
import math
import os
import glob
import re
import struct
import sys
import zipfile
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import townspeople

SIZE = 36
WHITE = (255, 255, 255, 255)
DARK = (35, 35, 40, 255)
RED = (214, 52, 52, 255)


def read_png(data):
    """Reads an 8-bit RGBA or palette PNG."""
    at, idat, palette, alpha = 8, b"", b"", b""
    while at < len(data):
        length = struct.unpack(">I", data[at:at + 4])[0]
        kind, body = data[at + 4:at + 8], data[at + 8:at + 8 + length]
        if kind == b"IHDR":
            width, height, depth, color = struct.unpack(">IIBB", body[:10])
            assert depth == 8 and color in (3, 6), (depth, color)
            step = 4 if color == 6 else 1
        elif kind == b"IDAT":
            idat += body
        elif kind == b"PLTE":
            palette = body
        elif kind == b"tRNS":
            alpha = body
        at += 12 + length
    raw, rows, previous, at = zlib.decompress(idat), [], bytearray(width * step), 0
    for _ in range(height):
        kind = raw[at]
        line = bytearray(raw[at + 1:at + 1 + width * step])
        at += 1 + width * step
        for i in range(width * step):
            a = line[i - step] if i >= step else 0
            b = previous[i]
            c = previous[i - step] if i >= step else 0
            if kind == 1:
                line[i] = (line[i] + a) & 255
            elif kind == 2:
                line[i] = (line[i] + b) & 255
            elif kind == 3:
                line[i] = (line[i] + (a + b) // 2) & 255
            elif kind == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[i] = (line[i] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        if color == 6:
            rows.append([tuple(line[x * 4:x * 4 + 4]) for x in range(width)])
        else:
            rows.append([tuple(palette[i * 3:i * 3 + 3]) + (alpha[i] if i < len(alpha) else 255,) for i in line])
        previous = line
    return width, rows


def png(pixels, size=SIZE):
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    rows = b"".join(b"\x00" + bytes(c for x in range(size) for c in pixels[y][x]) for y in range(size))
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(rows)) + chunk(b"IEND", b"")


# A tiny 3x5 alphabet, so an app's name is on the icon itself.
FONT = {"A": "010101111101101", "B": "110101110101110", "C": "011100100100011", "D": "110101101101110", "E": "111100110100111", "F": "111100110100100", "G": "011100101101011",
        "H": "101101111101101", "I": "111010010010111", "J": "001001001101010", "K": "101101110101101", "L": "100100100100111", "M": "101111111101101", "N": "110101101101101",
        "O": "010101101101010", "P": "110101110100100", "Q": "010101101111011", "R": "110101110101101", "S": "011100010001110", "T": "111010010010010", "U": "101101101101111",
        "V": "101101101101010", "W": "101101111111101", "X": "101101010101101", "Y": "101101010010010", "Z": "111001010100111", " ": "000000000000000"}


def caption(pixels, name):
    # A dark strip across the bottom, inside the icon's rounded corners, with the name in white.
    for y in range(27, 36):
        for x in range(SIZE):
            if pixels[y][x][3]:
                pixels[y][x] = tuple(int(c * 0.35) for c in pixels[y][x][:3]) + (255,)
    width = len(name) * 4 - 1
    left = (SIZE - width) // 2
    for i, letter in enumerate(name.upper()):
        bits = FONT[letter]
        for row in range(5):
            for col in range(3):
                if bits[row * 3 + col] == "1":
                    pixels[29 + row][left + i * 4 + col] = WHITE


def icon(background, draw, hover, name):
    color = tuple(min(255, c + 45) for c in background[:3]) + (255,) if hover else background
    pixels = [[(0, 0, 0, 0)] * SIZE for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            # Rounded square, like the phone's own buttons.
            dx, dy = max(6 - x, x - 29, 0), max(6 - y, y - 29, 0)
            if math.hypot(dx, dy) <= 6:
                pixels[y][x] = color

    def put(x, y, value=WHITE):
        y -= 4  # leaves room for the name underneath
        if 0 <= x < SIZE and 0 <= y < SIZE:
            pixels[y][x] = value

    def rect(x1, y1, x2, y2, value=WHITE):
        for y in range(y1, y2 + 1):
            for x in range(x1, x2 + 1):
                put(x, y, value)

    draw(put, rect)
    caption(pixels, name)
    return png(pixels)


def town(put, rect):
    # A house with a door.
    for i in range(10):
        rect(17 - i, 8 + i, 18 + i, 8 + i)
    rect(10, 18, 25, 27)
    rect(16, 21, 19, 27, (160, 60, 80, 255))


def hunts(put, rect):
    # A target.
    for y in range(36):
        for x in range(36):
            d = math.hypot(x - 17.5, y - 17.5)
            if 9.5 <= d <= 12 or d <= 3:
                put(x, y)
    rect(17, 4, 18, 9)
    rect(17, 26, 18, 31)
    rect(4, 17, 9, 18)
    rect(26, 17, 31, 18)


def balls(put, rect):
    # A Poke Ball.
    for y in range(36):
        for x in range(36):
            d = math.hypot(x - 17.5, y - 17.5)
            if d <= 12:
                put(x, y, RED if y < 17 else WHITE)
            if d <= 12 and (d >= 11 or 16.5 <= y <= 18.5):
                put(x, y, DARK)
            if d <= 4.5:
                put(x, y, DARK)
            if d <= 2.5:
                put(x, y, WHITE)


def dex(put, rect):
    # An open book.
    rect(7, 9, 16, 26)
    rect(19, 9, 28, 26)
    for y in (13, 17, 21):
        rect(9, y, 14, y, (100, 150, 110, 255))
        rect(21, y, 26, y, (100, 150, 110, 255))


def guide(put, rect):
    # A question mark.
    rect(13, 8, 22, 10)
    rect(11, 10, 14, 14)
    rect(21, 10, 24, 16)
    rect(17, 16, 22, 18)
    rect(16, 18, 19, 22)
    rect(16, 25, 19, 28)


def today(put, rect):
    # A sun.
    for y in range(36):
        for x in range(36):
            d = math.hypot(x - 17.5, y - 17.5)
            if d <= 6:
                put(x, y, (255, 214, 80, 255))
    for a in range(8):
        for r in range(8, 13):
            put(int(17.5 + r * math.cos(a * math.pi / 4)), int(17.5 + r * math.sin(a * math.pi / 4)), (255, 214, 80, 255))


def cup(put, rect):
    # A trophy.
    gold = (255, 214, 80, 255)
    rect(12, 8, 23, 17, gold)
    rect(8, 9, 10, 14, gold)
    rect(25, 9, 27, 14, gold)
    rect(16, 18, 19, 23, gold)
    rect(12, 24, 23, 27, gold)


def go_home(put, rect):
    # A house with an arrow going in.
    rect(9, 15, 26, 27)
    for i in range(9):
        rect(17 - i, 7 + i, 18 + i, 7 + i)
    rect(15, 20, 20, 27, (60, 90, 110, 255))


def set_home(put, rect):
    # A flag.
    rect(11, 7, 13, 28)
    for i in range(8):
        rect(14, 8 + i, 27 - i, 8 + i, (240, 80, 90, 255))


def go_back(put, rect):
    # An arrow pointing left.
    for i in range(8):
        rect(8 + i, 17 - i, 9 + i, 18 + i)
    rect(15, 15, 27, 20)


def spawns(put, rect):
    # A paw print.
    for cx, cy, r in ((17.5, 22, 6), (9.5, 14, 3), (15, 10, 3), (21, 10, 3), (26, 14, 3)):
        for y in range(36):
            for x in range(36):
                if math.hypot(x - cx, y - cy) <= r:
                    put(x, y)


def travel(put, rect):
    # A map pin.
    pin = (235, 70, 70, 255)
    for y in range(36):
        for x in range(36):
            if math.hypot(x - 17.5, y - 14) <= 8:
                put(x, y, pin)
    for i in range(10):
        rect(17 - (7 - i * 7 // 10), 20 + i, 18 + (7 - i * 7 // 10), 20 + i, pin)
    for y in range(36):
        for x in range(36):
            if math.hypot(x - 17.5, y - 14) <= 3:
                put(x, y)


def minimap(put, rect):
    # A small folded map.
    rect(8, 10, 27, 27, (235, 225, 190, 255))
    rect(14, 10, 15, 27, (200, 190, 150, 255))
    rect(21, 10, 22, 27, (200, 190, 150, 255))
    rect(10, 14, 19, 15, (90, 150, 90, 255))
    rect(17, 20, 25, 21, (80, 130, 200, 255))
    rect(23, 12, 25, 14, RED)


def story(put, rect):
    # A book with a star on the cover.
    rect(9, 8, 26, 28, (150, 90, 50, 255))
    rect(11, 9, 26, 27, (235, 225, 195, 255))
    rect(9, 8, 10, 28, (110, 65, 35, 255))
    star = (255, 205, 60, 255)
    for x, y in ((18, 12), (17, 13), (18, 13), (19, 13), (14, 15), (15, 15), (16, 15), (17, 15), (18, 15), (19, 15), (20, 15), (21, 15), (22, 15),
                 (15, 16), (16, 16), (17, 16), (18, 16), (19, 16), (20, 16), (21, 16), (16, 17), (17, 17), (18, 17), (19, 17), (20, 17),
                 (16, 18), (17, 18), (19, 18), (20, 18), (15, 19), (16, 19), (20, 19), (21, 19)):
        put(x, y, star)
    rect(13, 22, 23, 22, (150, 90, 50, 255))
    rect(13, 24, 21, 24, (150, 90, 50, 255))


def camera(put, rect):
    # A camera.
    rect(7, 13, 28, 27, (235, 235, 240, 255))
    rect(12, 9, 19, 12, (235, 235, 240, 255))
    for y in range(36):
        for x in range(36):
            d = math.hypot(x - 17.5, y - 20)
            if d <= 5:
                put(x, y, DARK)
            if d <= 2.5:
                put(x, y, (90, 150, 220, 255))
    rect(24, 15, 26, 16, RED)


def gift(put, rect):
    # A wrapped present.
    box, ribbon = (240, 240, 240, 255), (235, 70, 70, 255)
    rect(8, 15, 27, 28, box)
    rect(7, 11, 28, 15, box)
    rect(16, 11, 19, 28, ribbon)
    rect(7, 13, 28, 14, ribbon)
    rect(11, 7, 15, 10, ribbon)
    rect(20, 7, 24, 10, ribbon)


ICONS = {"travel": ("Travel", (40, 140, 140, 255), travel), "gift": ("Gift", (170, 60, 130, 255), gift), "hunts": ("Hunts", (205, 110, 30, 255), hunts), 
         "dex": ("Family", (50, 130, 80, 255), dex), "today": ("Today", (60, 130, 190, 255), today), "cup": ("Cup", (150, 40, 60, 255), cup), "home": ("Home", (70, 120, 140, 255), go_home),
         "story": ("Story", (60, 120, 70, 255), story), "camera": ("Camera", (90, 80, 150, 255), camera), "minimap": ("Minimap", (70, 90, 80, 255), minimap), "guide": ("Guide", (120, 70, 170, 255), guide)}
# Long names for when you hover over an app.
NAMES = {"travel": "Travel: fly to any city or gym", "gift": "Daily Gift", "hunts": "Hunts: catching missions", "dex": "Family Pok\u00e9dex", "today": "Today: time and weather",
         "cup": "Rose Cup", "home": "Home: go home, set home, or go back", "story": "Story: your next goal", "camera": "Camera: how battles are filmed", "minimap": "Minimap: on or off", "guide": "Trainer's Guide", "pc": "PC: change your team", "cloud": "Item Bag (your Ender Chest)", "cobbledollars_shop": "Shop: buy and sell",
         "rct_trainer_card": "Trainer Card", "pokedex": "Pok\u00e9dex", "pokeinfo": "Pok\u00e9mon Info", "cobblenav_pokenav": "PokeNav: who lives nearby", "crafting": "Crafting Table", "social": "Messages"}
# The phone's own icons get names too.
NATIVE_LABELS = {"pc": "PC", "cloud": "Bag", "cobbledollars": "Shop", "pokedex": "Pokedex", "pokeinfo": "Info", "pokenav": "PokeNav", "crafting": "Craft", "social": "Message", "trainer": "Card", "trainer_card": "Card", "rct_trainer_card": "Card", "rct": "Card", "trainercard": "Card", "fishnav": "FishNav"}
MOD = {"schemaVersion": 1, "id": "rose_family_icons", "version": "1.14.0", "name": "Rose Family Icons", "description": "Icons for the Rose Family Server's phone apps, plus wording for server-only mods.", "environment": "*", "entrypoints": {"main": ["rose.fixes.RoseFixes"], "client": ["rose.fixes.RoseClient"]}, "mixins": [{"config": "rose_fixes.mixins.json", "environment": "client"}, "rose_trainers.mixins.json"], "depends": {"minecraft": "~1.21.1"}, "license": "MIT"}
with zipfile.ZipFile(sys.argv[1], "w", zipfile.ZIP_DEFLATED) as jar:
    jar.writestr("fabric.mod.json", json.dumps(MOD))
    classes = os.path.join(os.path.dirname(os.path.abspath(__file__)), "rose_fixes")
    for folder, _, files in os.walk(os.path.join(classes, "rose")):
        for name in sorted(files):
            with open(os.path.join(folder, name), "rb") as handle:
                jar.writestr(os.path.relpath(os.path.join(folder, name), classes).replace(os.sep, "/"), handle.read())
    jar.writestr("rose_fixes.mixins.json", json.dumps({"required": True, "minVersion": "0.8", "package": "rose.fixes.mixin", "compatibilityLevel": "JAVA_21", "client": ["CameraMixin", "FovMixin"], "injectors": {"defaultRequire": 1}}))
    # These change the trainer mod on the server and on each PC. Not required, so the game still starts if that mod changes.
    jar.writestr("rose_trainers.mixins.json", json.dumps({"required": False, "minVersion": "0.8", "package": "rose.fixes.mixin", "compatibilityLevel": "JAVA_21", "mixins": ["TrainerMixin", "LevelMixin"], "injectors": {"defaultRequire": 1}}))
    for name, (label, background, draw) in ICONS.items():
        jar.writestr(f"assets/rose_family/textures/gui/buttons/{name}.png", icon(background, draw, False, label))
        jar.writestr(f"assets/rose_family/textures/gui/buttons/{name}_hover.png", icon(background, draw, True, label))
    wording = {f"cobblemon_smartphone.action.{key}": value for key, value in NAMES.items()}
    jar.writestr("assets/cobblemon_smartphone/lang/en_us.json", json.dumps(wording, ensure_ascii=False))
    # You can already walk during battles, so R only hides the menu.
    jar.writestr("assets/cobblemon/lang/en_us.json", json.dumps({'cobblemon.battle.ui.hide_label': 'Press %1$s to hide this menu.'}))
    jar.writestr("assets/cobbledollars/lang/en_us.json", json.dumps({"gui.cobbledollars.bank": "Sell", "screen.cobbledollars.bank": "Sell Items"}))
    # Battle music is the game's own, so nothing extra is downloaded: one track each for wild, trainer and gym battles.
    jar.writestr("assets/rose_family/sounds.json", json.dumps({
        f"battle.{kind}": {"sounds": [{"name": f"minecraft:records/{track}", "stream": True}]}
        for kind, track in (("wild", "creator"), ("trainer", "pigstep"), ("gym", "otherside"))}))
    # Gym leaders and other big opponents, who get the gym music and an opening camera sweep.
    with open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "big_trainers.txt"), encoding="utf-8") as handle:
        jar.writestr("assets/rose_family/big_trainers.txt", handle.read())
    if len(sys.argv) > 2:
        with zipfile.ZipFile(glob.glob(os.path.join(sys.argv[2], "cobblemon_smartphone*.jar"))[0]) as phone:
            for path in phone.namelist():
                found = re.fullmatch(r"assets/cobblemon_smartphone/textures/gui/buttons/([a-z_0-9]+?)(_hover)?\.png", path)
                if found and found.group(1) in NATIVE_LABELS:
                    size, pixels = read_png(phone.read(path))
                    if size == SIZE:
                        caption(pixels, NATIVE_LABELS[found.group(1)])
                        jar.writestr(path, png(pixels))
                        print("named", found.group(1))
    for key, (geo, pixels) in townspeople.build().items():
        jar.writestr(f"assets/rose_family/textures/npcs/{key}.png", png(pixels, townspeople.SIZE))
        jar.writestr(f"assets/rose_family/bedrock/npcs/models/{key}.geo.json", json.dumps(geo))
        jar.writestr(f"assets/rose_family/bedrock/npcs/variations/{key}/0_{key}.json", json.dumps({
            "name": f"rose:{key}", "order": 0, "variations": [{
                "aspects": [], "layers": [], "model": f"rose_family:{key}.geo", "poser": "cobblemon:standard", "texture": f"rose_family:textures/npcs/{key}.png"}]}))
    # Server-only mods send text by key, so the wording has to be on each player's PC.
    with open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "server-mod-wording.json"), encoding="utf-8") as handle:
        for path, wording in json.load(handle).items():
            jar.writestr(path, json.dumps(wording, ensure_ascii=False))
print("wrote", sys.argv[1])
