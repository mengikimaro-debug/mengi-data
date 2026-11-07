"""Generates Mengi Data monogram launcher/splash artwork.

Run from the project root:  python scripts/generate-icons.py
"""

from PIL import Image, ImageDraw
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "android" / "res"

BRAND = (12, 43, 34, 255)       # #0c2b22
BRAND_2 = (18, 61, 46, 255)     # #123d2e
LIME = (216, 243, 107, 255)     # #d8f36b
PALE = (241, 245, 233, 255)     # #f1f5e9

def rounded_icon(size: int, padding_ratio: float = 0.0) -> Image.Image:
    """Draw a rounded 3D M on a circular badge styled like the app UI."""
    scale = 4
    s = size * scale
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    pad = int(s * padding_ratio)
    center, radius = s / 2, (s - 2 * pad) / 2
    d.ellipse((pad, pad, s - pad, s - pad), fill=(226, 220, 205, 255))
    rim = radius * 0.955
    d.ellipse(
        (center - rim, center - rim, center + rim, center + rim),
        fill=(248, 244, 234, 255),
        outline=(216, 243, 107, 185),
        width=max(1, s // 256),
    )
    face = radius * 0.89
    d.ellipse(
        (center - face, center - face, center + face, center + face),
        fill=(252, 250, 244, 255),
        outline=(255, 255, 255, 220),
        width=max(1, s // 256),
    )
    dot_color = (12, 43, 34, 26)
    dot_step = max(12, int(s * 0.04))
    dot_radius = max(1, int(s * 0.002))
    for y in range(int(center - face * 0.9), int(center + face * 0.9), dot_step):
        for x in range(int(center - face * 0.9), int(center + face * 0.9), dot_step):
            if (x - center) ** 2 + (y - center) ** 2 < (face * 0.88) ** 2:
                d.ellipse((x, y, x + dot_radius * 2, y + dot_radius * 2), fill=dot_color)

    def sideways(points):
        return [(int((1 - y) * s), int(x * s)) for x, y in points]

    def cubic(start, control1, control2, end, steps=12):
        points = []
        for step in range(1, steps + 1):
            t = step / steps
            inverse = 1 - t
            points.append(tuple(
                inverse ** 3 * a
                + 3 * inverse ** 2 * t * b
                + 3 * inverse * t ** 2 * c
                + t ** 3 * e
                for a, b, c, e in zip(start, control1, control2, end)
            ))
        return points

    upright_path = [(0.285, 0.71), (0.285, 0.355)]
    upright_path += cubic((0.285, 0.355), (0.285, 0.31), (0.312, 0.312), (0.344, 0.35))
    upright_path.append((0.482, 0.493))
    upright_path += cubic((0.482, 0.493), (0.492, 0.505), (0.508, 0.505), (0.518, 0.493))
    upright_path.append((0.656, 0.35))
    upright_path += cubic((0.656, 0.35), (0.688, 0.312), (0.715, 0.31), (0.715, 0.355))
    upright_path.append((0.715, 0.71))
    path = sideways(upright_path)
    stroke = max(3, int(s * 0.085))

    def draw_rounded_line(draw, points, fill, width):
        draw.line(points, fill=fill, width=width, joint="curve")
        half = width / 2
        for x, y in (points[0], points[-1]):
            draw.ellipse((x - half, y - half, x + half, y + half), fill=fill)

    # Stacked rounded strokes make a soft extruded edge, not sharp polygon corners.
    for offset in range(16, 0, -1):
        depth = offset * s * 0.0013
        shade = 16 - offset
        color = (
            round(7 + 11 * shade / 15),
            round(30 + 55 * shade / 15),
            round(23 + 41 * shade / 15),
            255,
        )
        draw_rounded_line(
            d, [(x, y + depth) for x, y in path], color, stroke + max(1, s // 96)
        )

    mask = Image.new("L", (s, s), 0)
    draw_rounded_line(ImageDraw.Draw(mask), path, 255, stroke)
    gradient = Image.new("RGBA", (s, s))
    gradient_pixels = gradient.load()
    stops = [(74, 139, 104), (27, 85, 64), (12, 43, 34)]
    for y in range(int(0.26 * s), int(0.74 * s) + 1):
        for x in range(int(0.26 * s), int(0.74 * s) + 1):
            ratio = min(1, max(0, ((x / s) * 0.42 + (y / s) * 0.58 - 0.33) / 0.36))
            segment, local = (0, ratio * 2) if ratio < 0.5 else (1, (ratio - 0.5) * 2)
            start, end = stops[segment], stops[segment + 1]
            gradient_pixels[x, y] = tuple(round(a + (b - a) * local) for a, b in zip(start, end)) + (255,)
    img.alpha_composite(Image.composite(gradient, Image.new("RGBA", (s, s)), mask))

    highlight_path = [(0.30, 0.35)]
    highlight_path += cubic((0.30, 0.35), (0.30, 0.32), (0.315, 0.33), (0.35, 0.37))
    highlight_path.append((0.482, 0.503))
    highlight_path += cubic((0.482, 0.503), (0.492, 0.514), (0.508, 0.514), (0.518, 0.503))
    highlight_path.append((0.65, 0.37))
    highlight_path += cubic((0.65, 0.37), (0.685, 0.33), (0.70, 0.32), (0.70, 0.35))
    highlight = sideways(highlight_path)
    draw_rounded_line(d, highlight, (216, 243, 107, 190), max(2, int(s * 0.009)))
    return img.resize((size, size), Image.LANCZOS)


def splash_logo(size: int) -> Image.Image:
    return rounded_icon(size, padding_ratio=0.0)


def main() -> None:
    densities = {
        "mdpi": 48,
        "hdpi": 72,
        "xhdpi": 96,
        "xxhdpi": 144,
        "xxxhdpi": 192,
    }
    for dpi, px in densities.items():
        folder = RES / f"mipmap-{dpi}"
        folder.mkdir(parents=True, exist_ok=True)
        rounded_icon(px).save(folder / "ic_launcher.png", "PNG")
        # Round mask version for launchers that ask for it.
        rounded_icon(px).save(folder / "ic_launcher_round.png", "PNG")

    drawable = RES / "drawable-nodpi"
    drawable.mkdir(parents=True, exist_ok=True)
    splash_logo(512).save(drawable / "splash_logo.png", "PNG")

    docs = ROOT / "artwork"
    docs.mkdir(exist_ok=True)
    icon = rounded_icon(512)
    icon.save(docs / "mengi-data-icon-512.png", "PNG")
    icon.save(ROOT / "public" / "mengi-data-icon-512.png", "PNG")

    print("Icons written to", RES)


if __name__ == "__main__":
    main()
