"""Generate original pixel atlases, inventory icons and survival recipes for school armor."""
from pathlib import Path
import json
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1] / 'src/main/resources'
assets = root / 'assets/witchersbestiary'
recipes = root / 'data/witchersbestiary/recipe'
schools = {
    'griffin': ('Грифона', '#302b29', '#344252', '#d1a153', 'griffin_feather'),
    'bear': ('Медведя', '#2c2523', '#4a3435', '#ba9c7e', 'fiend_horn'),
    'cat': ('Кота', '#231e23', '#5c2e36', '#a09a91', 'nekker_claw'),
}
tiers = ('', 'enhanced_', 'mastercrafted_')
parts = {
    'helmet': ['SSS', 'LTL'],
    'chestplate': ['L L', 'STS', 'SSS'],
    'leggings': ['SSS', 'LTL', 'S S'],
    'boots': ['L L', 'STS'],
}

def write(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(content, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')

def shade(color, factor):
    return tuple(min(255,round(int(color[i:i+2],16)*factor)) for i in (1,3,5))

def save(path, image):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)

translations = {'en_us': {}, 'ru_ru': {}}
for school, (ru, leather, cloth, trim, trophy) in schools.items():
    details=Image.new('RGBA',(64,64),shade(leather,1.25))
    d=ImageDraw.Draw(details)
    for y in range(64):
        for x in range(64):
            if (x*7+y*13+x*y)%17<7:d.point((x,y),fill=shade(cloth,1.25))
    for y in (0,4,10,15,20,26,30):
        d.line((0,y,63,y),fill=trim)
    for x in (3,8,13,27,32,37):
        d.point((x,27),fill='#e3d7c3')
    save(assets/f'textures/models/armor/{school}_details.png',details)
    for level, tier in enumerate(tiers):
        prefix = f'{school}_{tier}'
        for layer in (1, 2):
            image = Image.new('RGBA', (64, 32))
            draw = ImageDraw.Draw(image)
            # Vanilla cuboid UV nets: head, body, arms and legs. Transparent space preserves the face.
            regions = [(0,16,16,32),(16,16,40,32),(40,16,56,32)]
            if layer == 1:
                regions += [(0,0,32,8),(0,8,8,16),(16,8,32,16)]
            for x,y,x2,y2 in regions:
                draw.rectangle((x,y,x2-1,y2-1),fill=leather)
                for yy in range(y,y2):
                    for xx in range(x,x2):
                        grain=((xx*17+yy*31+xx*yy*3)%13-6)/50
                        draw.point((xx,yy),fill=shade(leather,1.32+grain))
            for x,y,x2,y2 in [(20,20,27,31),(32,20,39,31),(4,23,7,29),(44,21,47,28)]:
                draw.rectangle((x,y,x2,y2),fill=cloth)
                draw.line((x,y,x2,y),fill=trim)
                if level: draw.line((x,y2,x2,y2),fill=trim)
                if level==2: draw.point((x+1,y+1),fill='#eee4c8')
            # Paint separate surfaces inside the vanilla UV nets.
            steel={'griffin':'#63717c','bear':'#56565e','cat':'#4a4448'}[school]
            for ox in (40,48):
                for yy in range(20,32):
                    for xx in range(ox,ox+4):
                        if school=='griffin' and 23<=yy<=26:
                            color=shade(steel,1.3 if (xx+yy)%2 else .65)
                        elif school=='bear' and yy in (20,21,27):
                            color=shade(trim,1.15 if (xx+yy)%3 else .78)
                        elif yy in (20,21,22,27,28):
                            color=shade(steel,1.2 if yy in (20,27) else .88)
                        else: continue
                        draw.point((xx,yy),fill=color)
                draw.point((ox+1,22),fill=trim)
            for ox in (20,32):
                draw.line((ox,28,ox+7,28),fill='#684830')
                draw.line((ox,29,ox+7,29),fill='#3d2922')
                draw.rectangle((ox+3,28,ox+4,29),outline=trim)
                for yy in range(21,27):
                    xx=ox+(yy-21 if ox==20 else 6-(yy-21))
                    draw.point((xx,yy),fill='#815a3a')
            for ox in (4,12):
                draw.line((ox,30,ox+3,30),fill=steel)
                draw.point((ox+1,27),fill=trim)
            if layer==1:
                if school=='griffin':
                    draw.line((21,22,26,27),fill=trim);draw.line((26,22,21,27),fill=trim)
                    draw.line((23,21,23,28),fill=trim);draw.line((21,24,26,24),fill=trim)
                elif school=='bear':
                    for xx in range(20,28):draw.line((xx,20,xx,21+xx%3),fill=trim)
                    draw.polygon([(21,24),(23,22),(26,24),(24,27)],fill=trim)
                else:
                    for xx in range(21,25,2):draw.line((xx,27,xx+2,23),fill=trim)
            save(assets/f'textures/models/armor/{prefix}armor_layer_{layer}.png',image)
        for part, pattern in parts.items():
            item=prefix+part
            key=f'item.witchersbestiary.{item}'
            translations['en_us'][key]=f'{["","Enhanced ","Mastercrafted "][level]}{school.title()} {part.title()}'
            translations['ru_ru'][key]=f'{["","Улучшенный комплект · ","Мастерский комплект · "][level]}{dict(helmet="Шлем",chestplate="Нагрудник",leggings="Поножи",boots="Сапоги")[part]} школы {ru}'
            icon=Image.new('RGBA',(16,16));d=ImageDraw.Draw(icon)
            for y,row in enumerate(pattern):
                for x,symbol in enumerate(row):
                    if symbol!=' ':
                        d.rectangle((2+x*4,2+y*4,5+x*4,5+y*4),fill=trim if symbol=='T' else cloth if symbol=='S' else leather)
                        if level:d.point((3+x*4,2+y*4),fill=trim)
            save(assets/f'textures/item/{item}.png',icon)
            write(assets/f'models/item/{item}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'witchersbestiary:item/{item}'}})
            if not level:
                recipe={'type':'minecraft:crafting_shaped','pattern':pattern,'key':{
                    'L':{'item':'minecraft:leather'},'S':{'item':'minecraft:iron_ingot'},'T':{'item':f'witchersbestiary:{trophy}'}},
                    'result':{'id':f'witchersbestiary:{item}'}}
            else:
                recipe={'type':'minecraft:smithing_transform','template':{'item':'minecraft:netherite_upgrade_smithing_template'},
                    'base':{'item':f'witchersbestiary:{school}_{tiers[level-1]}{part}'},
                    'addition':{'item':'witchersbestiary:'+('gravebound_steel_fragment' if level==2 else 'venom_gland')},
                    'result':{'id':f'witchersbestiary:{item}'}}
            write(recipes/f'{item}.json',recipe)
for language, values in translations.items():
    path=assets/f'lang/{language}.json'
    text=path.read_text(encoding='utf-8')
    current=json.loads(text)
    additions={key:value for key,value in values.items() if key not in current}
    if additions:
        path.write_text(text[:text.rfind('}')].rstrip()+',\n'+json.dumps(additions,indent=2,ensure_ascii=False)[2:]+'\n',encoding='utf-8')
