"""Assemble the Stage 22U framebuffer captures."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1] / "build" / "arcane-fx-v2-abilities"
LABELS = {
    "igni": ("Fire Cone", "Fireburst", "Fire Breath", "Ground Fire Wave"),
    "aard": ("Shock Cone", "Ring Blast", "Charged Thrust", "Vortex"),
    "quen": ("Shield", "Shield Burst", "Reflection", "Protection Aura"),
    "yrden": ("Trap", "Binding Trap", "Three Traps", "Pulsing Trap"),
    "axii": ("Calm", "Mass Confusion", "Domination", "Redirect Aggression"),
}
ANGLES = (("front", "Front"), ("side", "Side"), ("45", "45 degrees"))
ANIMATIONS = {
    "fireburst": "igni-1",
    "yrden-pulse": "yrden-3",
    "quen-reflection": "quen-2",
    "axii-redirect": "axii-3",
}


def frame(path):
    with Image.open(path) as image:
        if image.width < 400 or image.height < 240 or not image.getbbox():
            raise ValueError(path)
        return image.convert("RGB")


def sheet(sign, names):
    width, height = 520, 292
    board = Image.new("RGB", (3 * width, len(names) * (height + 34) + 30), "#1d1b20")
    draw = ImageDraw.Draw(board)
    font = ImageFont.load_default(size=18)
    for column, (_, label) in enumerate(ANGLES):
        draw.text((column * width + 12, 5), label, fill="#e5c98d", font=font)
    for form, name in enumerate(names):
        row = 30 + form * (height + 34)
        draw.text((12, row + 7), name, fill="#f0ece1", font=font)
        for column, (key, _) in enumerate(ANGLES):
            captured = frame(ROOT / f"{sign}-{form}-{key}.png")
            tile = captured.resize((width, height), Image.Resampling.LANCZOS)
            board.paste(tile, (column * width, row + 34))
    board.save(ROOT / f"{sign}-abilities.png")


def animation(name, prefix):
    paths = sorted(ROOT.glob(f"{prefix}-frame-*.png"))
    if len(paths) < 4:
        raise ValueError(f"Too few frames for {name}: {len(paths)}")
    frames = []
    for path in paths:
        image = frame(path)
        image.thumbnail((624, 351), Image.Resampling.LANCZOS)
        frames.append(image)
    frames[0].save(ROOT / f"{name}.gif", save_all=True, append_images=frames[1:], duration=65, loop=0)


if __name__ == "__main__":
    for key, form_names in LABELS.items():
        sheet(key, form_names)
    for name, capture in ANIMATIONS.items():
        animation(name, capture)
