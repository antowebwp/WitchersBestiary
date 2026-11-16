"""Generate original unicorn armor atlas sections."""
from pathlib import Path
import json
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/witchersbestiary"
model_path = ROOT / "geo/unicorn.geo.json"
model = json.loads(model_path.read_text())
bones = model["minecraft:geometry"][0]["bones"]
cubes = [
    {"origin": [-5.6, 14, -8.3], "size": [11.2, 8, 17.4], "uv": [0, 128]},
    {"origin": [-5.9, 18.2, -9], "size": [11.8, 2.2, 6.2], "uv": [66, 128]},
    {"origin": [-5.9, 16, 4.8], "size": [11.8, 5, 4.4], "uv": [110, 128]},
    {"origin": [-6.1, 15, -7.1], "size": [1.05, 5.1, 10.5], "uv": [152, 128]},
    {"origin": [5.05, 15, -7.1], "size": [1.05, 5.1, 10.5], "uv": [184, 128]},
]
bones[:] = [bone for bone in bones if bone["name"] != "armor_shell"]
bones.append({"name": "armor_shell", "parent": "body", "pivot": [0, 20.16, 0], "cubes": cubes})
model_path.write_text(json.dumps(model, indent=2) + "\n")
base = Image.open(ROOT / "textures/entity/unicorn.png").convert("RGBA")
materials = {
    "leather": ((119, 74, 44), (174, 114, 66), (71, 45, 34)),
    "iron": ((136, 151, 160), (215, 224, 225), (70, 86, 97)),
    "gold": ((173, 107, 32), (247, 199, 91), (101, 62, 29)),
    "diamond": ((45, 156, 155), (120, 231, 223), (29, 96, 109)),
}
for name, (body, rim, shadow) in materials.items():
    atlas = base.copy()
    draw = ImageDraw.Draw(atlas)
    for cube in cubes:
        x, y = cube["uv"]
        w, h, depth = [int(dim + .99999) for dim in cube["size"]]
        right, bottom = x + 2 * (w + depth), y + h + depth
        draw.rectangle((x,y,right,bottom),fill=shadow)
        draw.rectangle((x+1,y+1,right-1,bottom-1),fill=body)
        draw.line((x+1,y+1,right-1,y+1),fill=rim)
        draw.line((x+1,y+depth,right-1,y+depth),fill=rim)
        for i in range(x+4,right-1,5):
            draw.point((i,y+depth+1),fill=rim)
            draw.point((i,bottom-2),fill=shadow)
    atlas.save(ROOT / f"textures/entity/unicorn_armor_{name}.png")
