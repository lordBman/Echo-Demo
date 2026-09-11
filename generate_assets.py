"""
Generates placeholder sprites for ECHO so the vertical slice can be
visually playtested locally instead of relying only on Box2D debug draw.
These are deliberately simple flat-shape placeholders, not final art
(section 31/32's semi-transparent/trail/distortion treatment is applied
at the "distinct enough to read at a glance" level, not final polish) —
swap the PNGs in assets/ for real art later with zero code changes,
since the renderer just draws whatever texture is in each file.

Run (from the project root): python3 generate_assets.py
Requires: pillow (pip install pillow --break-system-packages)
"""
from PIL import Image, ImageDraw, ImageFilter
import os

OUT = os.path.join(os.path.dirname(__file__), "assets")
os.makedirs(OUT, exist_ok=True)

SS = 4  # supersample factor for anti-aliasing


def canvas(w, h):
    return Image.new("RGBA", (w * SS, h * SS), (0, 0, 0, 0))


def save(img, name, w, h):
    img = img.resize((w, h), Image.LANCZOS)
    img.save(os.path.join(OUT, name))
    print("wrote", name, img.size)


def draw_critter(draw, s, body_color, eye_color, pupil_color, ear_color, ghostly=False):
    """Shared silhouette for Player/Echo: rounded body, two ears, one big eye, small feet."""
    cx = 32 * s
    # Ears (small triangles on top)
    draw.polygon([(cx - 20 * s, 26 * s), (cx - 30 * s, 6 * s), (cx - 8 * s, 16 * s)], fill=ear_color)
    draw.polygon([(cx + 20 * s, 26 * s), (cx + 30 * s, 6 * s), (cx + 8 * s, 16 * s)], fill=ear_color)
    # Body (rounded rect)
    draw.rounded_rectangle(
        [cx - 22 * s, 20 * s, cx + 22 * s, 88 * s], radius=18 * s, fill=body_color)
    # Feet nubs
    draw.ellipse([cx - 18 * s, 82 * s, cx - 4 * s, 94 * s], fill=body_color)
    draw.ellipse([cx + 4 * s, 82 * s, cx + 18 * s, 94 * s], fill=body_color)
    # Eye (big, offset right = default facing right)
    eye_cx, eye_cy, eye_r = cx + 8 * s, 44 * s, 13 * s
    draw.ellipse([eye_cx - eye_r, eye_cy - eye_r, eye_cx + eye_r, eye_cy + eye_r], fill=eye_color)
    if ghostly:
        # Hollow ring pupil for the Echo's "not fully here" look.
        pr = 6 * s
        draw.ellipse([eye_cx - pr, eye_cy - pr, eye_cx + pr, eye_cy + pr], outline=pupil_color, width=3 * s)
    else:
        pr = 6 * s
        draw.ellipse([eye_cx - pr, eye_cy - pr, eye_cx + pr, eye_cy + pr], fill=pupil_color)


def make_player():
    W, H = 64, 96
    img = canvas(W, H)
    d = ImageDraw.Draw(img)
    draw_critter(
        d, SS,
        body_color=(242, 149, 68, 255),   # warm orange
        eye_color=(255, 255, 255, 255),
        pupil_color=(40, 30, 25, 255),
        ear_color=(224, 122, 63, 255),
    )
    save(img, "player.png", W, H)


def make_echo():
    W, H = 64, 96
    img = canvas(W, H)

    # Soft outer glow: draw the silhouette larger + blurred underneath, per section 32.
    glow = canvas(W, H)
    gd = ImageDraw.Draw(glow)
    draw_critter(
        gd, SS,
        body_color=(0, 217, 255, 90),
        eye_color=(0, 217, 255, 60),
        pupil_color=(0, 0, 0, 0),
        ear_color=(0, 217, 255, 90),
    )
    glow = glow.filter(ImageFilter.GaussianBlur(radius=6 * SS))

    d = ImageDraw.Draw(img)
    draw_critter(
        d, SS,
        body_color=(120, 220, 255, 150),   # translucent cool cyan
        eye_color=(200, 245, 255, 190),
        pupil_color=(0, 217, 255, 220),
        ear_color=(90, 200, 245, 150),
        ghostly=True,
    )

    combined = Image.alpha_composite(glow, img)
    save(combined, "echo.png", W, H)


def make_ground_tile():
    W, H = 64, 64
    img = canvas(W, H)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 64 * SS, 64 * SS], fill=(46, 52, 64, 255))
    # Beveled highlight edges
    d.rectangle([0, 0, 64 * SS, 6 * SS], fill=(76, 86, 106, 255))
    d.rectangle([0, 0, 4 * SS, 64 * SS], fill=(60, 68, 84, 255))
    d.rectangle([64 * SS - 4 * SS, 0, 64 * SS, 64 * SS], fill=(34, 39, 48, 255))
    d.rectangle([0, 64 * SS - 4 * SS, 64 * SS, 64 * SS], fill=(28, 32, 40, 255))
    # A couple of rivets for texture
    for rx, ry in [(10, 10), (54, 10), (10, 54), (54, 54)]:
        d.ellipse([(rx - 3) * SS, (ry - 3) * SS, (rx + 3) * SS, (ry + 3) * SS], fill=(20, 24, 30, 255))
    # Thin teal accent line (echoes the game's temporal-energy motif)
    d.rectangle([0, 6 * SS, 64 * SS, 8 * SS], fill=(0, 217, 255, 120))
    save(img, "ground_tile.png", W, H)


def make_plate(pressed: bool):
    W, H = 96, 24
    img = canvas(W, H)
    d = ImageDraw.Draw(img)
    base_color = (40, 44, 52, 255)
    accent = (46, 204, 113, 255) if pressed else (241, 196, 15, 255)
    d.rounded_rectangle([2 * SS, 2 * SS, 94 * SS, 22 * SS], radius=4 * SS, fill=base_color)
    inset = 6 * SS if pressed else 4 * SS
    top = 10 * SS if pressed else 6 * SS
    d.rounded_rectangle([inset, top, 96 * SS - inset, 18 * SS], radius=3 * SS, fill=accent)
    name = "plate_down.png" if pressed else "plate_up.png"
    save(img, name, W, H)


def make_door():
    W, H = 48, 192
    img = canvas(W, H)
    d = ImageDraw.Draw(img)
    d.rectangle([2 * SS, 0, 46 * SS, 192 * SS], fill=(52, 58, 70, 255))
    # Hazard stripes top and bottom
    stripe_h = 14 * SS
    for i in range(4):
        color = (241, 196, 15, 255) if i % 2 == 0 else (30, 34, 40, 255)
        d.rectangle([2 * SS, i * stripe_h, 46 * SS, (i + 1) * stripe_h], fill=color)
        d.rectangle([2 * SS, 192 * SS - (i + 1) * stripe_h, 46 * SS, 192 * SS - i * stripe_h], fill=color)
    # Glowing teal seam down the middle
    d.rectangle([23 * SS, 0, 25 * SS, 192 * SS], fill=(0, 217, 255, 200))
    save(img, "door.png", W, H)


def make_exit():
    W, H = 64, 64
    img = canvas(W, H)
    d = ImageDraw.Draw(img)
    cx, cy = 32 * SS, 32 * SS
    for r, alpha in [(30, 60), (24, 110), (18, 180), (12, 255)]:
        d.ellipse([cx - r * SS, cy - r * SS, cx + r * SS, cy + r * SS], fill=(255, 205, 80, alpha))
    d.ellipse([cx - 30 * SS, cy - 30 * SS, cx + 30 * SS, cy + 30 * SS], outline=(255, 235, 180, 255), width=2 * SS)
    save(img, "exit.png", W, H)


if __name__ == "__main__":
    make_player()
    make_echo()
    make_ground_tile()
    make_plate(pressed=False)
    make_plate(pressed=True)
    make_door()
    make_exit()
    print("done")
