"""Rose Town's people: small 3D models and pixel-art textures. build() returns {key: (geo_json, rgba_rows)}."""
import hashlib

SIZE = 128
HAIR_FACES = {"top", "back", "left", "right"}


def shade(color, factor):
    return tuple(max(0, min(255, int(c * factor))) for c in color[:3]) + (255,)


def noise(key, x, y):
    return hashlib.md5(f"{key}{x},{y}".encode()).digest()[0] / 255 * 0.05 - 0.025


class Figure:
    """One character. Each cube is painted by a function of (face, x, y) that returns a color or None."""

    def __init__(self, key, kid=False):
        self.key, self.kid = key, kid
        self.pixels = [[(0, 0, 0, 0)] * SIZE for _ in range(SIZE)]
        self.bones = {}
        self.cursor = [0, 0, 0]
        for name, parent, pivot in self.skeleton():
            self.bones[name] = {"name": name, "pivot": pivot}
            if parent:
                self.bones[name]["parent"] = parent

    def skeleton(self):
        k = self.kid
        hip, neck, arm = (7, 14, 13.5) if k else (12, 24, 22)
        return [("trainer", None, [0, 0, 0]), ("body", "trainer", [0, 0, 0]), ("torso", "body", [0, hip, 0]), ("head", "torso", [0, neck, 0]),
                ("eyes", "head", [0, neck, 0]), ("mouth", "head", [0, neck + 1.5, -3.41]), ("eyelid_right", "eyes", [-2, neck + 3.5, -3.41]), ("eyelid_left", "eyes", [2, neck + 3.5, -3.41]),
                ("arm_ball", "torso", [-4 if k else -5, arm, 0]), ("arm_right", "arm_ball", [-4 if k else -5, arm, 0]), ("arm_left", "torso", [4 if k else 5, arm, 0]),
                ("pokeball", "arm_ball", [-5, 12, 0]), ("item_right", "arm_right", [-5, hip, -2]), ("item_left", "arm_left", [5, hip, -2]),
                ("leg_right", "body", [-1.5 if k else -1.9, hip, 0]), ("leg_left", "body", [1.5 if k else 1.9, hip, 0])]

    def cube(self, bone, origin, size, paint, inflate=0.0):
        w, h, d = (max(1, int(round(s))) for s in size)
        width, height = 2 * (w + d), h + d
        if self.cursor[0] + width > SIZE:
            self.cursor = [0, self.cursor[1] + self.cursor[2], 0]
        u, v = self.cursor[0], self.cursor[1]
        assert v + height <= SIZE, f"{self.key}: texture is full"
        self.cursor[0] += width
        self.cursor[2] = max(self.cursor[2], height)
        faces = {"top": (u + d, v, w, d, 1.08), "bottom": (u + d + w, v, w, d, 0.6), "right": (u, v + d, d, h, 0.84), "front": (u + d, v + d, w, h, 1.0),
                 "left": (u + d + w, v + d, d, h, 0.84), "back": (u + d + w + d, v + d, w, h, 0.9)}
        for face, (fx, fy, fw, fh, light) in faces.items():
            for y in range(fh):
                for x in range(fw):
                    color = paint(face, x, y, fw, fh)
                    if color is None:
                        continue
                    ramp = 1.04 - 0.14 * (y / max(1, fh - 1)) if face not in ("top", "bottom") else 1.0
                    edge = 0.94 if face != "top" and (x in (0, fw - 1)) and fw > 2 else 1.0
                    self.pixels[fy + y][fx + x] = shade(color, light * ramp * edge + noise(self.key + bone, fx + x, fy + y))
        entry = {"origin": origin, "size": list(size), "uv": [u, v]}
        if inflate:
            entry["inflate"] = inflate
        self.bones[bone].setdefault("cubes", []).append(entry)

    def geo(self):
        return {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": f"geometry.{self.key}", "texture_width": SIZE, "texture_height": SIZE, "visible_bounds_width": 3, "visible_bounds_height": 3.5, "visible_bounds_offset": [0, 1.25, 0]},
            "bones": list(self.bones.values())}]}


def solid(color):
    return lambda face, x, y, w, h: color


SKIN_LINE = (120, 70, 55)


def face_painter(skin, hair, eye, brow=None, blush=None, glasses=None, fringe=1, smile=True):
    brow = brow or shade(hair, 0.7)

    def paint(face, x, y, w, h):
        if face in ("top",) or (face == "back" and y < 7) or (face in ("left", "right") and y < 5 - (x == 0 and face == "right") - (x == w - 1 and face == "left")):
            streak = 0.92 if (x + y * 2) % 5 == 0 else 1.0
            return shade(hair, streak)
        if face == "bottom":
            return shade(skin, 0.8)
        if face != "front":
            return skin
        if y < fringe or (y == fringe and x in (0, 7)):
            return shade(hair, 0.9 if (x + y) % 3 == 0 else 1.05)
        # A row of skin between brow and eye keeps the expression friendly.
        if y == fringe and x in (1, 2, 5, 6):
            return brow
        if y == 4:
            if x in (1, 6):
                return (245, 245, 250, 255)
            if x in (2, 5):
                return eye
        if glasses and y == 4 and x in (0, 3, 4, 7):
            return glasses
        if y == 5 and x in (3, 4):
            return shade(skin, 0.88)
        if y == 6 and x in (3, 4) and smile:
            return SKIN_LINE
        if y == 6 and x in (2, 5) and smile:
            return shade(skin, 0.85)
        if blush and y == 5 and x in (1, 6):
            return blush
        return skin

    return paint


def limb(sleeve, cuff_at, hand, cuff=None):
    def paint(face, x, y, w, h):
        if face == "bottom":
            return shade(hand, 0.8)
        if y >= h - cuff_at:
            return hand
        if cuff and y == h - cuff_at - 1:
            return cuff
        return shade(sleeve, 0.96 if (y + x) % 7 == 0 else 1.0)

    return paint


def leg(pants, shoe, shoe_rows=3, sock=None, sole=(55, 45, 40)):
    def paint(face, x, y, w, h):
        if face == "bottom" or y == h - 1:
            return sole
        if y >= h - shoe_rows:
            return shoe
        if sock and y == h - shoe_rows - 1:
            return sock
        return pants(face, x, y, w, h) if callable(pants) else pants

    return paint


def base_adult(f, head, torso, arm, right_leg, left_leg=None):
    f.cube("head", [-4, 24, -4], [8, 8, 8], head, 0.01)
    f.cube("torso", [-4, 12, -2], [8, 12, 4], torso)
    f.cube("arm_right", [-8, 12, -2], [4, 12, 4], arm)
    f.cube("arm_left", [4, 12, -2], [4, 12, 4], arm)
    f.cube("leg_right", [-4, 0, -2], [4, 12, 4], right_leg)
    f.cube("leg_left", [0, 0, -2], [4, 12, 4], left_leg or right_leg)


def professor():
    f = Figure("professor")
    skin, hair, eye = (232, 190, 160, 255), (196, 198, 205, 255), (60, 90, 110, 255)
    coat, shirt, tie = (240, 241, 245, 255), (70, 130, 150, 255), (170, 45, 60, 255)

    def torso(face, x, y, w, h):
        if face != "front":
            return coat
        if x in (3, 4):
            if x == 3 and y > 0 or x == 4 and y > 0:
                return tie if y < 7 else shirt
            return shirt
        if x in (2, 5) and y < 6:
            return shade(coat, 0.8)
        if y == 8 and x in (0, 1):
            return (200, 200, 208, 255)
        if y == 3 and x == 6:
            return (50, 110, 190, 255)  # a pen in the pocket
        if y in (3, 4) and x == 6:
            return shade(coat, 0.9)
        return coat

    def tails(face, x, y, w, h):
        if face in ("top", "bottom"):
            return None
        if face == "front" and 3 <= x <= 5:
            return None
        return shade(coat, 0.97 - 0.02 * (y % 3))

    base_adult(f, face_painter(skin, hair, eye, glasses=(95, 95, 110, 255)), torso, limb(coat, 2, skin, shade(coat, 0.8)),
               leg((70, 72, 88, 255), (95, 60, 40, 255)))
    f.cube("torso", [-4.5, 5, -2.5], [9, 7, 5], tails)
    return f


def angler():
    f = Figure("angler")
    skin, hair, eye = (210, 165, 130, 255), (215, 215, 212, 255), (60, 60, 70, 255)
    vest, shirt, hat = (112, 112, 72, 255), (170, 70, 60, 255), (205, 180, 115, 255)

    def torso(face, x, y, w, h):
        if face == "front" and x in (3, 4):
            return shirt if (y % 2) else shade(shirt, 0.75)
        if face == "front" and y in (6, 7, 8) and x in (0, 1, 2, 5, 6, 7):
            return shade(vest, 0.78) if y == 6 else shade(vest, 0.9)
        return vest

    def sleeve(face, x, y, w, h):
        return shirt if (x + y) % 2 else shade(shirt, 0.72)

    def jeans(face, x, y, w, h):
        return (64, 90, 130, 255) if y % 4 else (76, 104, 146, 255)

    base_adult(f, face_painter(skin, hair, eye), torso,
               lambda face, x, y, w, h: skin if y >= h - 2 else sleeve(face, x, y, w, h),
               leg(jeans, (60, 95, 70, 255), shoe_rows=5))

    def beard(face, x, y, w, h):
        if face in ("top", "back"):
            return None
        return (222, 222, 220, 255) if (x + y) % 3 else (195, 195, 195, 255)

    f.cube("head", [-3.5, 23, -4.6], [7, 3, 1], beard)
    f.cube("head", [-4.5, 31, -4.5], [9, 3, 9], lambda face, x, y, w, h: (70, 110, 80, 255) if face != "top" and y == h - 1 else hat)
    f.cube("head", [-6.5, 30.5, -6.5], [13, 1, 13], solid(shade(hat, 0.93)))
    f.cube("arm_right", [-6.5, 11, -3.5], [1, 28, 1], lambda face, x, y, w, h: (150, 45, 45, 255) if y % 9 == 0 else (120, 85, 55, 255))
    return f


def ranger():
    f = Figure("ranger")
    skin, hair, eye = (178, 124, 92, 255), (60, 40, 32, 255), (45, 35, 30, 255)
    jacket, trim, cap = (58, 105, 82, 255), (225, 190, 70, 255), (190, 50, 55, 255)

    def torso(face, x, y, w, h):
        if face == "front" and y == 8:
            return (70, 55, 40, 255)
        if face == "front" and x in (3, 4) and y < 8:
            return (235, 235, 230, 255) if y < 3 else shade(jacket, 0.82)
        if face == "front" and x == 6 and y in (2, 3):
            return trim
        return jacket

    def shorts(face, x, y, w, h):
        return (170, 150, 105, 255) if y < 5 else skin

    base_adult(f, face_painter(skin, hair, eye, smile=True), torso, limb(jacket, 2, skin, trim),
               leg(shorts, (75, 55, 40, 255), shoe_rows=4, sock=(230, 230, 225, 255)))
    f.cube("head", [-4.5, 29.5, -4.5], [9, 3, 9], lambda face, x, y, w, h: (240, 240, 240, 255) if face == "front" and 3 <= x <= 5 and y < 2 else cap)
    f.cube("head", [-4, 29.5, -8], [8, 1, 4], solid(shade(cap, 0.85)))
    f.cube("head", [-1.5, 22.5, 4], [3, 7, 2], lambda face, x, y, w, h: shade(hair, 0.9 if x % 2 else 1.05))
    f.cube("torso", [-3.5, 13, 2], [7, 9, 3], lambda face, x, y, w, h: (150, 80, 45, 255) if y % 4 else (110, 60, 35, 255))
    return f


def lily():
    f = Figure("lily", kid=True)
    skin, hair, eye = (245, 208, 180, 255), (130, 78, 42, 255), (70, 110, 60, 255)
    dress, white, straw = (250, 205, 80, 255), (250, 250, 250, 255), (235, 210, 140, 255)
    ribbon = (235, 90, 130, 255)
    head = face_painter(skin, hair, eye, blush=(240, 170, 165, 255))
    f.cube("head", [-4, 14, -4], [8, 8, 8], head, 0.01)

    def torso(face, x, y, w, h):
        if y == 0 and face == "front":
            return white
        return dress

    f.cube("torso", [-3, 7, -1.5], [6, 7, 3], torso)
    f.cube("torso", [-3.5, 3, -2], [7, 5, 4], lambda face, x, y, w, h: None if face in ("top", "bottom") else shade(dress, 1.0 if x % 2 else 0.9))
    f.cube("arm_right", [-5, 7.5, -1], [2, 6, 2], limb(dress, 2, skin))
    f.cube("arm_left", [3, 7.5, -1], [2, 6, 2], limb(dress, 2, skin))
    shoes = leg(skin, (205, 60, 90, 255), shoe_rows=2, sock=white, sole=(150, 40, 65, 255))
    f.cube("leg_right", [-3, 0, -1.5], [3, 7, 3], shoes)
    f.cube("leg_left", [0, 0, -1.5], [3, 7, 3], shoes)
    f.cube("head", [-4.5, 21, -4.5], [9, 3, 9], lambda face, x, y, w, h: ribbon if face != "top" and y == h - 1 else straw)
    f.cube("head", [-7, 20.5, -7], [14, 1, 14], solid(shade(straw, 0.95)))
    for side in (-6, 4):
        f.cube("head", [side, 14, -1], [2, 5, 2], lambda face, x, y, w, h: ribbon if y == 0 else hair)
    return f


def plain(key, skin, hair, eye, top, pants, shoes, extra=None, sleeve=None, glasses=None, kid=False):
    """A townsperson with a simple outfit, plus any extra pieces."""
    f = Figure(key, kid=kid)
    if kid:
        f.cube("head", [-4, 14, -4], [8, 8, 8], face_painter(skin, hair, eye, glasses=glasses), 0.01)
        f.cube("torso", [-3, 7, -1.5], [6, 7, 3], solid(top))
        f.cube("arm_right", [-5, 7.5, -1], [2, 6, 2], limb(sleeve or top, 2, skin))
        f.cube("arm_left", [3, 7.5, -1], [2, 6, 2], limb(sleeve or top, 2, skin))
        legs = leg(pants, shoes, shoe_rows=2)
        f.cube("leg_right", [-3, 0, -1.5], [3, 7, 3], legs)
        f.cube("leg_left", [0, 0, -1.5], [3, 7, 3], legs)
    else:
        base_adult(f, face_painter(skin, hair, eye, glasses=glasses), top if callable(top) else solid(top), limb(sleeve or (top if not callable(top) else pants), 2, skin), leg(pants, shoes))
    if extra:
        extra(f)
    return f


def oak():
    coat, shirt = (240, 241, 245, 255), (160, 60, 55, 255)

    def torso(face, x, y, w, h):
        if face == "front" and x in (3, 4):
            return shirt
        if face == "front" and x in (2, 5) and y < 6:
            return shade(coat, 0.8)
        return coat

    def tails(f):
        f.cube("torso", [-4.5, 5, -2.5], [9, 7, 5], lambda face, x, y, w, h: None if face in ("top", "bottom") or (face == "front" and 3 <= x <= 5) else shade(coat, 0.96))

    return plain("oak", (226, 182, 150, 255), (205, 205, 205, 255), (60, 50, 40, 255), torso, (135, 110, 80, 255), (70, 50, 35, 255), tails, sleeve=coat)


def mom():
    def apron(face, x, y, w, h):
        if face == "front" and 1 <= x <= 6 and y >= 3:
            return (250, 245, 235, 255)
        return (225, 110, 130, 255)

    def hair(f):
        f.cube("head", [-4, 22, 3.5], [8, 7, 1], lambda face, x, y, w, h: (110, 60, 35, 255))

    return plain("mom", (240, 200, 170, 255), (110, 60, 35, 255), (70, 90, 60, 255), apron, (70, 90, 150, 255), (200, 200, 200, 255), hair, sleeve=(225, 110, 130, 255))


def aide():
    coat = (235, 240, 240, 255)

    def torso(face, x, y, w, h):
        return (70, 120, 90, 255) if face == "front" and x in (3, 4) else coat

    return plain("aide", (190, 140, 105, 255), (40, 30, 30, 255), (40, 30, 30, 255), torso, (60, 70, 80, 255), (40, 40, 45, 255), sleeve=coat, glasses=(95, 95, 110, 255))


def boy():
    def cap(f):
        red = (205, 50, 50, 255)
        f.cube("head", [-4.5, 19.5, -4.5], [9, 3, 9], solid(red))
        f.cube("head", [-4, 19.5, -8], [8, 1, 4], solid(shade(red, 0.85)))

    return plain("boy", (240, 200, 170, 255), (70, 45, 30, 255), (60, 80, 120, 255), (70, 120, 200, 255), (60, 70, 110, 255), (205, 50, 50, 255), cap, kid=True)


def fisher():
    hat, vest = (70, 110, 80, 255), (112, 112, 72, 255)

    def gear(f):
        f.cube("head", [-4.5, 31, -4.5], [9, 2, 9], solid(hat))
        f.cube("head", [-6, 30.5, -6], [12, 1, 12], solid(shade(hat, 0.9)))
        f.cube("arm_right", [-6.5, 11, -3.5], [1, 26, 1], lambda face, x, y, w, h: (120, 85, 55, 255))

    return plain("fisher", (195, 150, 115, 255), (50, 35, 25, 255), (40, 40, 45, 255), vest, (60, 80, 120, 255), (60, 95, 70, 255), gear, sleeve=(190, 180, 150, 255))


def fuji():
    robe = (150, 135, 170, 255)

    def beard(f):
        f.cube("head", [-3.5, 23, -4.6], [7, 3, 1], lambda face, x, y, w, h: None if face in ("top", "back") else (225, 225, 225, 255))
        f.cube("torso", [-4.5, 3, -2.5], [9, 9, 5], lambda face, x, y, w, h: None if face in ("top", "bottom") else shade(robe, 0.96))

    return plain("fuji", (220, 180, 150, 255), (225, 225, 225, 255), (50, 45, 50, 255), robe, robe, (60, 50, 60, 255), beard)


def channeler():
    robe = (85, 55, 110, 255)

    def hood(f):
        f.cube("head", [-4.5, 25, -4.5], [9, 8, 9], lambda face, x, y, w, h: None if face == "front" and 1 <= x <= 7 and y >= 2 else robe)
        f.cube("torso", [-4.5, 1, -2.5], [9, 11, 5], lambda face, x, y, w, h: None if face in ("top", "bottom") else shade(robe, 0.94 if x % 2 else 1.0))

    return plain("channeler", (225, 210, 200, 255), (40, 30, 45, 255), (60, 40, 80, 255), robe, robe, (40, 30, 45, 255), hood)


def lavgirl():
    dress = (170, 130, 210, 255)

    def skirt(f):
        f.cube("torso", [-3.5, 3, -2], [7, 5, 4], lambda face, x, y, w, h: None if face in ("top", "bottom") else shade(dress, 1.0 if x % 2 else 0.9))
        for side in (-5, 3):
            f.cube("head", [side, 14, -1], [2, 5, 2], solid((60, 40, 30, 255)))

    return plain("lavgirl", (240, 205, 180, 255), (60, 40, 30, 255), (80, 60, 110, 255), dress, (240, 205, 180, 255), (120, 80, 150, 255), skirt, kid=True)


def rater():
    suit = (60, 65, 95, 255)

    def torso(face, x, y, w, h):
        if face == "front" and x in (3, 4):
            return (215, 180, 60, 255) if y > 0 else (240, 240, 240, 255)
        return suit

    return plain("rater", (225, 185, 155, 255), (90, 90, 95, 255), (40, 40, 50, 255), torso, suit, (35, 30, 30, 255), sleeve=suit, glasses=(50, 50, 60, 255))


def build():
    figures = (professor(), angler(), ranger(), lily(), oak(), mom(), aide(), boy(), fisher(), fuji(), channeler(), lavgirl(), rater())
    return {figure.key: (figure.geo(), figure.pixels) for figure in figures}
