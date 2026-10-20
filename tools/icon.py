"""Draws the README logo, the launcher icon's VHS tape in pink neon, as docs/logo.png.

python tools/icon.py
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
S = 1024
K = 4  # supersampling


def main():
    n = S * K
    u = n / 108
    im = Image.new("RGBA", (n, n), (0x0D, 0x0B, 0x14, 255))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 78 * u, n, n], fill=(0x1C, 0x23, 0x50))
    d.rectangle([0, 74 * u, n, 78 * u], fill=(0x16, 0x1A, 0x3C))

    box = [27 * u, 38 * u, 81 * u, 70 * u]
    # The neon's glow, blurred, under the tube.
    glow = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    ImageDraw.Draw(glow).rounded_rectangle(box, radius=3 * u, outline=(0xFF, 0x4F, 0xA3, 200), width=int(4 * u))
    im = Image.alpha_composite(im, glow.filter(ImageFilter.GaussianBlur(4 * u)))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle(box, radius=3 * u, fill=(0x17, 0x13, 0x1F), outline=(0xFF, 0x4F, 0xA3), width=int(2 * u))
    d.rectangle([34 * u, 41 * u, 74 * u, 45 * u], fill=(0xF5, 0xF0, 0xE4))
    d.rounded_rectangle([32 * u, 48 * u, 76 * u, 60 * u], radius=6 * u, fill=(0x0D, 0x0B, 0x14))

    def circle(cx, cy, r, **kw):
        d.ellipse([(cx - r) * u, (cy - r) * u, (cx + r) * u, (cy + r) * u], **kw)

    circle(42, 54, 8, fill=(0x4A, 0x2E, 0x1E))
    circle(42, 54, 3.5, fill=(0xF4, 0xEE, 0xE3))
    circle(66, 54, 3.5, fill=(0xF4, 0xEE, 0xE3))
    cyan = (0x52, 0xE6, 0xFF)
    d.polygon([(54 * u, 62 * u), (48 * u, 65.5 * u), (54 * u, 69 * u)], fill=cyan)
    d.polygon([(60 * u, 62 * u), (54 * u, 65.5 * u), (60 * u, 69 * u)], fill=cyan)

    mask = Image.new("L", (n, n), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, n - 1, n - 1], radius=int(n * 0.22), fill=255)
    out = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    out.paste(im, (0, 0), mask)
    out = out.resize((S, S), Image.LANCZOS)
    (ROOT / "docs").mkdir(exist_ok=True)
    out.save(ROOT / "docs/logo.png")


if __name__ == "__main__":
    main()
