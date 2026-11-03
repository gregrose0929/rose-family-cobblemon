"""Draw the phone's extra app icons and package them as a small resource-only mod. Usage: make_phone_icons.py OUTPUT.jar"""
import json
import math
import os
import struct
import sys
import zipfile
import zlib

SIZE = 36
WHITE = (255, 255, 255, 255)
DARK = (35, 35, 40, 255)
RED = (214, 52, 52, 255)


def png(pixels, size=SIZE):
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    rows = b"".join(b"\x00" + bytes(c for x in range(size) for c in pixels[y][x]) for y in range(size))
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(rows)) + chunk(b"IEND", b"")


def icon(background, draw, hover):
    color = tuple(min(255, c + 45) for c in background[:3]) + (255,) if hover else background
    pixels = [[(0, 0, 0, 0)] * SIZE for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            # Rounded square, like the phone's own buttons.
            dx, dy = max(6 - x, x - 29, 0), max(6 - y, y - 29, 0)
            if math.hypot(dx, dy) <= 6:
                pixels[y][x] = color

    def put(x, y, value=WHITE):
        if 0 <= x < SIZE and 0 <= y < SIZE:
            pixels[y][x] = value

    def rect(x1, y1, x2, y2, value=WHITE):
        for y in range(y1, y2 + 1):
            for x in range(x1, x2 + 1):
                put(x, y, value)

    draw(put, rect)
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


def gift(put, rect):
    # A wrapped present.
    box, ribbon = (240, 240, 240, 255), (235, 70, 70, 255)
    rect(8, 15, 27, 28, box)
    rect(7, 11, 28, 15, box)
    rect(16, 11, 19, 28, ribbon)
    rect(7, 13, 28, 14, ribbon)
    rect(11, 7, 15, 10, ribbon)
    rect(20, 7, 24, 10, ribbon)


ICONS = {"gift": ((170, 60, 130, 255), gift), "travel": ((40, 140, 140, 255), travel), "today": ((60, 130, 190, 255), today), "cup": ((150, 40, 60, 255), cup), "go_home": ((70, 120, 140, 255), go_home), "set_home": ((60, 110, 60, 255), set_home), "go_back": ((110, 110, 120, 255), go_back), "spawns": ((130, 90, 50, 255), spawns), "town": ((190, 60, 90, 255), town), "hunts": ((205, 110, 30, 255), hunts), "balls": ((60, 90, 170, 255), balls), "dex": ((50, 130, 80, 255), dex), "guide": ((120, 70, 170, 255), guide)}
def skin(tone, hair, shirt, sleeves, pants, shoes, extra=None):
    """Paints a 64x64 player-layout skin, front details on the head and chest."""
    pixels = [[(0, 0, 0, 0)] * 64 for _ in range(64)]

    def fill(x1, y1, x2, y2, color):
        for y in range(y1, y2 + 1):
            for x in range(x1, x2 + 1):
                pixels[y][x] = color + (255,)

    fill(0, 0, 31, 15, tone)  # head, all sides
    fill(8, 0, 15, 7, hair)
    fill(0, 8, 31, 9, hair)
    fill(24, 8, 31, 15, hair)
    for x, y in ((9, 12), (14, 12)):
        fill(x, y, x, y, (40, 40, 50))
    fill(11, 14, 12, 14, (150, 80, 70))
    fill(16, 16, 39, 31, shirt)  # body
    fill(40, 16, 55, 31, sleeves)  # arms
    fill(32, 48, 47, 63, sleeves)
    fill(40, 28, 55, 31, tone)
    fill(32, 60, 47, 63, tone)
    fill(0, 16, 15, 31, pants)  # legs
    fill(16, 48, 31, 63, pants)
    fill(0, 28, 15, 31, shoes)
    fill(16, 60, 31, 63, shoes)
    if extra:
        extra(fill)
    return png(pixels, 64)


def professor_look(fill):
    fill(23, 20, 24, 31, (170, 60, 90))  # tie
    fill(21, 20, 22, 31, (120, 170, 210))  # shirt under the coat
    fill(25, 20, 26, 31, (120, 170, 210))
    fill(9, 11, 10, 11, (60, 60, 70))  # glasses
    fill(13, 11, 14, 11, (60, 60, 70))


def angler_look(fill):
    fill(8, 8, 15, 9, (70, 110, 70))  # bucket hat
    fill(0, 8, 7, 9, (70, 110, 70))
    fill(16, 8, 31, 9, (70, 110, 70))
    fill(8, 0, 15, 7, (70, 110, 70))
    fill(9, 13, 14, 15, (215, 215, 215))  # beard
    fill(23, 20, 24, 31, (90, 80, 60))  # vest opening
    fill(11, 14, 12, 14, (150, 80, 70))


def ranger_look(fill):
    fill(8, 0, 15, 8, (190, 50, 50))  # cap
    fill(0, 8, 31, 8, (190, 50, 50))
    fill(20, 20, 27, 21, (190, 50, 50))  # neckerchief
    fill(20, 26, 27, 26, (60, 50, 40))  # belt


def lily_look(fill):
    fill(20, 20, 27, 21, (255, 255, 255))  # collar
    fill(0, 16, 15, 21, (240, 130, 170))  # dress over the legs
    fill(16, 48, 31, 53, (240, 130, 170))
    fill(8, 10, 8, 15, (120, 75, 40))  # pigtails
    fill(15, 10, 15, 15, (120, 75, 40))


TOWNSPEOPLE = {
    "professor": ((225, 180, 150), (190, 190, 195), (235, 235, 240), (235, 235, 240), (70, 70, 85), (50, 40, 35), professor_look),
    "angler": ((205, 160, 125), (215, 215, 215), (120, 110, 80), (160, 145, 110), (60, 80, 120), (70, 50, 35), angler_look),
    "ranger": ((170, 120, 90), (50, 35, 30), (70, 120, 80), (70, 120, 80), (95, 85, 60), (50, 40, 35), ranger_look),
    "lily": ((240, 200, 170), (120, 75, 40), (240, 130, 170), (240, 130, 170), (240, 200, 170), (200, 60, 90), lily_look),
}
MOD = {"schemaVersion": 1, "id": "rose_family_icons", "version": "1.3.0", "name": "Rose Family Icons", "description": "Icons for the Rose Family Server's phone apps, plus wording for server-only mods.", "environment": "client", "depends": {"minecraft": "~1.21.1"}, "license": "MIT"}
with zipfile.ZipFile(sys.argv[1], "w", zipfile.ZIP_DEFLATED) as jar:
    jar.writestr("fabric.mod.json", json.dumps(MOD))
    for name, (background, draw) in ICONS.items():
        jar.writestr(f"assets/rose_family/textures/gui/buttons/{name}.png", icon(background, draw, False))
        jar.writestr(f"assets/rose_family/textures/gui/buttons/{name}_hover.png", icon(background, draw, True))
    for key, look in TOWNSPEOPLE.items():
        jar.writestr(f"assets/rose_family/textures/npcs/{key}.png", skin(*look))
        jar.writestr(f"assets/rose_family/bedrock/npcs/variations/{key}/0_{key}.json", json.dumps({
            "name": f"rose:{key}", "order": 0, "variations": [{
                "aspects": [], "layers": [], "model": "cobblemon:steve.geo", "poser": "cobblemon:standard", "texture": f"rose_family:textures/npcs/{key}.png"}]}))
    # Server-only mods send text by key, so the wording has to be on each player's PC.
    with open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "server-mod-wording.json"), encoding="utf-8") as handle:
        for path, wording in json.load(handle).items():
            jar.writestr(path, json.dumps(wording, ensure_ascii=False))
print("wrote", sys.argv[1])
