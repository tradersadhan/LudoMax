#!/usr/bin/env python3
"""Generates the LudoMax logo, launcher icons, in-app title logo and Play Store graphics.

Everything is drawn from scratch (original artwork). Requires: Pillow.
Usage: python3 scripts/generate_icons.py
"""
import math, os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.join(os.path.dirname(__file__), "..")
RES = os.path.join(ROOT, "app", "src", "main", "res")
STORE = os.path.join(ROOT, "store")
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"

GREEN, RED, BLUE, YELLOW = (34, 177, 76), (237, 28, 36), (0, 162, 232), (255, 201, 14)
NAVY1, NAVY2 = (10, 47, 194), (2, 11, 79)
S = 4  # supersampling


def gradient(size, c1, c2, angle=315):
    w, h = size
    base = Image.new("RGB", size, c1)
    top = Image.new("RGB", size, c2)
    mask = Image.new("L", size)
    px = mask.load()
    a = math.radians(angle)
    dx, dy = math.cos(a), math.sin(a)
    mx = abs(dx) * w + abs(dy) * h
    for y in range(h):
        for x in range(w):
            v = ((x - w / 2) * dx + (y - h / 2) * dy) / mx + 0.5
            px[x, y] = int(max(0, min(1, v)) * 255)
    return Image.composite(top, base, mask)


def die(size, pips=6, rot=0, border=(255, 255, 255)):
    """Returns an RGBA die image (white, soft shadow baked in outside)."""
    w = size * S
    img = Image.new("RGBA", (w, w), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = int(w * 0.2)
    d.rounded_rectangle([0, 0, w - 1, w - 1], r, fill=(255, 255, 255, 255))
    d.rounded_rectangle([int(w * .035), int(w * .035), w - int(w * .035), w - int(w * .035)], int(r * .8),
                        fill=(244, 247, 252, 255))
    step = w * 0.25
    c = w / 2
    layout = {1: [4], 2: [0, 8], 3: [0, 4, 8], 4: [0, 2, 6, 8], 5: [0, 2, 4, 6, 8], 6: [0, 2, 3, 5, 6, 8]}[pips]
    pr = w * 0.085
    for p in layout:
        x = c + (p % 3 - 1) * step
        y = c + (p // 3 - 1) * step
        d.ellipse([x - pr, y - pr, x + pr, y + pr], fill=(20, 33, 61, 255))
    img = img.rotate(rot, resample=Image.BICUBIC, expand=True)
    return img.resize((img.width // S, img.height // S), Image.LANCZOS)


def badge(size):
    """Four-colour ludo disc with a white cross and a die on top. RGBA, transparent corners."""
    w = size * S
    img = Image.new("RGBA", (w, w), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    pad = int(w * 0.02)
    box = [pad, pad, w - pad, w - pad]
    # outer ring
    d.ellipse(box, fill=(255, 255, 255, 255))
    inner = [pad + w * 0.035] * 2 + [w - pad - w * 0.035] * 2
    d.ellipse(inner, fill=(20, 30, 90, 255))
    # four quadrants clipped to a circle
    quad = Image.new("RGBA", (w, w), (0, 0, 0, 0))
    q = ImageDraw.Draw(quad)
    m = w / 2
    g = w * 0.035
    q.rectangle([m + g, 0, w, m - g], fill=GREEN + (255,))       # top-right
    q.rectangle([m + g, m + g, w, w], fill=RED + (255,))         # bottom-right
    q.rectangle([0, m + g, m - g, w], fill=BLUE + (255,))        # bottom-left
    q.rectangle([0, 0, m - g, m - g], fill=YELLOW + (255,))      # top-left
    mask = Image.new("L", (w, w), 0)
    ImageDraw.Draw(mask).ellipse([inner[0] + w * .012, inner[1] + w * .012, inner[2] - w * .012, inner[3] - w * .012], fill=255)
    img.paste(quad, (0, 0), mask)
    # small pawn-dots in each quadrant
    for (cx, cy, col) in [(0.75, 0.25, GREEN), (0.75, 0.75, RED), (0.25, 0.75, BLUE), (0.25, 0.25, YELLOW)]:
        pass
    out = img.resize((size, size), Image.LANCZOS)
    dsize = int(size * 0.5)
    dimg = die(dsize, 6, rot=-12)
    # shadow
    sh = Image.new("RGBA", out.size, (0, 0, 0, 0))
    sh.paste((0, 0, 20, 140), (int((size - dimg.width) / 2 + size * .02), int((size - dimg.height) / 2 + size * .035)), dimg.split()[3])
    sh = sh.filter(ImageFilter.GaussianBlur(size * 0.02))
    out = Image.alpha_composite(out, sh)
    out.alpha_composite(dimg, (int((size - dimg.width) / 2), int((size - dimg.height) / 2)))
    return out


def foreground_layer(size=1024):
    """Adaptive icon foreground: badge inside the 66% safe zone."""
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    b = badge(int(size * 0.60))
    layer.alpha_composite(b, ((size - b.width) // 2, (size - b.height) // 2))
    return layer


def background_layer(size=1024):
    return gradient((size, size), NAVY1, NAVY2).convert("RGBA")


def monochrome_layer(size=1024):
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    w = int(size * 0.60) * S
    big = Image.new("RGBA", (w, w), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    d.ellipse([w * .02, w * .02, w * .98, w * .98], outline=(255, 255, 255, 255), width=int(w * 0.05))
    # die silhouette with pips cut out
    ds = int(w * 0.5)
    di = Image.new("RGBA", (ds, ds), (0, 0, 0, 0))
    dd = ImageDraw.Draw(di)
    dd.rounded_rectangle([0, 0, ds - 1, ds - 1], int(ds * .2), fill=(255, 255, 255, 255))
    step = ds * 0.25
    pr = ds * 0.085
    for p in [0, 2, 3, 5, 6, 8]:
        x = ds / 2 + (p % 3 - 1) * step
        y = ds / 2 + (p // 3 - 1) * step
        dd.ellipse([x - pr, y - pr, x + pr, y + pr], fill=(0, 0, 0, 0))
    di = di.rotate(-12, resample=Image.BICUBIC, expand=True)
    big.alpha_composite(di, ((w - di.width) // 2, (w - di.height) // 2))
    small = big.resize((w // S, w // S), Image.LANCZOS)
    layer.alpha_composite(small, ((size - small.width) // 2, (size - small.height) // 2))
    return layer


def composite_icon(size, shape):
    """Full legacy icon: background + foreground (scaled up a bit), masked to shape."""
    bg = background_layer(1024)
    fg = foreground_layer(1024)
    fg = fg.resize((int(1024 * 1.22), int(1024 * 1.22)), Image.LANCZOS)
    bg.alpha_composite(fg, ((1024 - fg.width) // 2, (1024 - fg.height) // 2))
    if shape != "square":
        mask = Image.new("L", (1024 * 2, 1024 * 2), 0)
        md = ImageDraw.Draw(mask)
        if shape == "round":
            md.ellipse([0, 0, 2047, 2047], fill=255)
        else:
            md.rounded_rectangle([0, 0, 2047, 2047], 450, fill=255)
        mask = mask.resize((1024, 1024), Image.LANCZOS)
        bg.putalpha(mask)
    return bg.resize((size, size), Image.LANCZOS)


def title_logo(width=1100):
    """'LudoMax' wordmark with the badge above it (transparent PNG for splash/menu)."""
    height = int(width * 0.62)
    img = Image.new("RGBA", (width * S // 2, height * S // 2), (0, 0, 0, 0))
    W, H = img.size
    d = ImageDraw.Draw(img)
    # badge on top
    bsz = int(H * 0.52)
    b = badge(bsz)
    img.alpha_composite(b, ((W - bsz) // 2, int(H * 0.02)))
    # text
    text = "LudoMax"
    fs = int(W * 0.185)
    font = ImageFont.truetype(FONT, fs)
    cols = [GREEN, RED, BLUE, YELLOW, GREEN, RED, BLUE]
    widths = [d.textlength(ch, font=font) for ch in text]
    total = sum(widths)
    x = (W - total) / 2
    y = int(H * 0.60)
    # shadow + outline passes
    shadow = Image.new("RGBA", img.size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    xx = x
    for ch, wv in zip(text, widths):
        sd.text((xx + 6, y + 12), ch, font=font, fill=(0, 0, 30, 170), stroke_width=int(fs * 0.12), stroke_fill=(0, 0, 30, 170))
        xx += wv
    shadow = shadow.filter(ImageFilter.GaussianBlur(5))
    img = Image.alpha_composite(img, shadow)
    d = ImageDraw.Draw(img)
    xx = x
    for ch, wv, col in zip(text, widths, cols):
        d.text((xx, y), ch, font=font, fill=col + (255,), stroke_width=int(fs * 0.12), stroke_fill=(255, 255, 255, 255))
        xx += wv
    xx = x
    for ch, wv, col in zip(text, widths, cols):
        d.text((xx, y), ch, font=font, fill=col + (255,), stroke_width=int(fs * 0.035), stroke_fill=tuple(int(c * 0.55) for c in col) + (255,))
        xx += wv
    img = img.resize((width, height), Image.LANCZOS)
    return img


def board_art(size):
    """Stylised 9x9 ludo board used only for the Play Store feature graphic."""
    w = size * 2
    img = Image.new("RGBA", (w, w), (255, 255, 255, 255))
    d = ImageDraw.Draw(img)
    c = w / 9
    cols = {0: GREEN, 1: RED, 2: BLUE, 3: YELLOW}
    colored = {(1, 5): 0, (1, 4): 0, (2, 4): 0, (5, 7): 1, (4, 7): 1, (4, 6): 1,
               (7, 3): 2, (7, 4): 2, (6, 4): 2, (3, 1): 3, (4, 1): 3, (4, 2): 3}
    for r in range(9):
        for k in range(9):
            corner = (r < 3 or r > 5) and (k < 3 or k > 5)
            centre = 3 <= r <= 5 and 3 <= k <= 5
            if corner or centre:
                continue
            fill = cols[colored[(r, k)]] if (r, k) in colored else (255, 255, 255)
            d.rectangle([k * c, r * c, (k + 1) * c, (r + 1) * c], fill=fill, outline=(60, 60, 60))
    for i, (ox, oy) in enumerate([(6, 0), (6, 6), (0, 6), (0, 0)]):
        d.rectangle([ox * c, oy * c, (ox + 3) * c, (oy + 3) * c], fill=cols[i], outline=(40, 40, 40))
        d.rectangle([(ox + .3) * c, (oy + .3) * c, (ox + 2.7) * c, (oy + 2.7) * c], fill=(255, 255, 255), outline=(40, 40, 40))
        for t in (1, 2):
            cx, cy = (ox + t) * c, (oy + t) * c
            d.ellipse([cx - .34 * c, cy - .34 * c, cx + .34 * c, cy + .34 * c], fill=cols[i], outline=(40, 40, 40))
    a, b, m = 3 * c, 6 * c, 4.5 * c
    d.polygon([(a, a), (b, a), (m, m)], fill=GREEN)
    d.polygon([(b, a), (b, b), (m, m)], fill=RED)
    d.polygon([(b, b), (a, b), (m, m)], fill=BLUE)
    d.polygon([(a, b), (a, a), (m, m)], fill=YELLOW)
    return img.resize((size, size), Image.LANCZOS)


def feature_graphic():
    W, H = 1024, 500
    bg = gradient((W, H), NAVY1, NAVY2, 20).convert("RGBA")
    d = ImageDraw.Draw(bg, "RGBA")
    for (x, y, r, col) in [(80, 90, 40, GREEN), (940, 420, 55, RED), (520, 440, 26, YELLOW), (300, 40, 22, BLUE), (980, 70, 30, YELLOW)]:
        d.ellipse([x - r, y - r, x + r, y + r], fill=col + (70,))
    ba = board_art(380).rotate(-8, resample=Image.BICUBIC, expand=True)
    sh = Image.new("RGBA", bg.size, (0, 0, 0, 0))
    sh.paste((0, 0, 30, 150), (590, 70), ba.split()[3])
    sh = sh.filter(ImageFilter.GaussianBlur(14))
    bg = Image.alpha_composite(bg, sh)
    bg.alpha_composite(ba, (575, 50))
    dz = die(120, 6, rot=14)
    bg.alpha_composite(dz, (520, 330))
    dz2 = die(80, 5, rot=-18)
    bg.alpha_composite(dz2, (930, 40))
    logo = title_logo(560)
    bg.alpha_composite(logo, (10, 20))
    d = ImageDraw.Draw(bg)
    f = ImageFont.truetype(FONT, 30)
    d.text((40, 395), "Classic Ludo. 100% offline.", font=f, fill=(255, 233, 74))
    f2 = ImageFont.truetype(FONT, 22)
    d.text((40, 440), "Play vs computer or with friends", font=f2, fill=(207, 227, 255))
    return bg.convert("RGB")


def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    print("wrote", os.path.relpath(path, ROOT))


if __name__ == "__main__":
    # Adaptive icon layers (API 26+)
    save(foreground_layer(1024).resize((432, 432), Image.LANCZOS), os.path.join(RES, "drawable-nodpi", "ic_launcher_foreground.png"))
    save(monochrome_layer(1024).resize((432, 432), Image.LANCZOS), os.path.join(RES, "drawable-nodpi", "ic_launcher_monochrome.png"))
    # Legacy icons
    for name, px in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        save(composite_icon(px, "rounded"), os.path.join(RES, f"mipmap-{name}", "ic_launcher.png"))
        save(composite_icon(px, "round"), os.path.join(RES, f"mipmap-{name}", "ic_launcher_round.png"))
    # In-app logo
    save(title_logo(1000), os.path.join(RES, "drawable-nodpi", "logo_title.png"))
    # Play Store assets
    save(composite_icon(512, "square").convert("RGBA"), os.path.join(STORE, "play-store-icon-512.png"))
    save(feature_graphic(), os.path.join(STORE, "feature-graphic-1024x500.png"))
    save(title_logo(1200), os.path.join(STORE, "logo-wordmark.png"))
