from pathlib import Path
from math import hypot, atan2, pi
from PIL import Image, ImageDraw

atlas = Image.new("RGBA", (128, 32))
draw = ImageDraw.Draw(atlas)
for x in range(32):
    y = 13 + abs(x - 16) // 6
    draw.line((x, y, x, y + (3 if 4 < x < 27 else 1)), (255, 255, 255, 230))
    if x % 11 < 6:
        draw.point((x, y - 3 - x % 3), (255, 255, 255, 150))
for y in range(32):
    for x in range(32):
        r = hypot(x - 15.5, y - 15.5)
        angle = atan2(y - 15.5, x - 15.5)
        sector = int((angle + pi) * 12 / (2 * pi))
        if 14 <= r < 15.6 or 10.5 <= r < 11.5 or (12 <= r < 14 and sector % 2 == 0):
            atlas.putpixel((32 + x, y), (255, 255, 255, 255 if r >= 12 else 170))
for row in range(4):
    for col in range(4):
        x, y = 64 + col * 8, row * 8
        draw.polygon([(x+3,y+1),(x+6,y+3),(x+6,y+5),(x+3,y+7),(x,y+5),(x,y+3)],
                     fill=(255,255,255,28), outline=(255,255,255,130))
draw.line([(111,4),(100,24),(123,24),(111,4),(111,19),(118,15)], (255,255,255,255), 3)
draw.point((111,28), (255,255,255,230))
atlas.save(Path(__file__).resolve().parents[1] / "src/main/resources/assets/witchersbestiary/textures/effect/sign_pixels.png")
