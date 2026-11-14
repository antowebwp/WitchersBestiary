"""Generate the school templates and family-based alchemy recipe alternatives."""
import json
from pathlib import Path
from PIL import Image, ImageDraw
p = Path("src/main/resources")
data = p / "data/witchersbestiary"
assets = p / "assets/witchersbestiary"

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

def item(name):
    return {"item": name if ":" in name else "witchersbestiary:" + name}

families = {
    "necrophage": "ghoul_claw drowner_claw necrophage_remains ghoul_essence drowner_essence foglet_essence",
    "specter": "wraith_remains noonwraith_remains wraith_essence noonwraith_essence",
    "vampire": "vampire_fang katakan_claw katakan_essence",
    "relict": "fiend_horn forest_essence ancient_leshen_heart",
    "insectoid": "chitin_plate venom_gland endrega_essence",
    "hybrid": "griffin_feather griffin_claw hybrid_essence",
    "ogroid": "ogroid_fang nekker_claw ogroid_essence",
    "cursed": "werewolf_claw werewolf_essence",
}
for family, materials in families.items():
    tag = f"alchemy/{family}_materials"
    write(data / f"tags/item/{tag}.json", {"values": [item(x)["item"] for x in materials.split()]})
    for product in ("oil", "elixir"):
        name = f"{family}_{product}"
        original = data / f"recipe/{name}.json"
        if original.exists():
            recipe = json.loads(original.read_text(encoding="utf-8"))
            recipe["ingredients"][0] = {"tag": f"witchersbestiary:{tag}"}
            write(data / f"recipe/{name}_from_monsters.json", recipe)

languages = {lang: json.loads((assets / f"lang/{lang}.json").read_text(encoding="utf-8"))
             for lang in ("en_us", "ru_ru")}
schools = [
    ("cat", "Кота", "#ae8fcc", "venom_gland", [(4,4),(6,7),(10,7),(12,4),(12,10),(8,13),(4,10),(4,4)]),
    ("griffin", "Грифона", "#e4bd65", "griffin_feather", [(3,9),(7,4),(12,5),(9,7),(13,8),(9,9),(7,13),(6,10),(3,9)]),
    ("bear", "Медведя", "#ce7864", "ogroid_fang", [(4,7),(4,4),(7,6),(9,6),(12,4),(12,10),(9,13),(7,13),(4,10),(4,7)]),
]
for school, russian, color, material, symbol in schools:
    for tier in ("enhanced", "mastercrafted"):
        name = f"{school}_{tier}_template"
        for recipe_path in sorted((data / "recipe").glob(f"{school}_{tier}_*.json")):
            recipe = json.loads(recipe_path.read_text(encoding="utf-8"))
            if recipe["type"] == "minecraft:smithing_transform":
                recipe["template"] = item(name)
                write(recipe_path, recipe)
        write(data / f"recipe/copy_{name}.json", {
            "type": "minecraft:crafting_shaped",
            "pattern": ["DMD", "DTD", "DMD"],
            "key": {"D": item("minecraft:diamond"), "M": item(material), "T": item(name)},
            "result": {"id": item(name)["item"], "count": 2},
        })
        write(assets / f"models/item/{name}.json", {
            "parent": "item/generated", "textures": {"layer0": "witchersbestiary:item/" + name},
        })
        im = Image.new("RGBA", (16,16))
        draw = ImageDraw.Draw(im)
        draw.rectangle((2,1,13,15), fill="#1b1921")
        draw.rectangle((3,2,12,14), fill="#463a44", outline=color)
        draw.line(symbol, fill=color, width=1)
        if tier == "mastercrafted":
            for x, y in ((3,2),(12,2),(3,14),(12,14)):
                draw.point((x,y), fill="#fff2bd")
        im.save(assets / f"textures/item/{name}.png")
        key = "item.witchersbestiary." + name
        languages["en_us"][key] = f"{school.title()} {'Mastercrafted' if tier == 'mastercrafted' else 'Upgrade'} Template"
        languages["ru_ru"][key] = f"Шаблон {'мастерского улучшения' if tier == 'mastercrafted' else 'улучшения'} школы {russian}"
for lang, entries in languages.items():
    path = assets / f"lang/{lang}.json"
    original = path.read_text(encoding="utf-8")
    old = json.loads(original)
    new = {k: v for k, v in entries.items() if k not in old}
    if new:
        path.write_text(original.rstrip()[:-1].rstrip() + ",\n" +
                        json.dumps(new, ensure_ascii=False, indent=2)[2:-2] + "\n}\n", encoding="utf-8")
