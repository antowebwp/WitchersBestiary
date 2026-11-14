import json
from itertools import combinations
from pathlib import Path
from PIL import Image, ImageDraw
root=Path("src/main/resources")
data=root/"data/witchersbestiary"
assets=root/"assets/witchersbestiary"
families=sorted(p.stem.removesuffix("_materials") for p in (data/"tags/item/alchemy").glob("*_materials.json"))
translations={lang:{} for lang in ("en_us","ru_ru")}
ru=dict(zip(("cursed","hybrid","insectoid","necrophage","ogroid","relict","specter","vampire"),
            ("Проклятые","Гибриды","Инсектоиды","Трупоеды","Огроиды","Реликты","Призраки","Вампиры")))
def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
def item(name):
    return {"item":"witchersbestiary:"+name}
for tier in range(1,4):
    for pair in [(family,) for family in families]+list(combinations(families,2)):
        name=f"mutagen_{'_'.join(pair)}_{tier}"
        if len(pair)==2:
            ingredients=[item(f"mutagen_{f}_{tier}") for f in pair]
        elif tier==1:
            ingredients=[{"tag":f"witchersbestiary:alchemy/{pair[0]}_materials"}]*2
        else:
            ingredients=[item(f"mutagen_{pair[0]}_{tier-1}")]*3
        write(data/f"recipe/{name}.json",{"type":"minecraft:crafting_shapeless","ingredients":ingredients,"result":{"id":item(name)["item"]}})
        texture=f"mutagen_{pair[0]}_{tier}" if len(pair)==1 else f"mutagen_fusion_{tier}"
        write(assets/f"models/item/{name}.json",{"parent":"item/generated","textures":{"layer0":"witchersbestiary:item/"+texture}})
        if len(pair)==1 or pair==tuple(families[:2]):
            image=Image.new("RGBA",(16,16))
            draw=ImageDraw.Draw(image)
            color=["#bb394e","#9374ce","#bb874e","#ce4a79","#769cbd","#ba4c26","#9a57de","#df3254"][families.index(pair[0])] if len(pair)==1 else "#cc70d6"
            edge=4-tier
            shape=[(8,edge),(13-edge//2,5),(15-edge,10),(9,15-edge),(4,13),(edge,7)]
            draw.polygon(shape,fill=color,outline="#28202c")
            draw.line([(6,7),(8,5),(10,6)],fill="#ffe2e8",width=1)
            image.save(assets/f"textures/item/{texture}.png")
        key="item.witchersbestiary."+name
        translations["en_us"][key]=f"{['Small','Greater','Superior'][tier-1]} Mutagen · "+" / ".join(f.title() for f in pair)
        translations["ru_ru"][key]=f"{['Малый','Большой','Высший'][tier-1]} мутаген · "+" / ".join(ru[f] for f in pair)
translations["en_us"]["item.witchersbestiary.mutagen.equipped"]="Mutagens equipped: %s/3"
translations["ru_ru"]["item.witchersbestiary.mutagen.equipped"]="Мутагены установлены: %s/3"
for lang,entries in translations.items():
    path=assets/f"lang/{lang}.json"
    original=path.read_text(encoding="utf-8")
    new={k:v for k,v in entries.items() if k not in json.loads(original)}
    if new:
        path.write_text(original.rstrip()[:-1].rstrip()+",\n"+json.dumps(new,ensure_ascii=False,indent=2)[2:-2]+"\n}\n",encoding="utf-8")
