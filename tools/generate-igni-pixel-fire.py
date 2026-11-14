"""Generate the original Igni flipbook. Each cell is 64x32, with binary alpha."""
from math import sin, pi
from pathlib import Path
from PIL import Image

atlas = Image.new("RGBA", (512, 64))
palette = ["#aa310e", "#ec5110", "#ff8816", "#ffb623", "#ffe060", "#fff4b0"]
colors = [tuple(bytes.fromhex(color[1:])) + (255,) for color in palette]
for variant in range(2):
    for frame in range(8):
        phase = frame * pi / 4
        for x in range(64):
            t = x / 63
            envelope = min(1, (1 - t) * 4) * (.32 + .68 * min(1, t * 6))
            center = 16 - 3 * t + 2 * sin(t * 8 - phase + variant)
            ripple = 2 * sin(t * 29 - phase * 2 + variant)
            upper = envelope * (8 + ripple + 4 * max(0, sin(t * 45 - phase)) ** 3)
            lower = envelope * (6 + 2 * sin(t * 36 - phase + variant))
            for y in range(32):
                offset = y - center
                radius = upper if offset < 0 else lower
                if radius <= 0 or abs(offset) >= radius:
                    continue
                heat = 1 - abs(offset) / radius
                flicker = .12 * sin(t * 46 - phase * 2 + y * .42)
                index = max(0, min(5, int((heat + flicker) * 6)))
                atlas.putpixel((frame * 64 + x, variant * 32 + y), colors[index])

root = Path(__file__).resolve().parents[1]
atlas.save(root / "src/main/resources/assets/witchersbestiary/textures/effect/igni_pixel_fire.png")
