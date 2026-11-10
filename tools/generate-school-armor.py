"""Generate original pixel atlases, inventory icons and survival recipes for school armor."""
from pathlib import Path
import json
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1] / 'src/main/resources'
assets = root / 'assets/witchersbestiary'
recipes = root / 'data/witchersbestiary/recipe'
schools = {
    'griffin': ('Грифона', '#302b29', '#344252', '#d1a153', 'gold_ingot'),
    'bear': ('Медведя', '#2c2523', '#4a3435', '#ba9c7e', 'chain'),
    'cat': ('Кота', '#231e23', '#5c2e36', '#a09a91', 'string'),
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

# Draw at native resolution. Each outline is independent of its crafting recipe.
def armor_icon(school, part, tier):
    image = Image.new('RGBA', (16, 16))
    d = ImageDraw.Draw(image)
    edge, deep, mid, light, accent, highlight = {
        'bear': ('#211c23','#35313a','#646871','#a9abb0','#ae8965','#e5c59a'),
        'cat': ('#21151d','#392730','#754d48','#a9755d','#b85365','#eaa49c'),
        'griffin': ('#192331','#2b3d54','#526c87','#91a9be','#d4a349','#ffe2a1'),
    }[school]
    heavy = school == 'bear'
    agile = school == 'cat'
    def poly(points, color, border=None):
        d.polygon(points, fill=color, outline=border)
    def line(points, color):
        d.line(points, fill=color)
    if part == 'helmet':
        if heavy:
            poly([(2,4),(4,2),(11,2),(13,4),(13,12),(10,14),(5,14),(2,12)],deep,edge)
            poly([(3,5),(5,3),(10,3),(12,5),(12,7),(3,7)],mid)
            line([(4,5),(6,3),(10,3)],light)
            line([(3,10),(4,12),(5,11),(6,13),(9,13),(10,11),(11,12),(12,10)],accent)
        elif agile:
            poly([(3,6),(5,3),(10,2),(12,5),(12,10),(10,13),(5,13),(3,11)],deep,edge)
            poly([(4,6),(6,4),(10,3),(11,6),(9,5),(5,7)],mid)
            line([(4,11),(7,12),(10,11),(11,10)],accent)
        else:
            poly([(3,4),(6,2),(9,2),(12,4),(12,12),(9,14),(6,14),(3,12)],deep,edge)
            poly([(4,5),(7,3),(8,3),(11,5),(11,7),(4,7)],mid)
            line([(5,5),(7,3),(8,3),(10,5)],light)
            line([(4,7),(7,6),(8,6),(11,7)],accent)
        d.rectangle((5,8,10,9),fill=edge)
        d.point((6,8),fill=light)
        d.point((9,8),fill=light)
    elif part == 'chestplate':
        x = 1 if heavy else 3 if agile else 2
        poly([(x,4),(5,2),(6,4),(9,4),(10,2),(15-x,4),(15-x,8),(11,8),
              (12,13),(10,14),(5,14),(3,13),(4,8),(x,8)],deep,edge)
        poly([(5,5),(10,5),(10,10),(8,12),(5,10)],mid)
        line([(5,6),(7,5),(9,5)],light)
        line([(4,11),(11,11)],edge)
        d.rectangle((7,11,8,12),fill=accent)
        if heavy:
            line([(2,5),(4,4),(5,5),(7,6),(9,5),(11,4),(13,5)],accent)
            line([(2,7),(4,6)],light); line([(11,6),(13,7)],light)
        elif agile:
            line([(4,5),(9,9)],accent);line([(5,3),(7,5),(9,4)],highlight)
        else:
            line([(3,5),(5,4)],accent);line([(10,4),(12,5)],accent)
            line([(7,6),(7,9)],accent);line([(6,7),(8,7)],highlight)
    elif part == 'leggings':
        x = 2 if heavy else 4 if agile else 3
        poly([(x,2),(15-x,2),(15-x,7),(14-x,14),(9,14),(8,8),(7,8),(6,14),(x+1,14),(x,7)],deep,edge)
        line([(x+1,3),(14-x,3)],accent)
        poly([(x+1,5),(7,5),(6,8),(x+1,9)],mid)
        poly([(8,5),(14-x,5),(14-x,9),(9,8)],mid)
        for left,right in ((x+1,6),(9,14-x)):
            line([(left,10),(right,10)],light)
            line([(left,11),(right,11)],mid)
            d.point((left,13),fill=accent if heavy else mid)
        if agile:
            line([(5,6),(6,5)],accent);line([(9,7),(10,6)],accent)
    else:
        # Two shafts and toes, offset so the ankle and sole read independently.
        x = 1 if heavy else 3 if agile else 2
        poly([(x+1,5),(x+5,5),(x+5,10),(x+6,11),(x+6,13),(x,13),(x,11),(x+1,10)],deep,edge)
        poly([(8,3),(12,3),(12,9),(14,10),(14,12),(8,12),(7,11),(8,9)],mid,edge)
        line([(x+2,6),(x+4,6)],accent);line([(9,4),(11,4)],accent)
        line([(x+1,11),(x+4,11)],mid);line([(9,9),(12,10)],light)
        line([(x+1,12),(x+5,12)],light if heavy else mid)
        line([(9,11),(13,11)],light if heavy else deep)
        if agile:
            line([(9,6),(11,6)],accent);d.point((x+3,8),fill=accent)
        elif not heavy:
            line([(9,8),(11,8)],accent)
    if tier:
        # Rank marks sit on the piece and keep the silhouette intact.
        edge_rgb=shade(edge,1)
        for y in range(16):
            xs=[x for x in range(16) if image.getpixel((x,y))[3] and image.getpixel((x,y))[:3] != edge_rgb]
            if len(xs)>=4:
                d.point((xs[-2],y),fill=highlight)
                if tier==2:d.point((xs[-3],y),fill=accent)
                break
    return image


translations = {'en_us': {}, 'ru_ru': {}}
for school, (ru, leather, cloth, trim, trophy) in schools.items():
    details=Image.new('RGBA',(64,96))
    d=ImageDraw.Draw(details)
    steel={'griffin':'#697984','bear':'#535561','cat':'#534a47'}[school]
    lining={'griffin':'#889095','bear':'#a89075','cat':'#673641'}[school]
    for band,color in enumerate((steel,'#654632',cloth,trim,lining,'#52968d')):
        for y in range(16):
            for x in range(64):
                grain=((x*11+y*7+x*y)%11-5)*.028
                edge=.19 if y in (0,1,4,8) else -.15 if y in (7,11,15) else 0
                if band==4 and school=='bear':
                    edge+=.17 if (x//2+y//2)%3==0 else -.1
                if band==4 and school=='griffin':
                    edge+=.22 if (x+y)%2 else -.24
                if band==5 and x%5 in (1,2):
                    edge+=.25
                d.point((x,band*16+y),fill=shade(color,1+grain+edge))
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
            icon=armor_icon(school, part, level)
            save(assets/f'textures/item/{item}.png',icon)
            write(assets/f'models/item/{item}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'witchersbestiary:item/{item}'}})
            if not level:
                recipe={'type':'minecraft:crafting_shaped','pattern':pattern,'key':{
                    'L':{'item':'minecraft:leather'},'S':{'item':'minecraft:iron_ingot'},'T':{'item':f'minecraft:{trophy}'}},
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
