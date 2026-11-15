"""Assemble real Minecraft framebuffer captures from runArcaneV2Review."""
from pathlib import Path
import sys
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("build/arcane-fx-v2-final")
WIDTH, HEIGHT, HEADER, GAP = 576, 324, 33, 12
FONT = ImageFont.truetype("segoeui.ttf", 20)


def take(stem, tick):
    path = ROOT / f"{stem}-{tick}.png"
    with Image.open(path) as source:
        source.load()
        if source.size != (960, 540):
            raise ValueError(f"Unexpected framebuffer dimensions: {path}")
        if source.getextrema()[0][0] == source.getextrema()[0][1]:
            raise ValueError(f"Blank framebuffer: {path}")
        return source.convert("RGB").resize((WIDTH, HEIGHT), Image.Resampling.LANCZOS)


def select(stem, preferred):
    for tick in (preferred, preferred - 1, preferred + 1, preferred - 2, preferred + 2):
        if (ROOT / f"{stem}-{tick}.png").exists():
            return take(stem, tick)
    raise FileNotFoundError(f"No framebuffer near {stem}-{preferred}")


def sheet(name, shots, columns):
    rows = (len(shots) + columns - 1) // columns
    canvas = Image.new("RGB", (GAP + columns*(WIDTH+GAP), GAP + rows*(HEADER+HEIGHT+GAP)), "#161619")
    draw = ImageDraw.Draw(canvas)
    for index, (label, stem, tick) in enumerate(shots):
        x = GAP + index % columns * (WIDTH + GAP)
        y = GAP + index // columns * (HEADER + HEIGHT + GAP)
        draw.text((x, y), label, font=FONT, fill="#f5e8d7")
        canvas.paste(select(stem, tick), (x, y + HEADER))
    canvas.save(ROOT / name)


def gif(name, stem, first, last):
    images, delays = [], []
    for tick in range(first, last + 1):
        if (ROOT / f"{stem}-{tick}.png").exists():
            images.append(take(stem, tick))
            delays.append(50)
        elif delays:
            delays[-1] += 50
    if len(images) < 8:
        raise ValueError(f"Too few actual frames: {stem}")
    delays[-1] += 250
    images[0].save(ROOT / name, save_all=True, append_images=images[1:],
                   duration=delays, loop=0, optimize=False, disposal=2)


sheet("signs.png", [
    ("Igni", "igni", 4),
    ("Aard", "aard", 5),
    ("Fire Breath / side", "breath_side", 12),
    ("Fire Breath / 45 degrees", "breath_45", 12),
    ("Fire Breath / wall", "breath_wall", 12),
    ("Quen", "quen", 4),
    ("Yrden", "yrden", 9),
    ("Axii", "axii", 4),
], 2)
sheet("bombs.png", [
    ("Grapeshot", "grapeshot", 4),
    ("Moon Dust", "moon_dust", 12),
    ("Samum", "samum", 2),
    ("Dragon's Dream", "gas", 27),
], 2)
gif("fire-breath-motion.gif", "breath_motion", 0, 61)
gif("grapeshot.gif", "grapeshot", 0, 26)
gif("dragons-dream-ignition.gif", "gas", 18, 65)
