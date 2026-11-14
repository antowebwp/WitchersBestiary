"""Build the Crimson Phylactery model and its two original pixel palettes."""
import json
from pathlib import Path
from PIL import Image

root = Path(__file__).resolve().parents[1] / "src/main/resources/assets/witchersbestiary"
faces = ("north", "south", "east", "west", "up", "down")
elements = []


def box(low, high, material):
    x, y = (material % 4) * 4, (material // 4) * 4
    elements.append({"from": low, "to": high, "faces": {
        face: {"texture": "#metal", "uv": [x, y, x + 4, y + 4]} for face in faces
    }})


box([4, 1, 4], [12, 2.5, 12], 0)
box([5, 2.5, 5], [11, 3.5, 11], 1)
box([5, 12, 5], [11, 13.4, 11], 1)
box([6, 13.4, 6], [10, 14.5, 10], 0)
box([7.3, 14.5, 7.3], [8.7, 16, 8.7], 1)
box([6.6, 14.9, 7.45], [9.4, 15.4, 8.55], 2)
for x in (4.7, 10):
    for z in (4.7, 10):
        box([x, 3.3, z], [x + 1.3, 12.3, z + 1.3], 0)
        box([x, 12.3, z], [x + 1.3, 14, z + 1.3], 1)
for low, high in [(4, 5.2), (10.8, 12)]:
    box([low, 0, 4], [high, 2, 5.4], 1)
    box([low, 0, 10.6], [high, 2, 12], 1)
for y, inset in [(4, .8), (5, .3), (6, 0), (9.5, .3), (10.5, .8)]:
    height = 3.5 if y == 6 else 1
    box([6 + inset, y, 6 + inset], [10 - inset, y + height, 10 - inset], 3)
for z in (4.75, 10.8):
    box([5, 6.1, z], [11, 6.75, z + .45], 1)
    box([7.6, 5.7, z - .05], [8.4, 7.15, z + .5], 2)
for x in (4.8, 10.75):
    box([x, 6.1, 5.3], [x + .45, 6.75, 10.7], 1)
model = {"textures": {"metal": "witchersbestiary:item/crimson_phylactery_empty",
                      "particle": "#metal"}, "elements": elements,
         "display": {slot: {"rotation": rotation, "translation": [0, 0, 0],
                            "scale": [scale] * 3} for slot, rotation, scale in [
            ("gui", [25, 35, 0], .9), ("ground", [0, 0, 0], .45),
            ("fixed", [0, 180, 0], .8), ("firstperson_righthand", [8, -22, 0], .55),
            ("firstperson_lefthand", [8, 22, 0], .55), ("thirdperson_righthand", [0, 0, 0], .45),
            ("thirdperson_lefthand", [0, 0, 0], .45)]},
         "overrides": [{"predicate": {"witchersbestiary:sealed": 1},
                        "model": "witchersbestiary:item/crimson_phylactery_sealed"}]}
for name, sealed in [("empty", False), ("sealed", True)]:
    texture = Image.new("RGB", (32, 32))
    palette = [(49, 39, 49), (113, 96, 104),
               (227, 46, 62) if sealed else (105, 32, 50),
               (235, 30, 56) if sealed else (76, 22, 39)]
    for material, color in enumerate(palette):
        for y in range(8):
            for x in range(8):
                offset = 18 if x == 1 or y == 0 else -19 if x == 7 or y == 7 else (x * 7 + y * 11) % 15 - 7
                texture.putpixel((material * 8 + x, y), tuple(max(0, min(255, c + offset)) for c in color))
    texture.save(root / f"textures/item/crimson_phylactery_{name}.png")
(root / "models/item/crimson_phylactery.json").write_text(json.dumps(model, indent=2) + "\n")
(root / "models/item/crimson_phylactery_sealed.json").write_text(json.dumps({
    "parent": "witchersbestiary:item/crimson_phylactery",
    "textures": {"metal": "witchersbestiary:item/crimson_phylactery_sealed"}
}, indent=2) + "\n")
