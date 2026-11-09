"""Build the original stepped unicorn geometry and its pixel atlas."""
import json
import math
import random
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/witchersbestiary"
SIZE = 256
# Uniform growth preserves the angles of the horn, face and stepped locks.
SCALE = 1.12
atlas = Image.new("RGBA", (SIZE, SIZE))
glow = Image.new("RGBA", (SIZE, SIZE))
rng = random.Random(22001)
bones = []
x = y = height = 0
palette = {
    "coat": (254, 248, 253), "muzzle": (202, 181, 213), "hoof": (140, 113, 158),
    "rose": (254, 209, 241), "lilac": (213, 170, 249), "ice": (204, 232, 251),
    "pearl": (255, 245, 253), "gold": (255, 228, 178), "eye": (119, 62, 179),
    "leather": (114, 78, 106), "binding": (154, 107, 134), "lash": (44, 29, 59), "glint": (255, 252, 255), "ear": (224, 173, 200),
}

def bone(name, pivot, parent=None):
    result = dict(name=name, pivot=[round(value * SCALE, 5) for value in pivot], cubes=[])
    if parent:
        result["parent"] = parent
    bones.append(result)
    return result

def cube(b, origin, size, color, rotation=None, pivot=None):
    global x, y, height
    origin = [round(value * SCALE, 5) for value in origin]
    size = [round(value * SCALE, 5) for value in size]
    w, h, d = [math.ceil(v) for v in size]
    width, depth = 2*(w+d), h+d
    if x + width > SIZE:
        x, y, height = 0, y+height+1, 0
    assert y+depth <= SIZE
    a = dict(origin=origin, size=size, uv=[x, y])
    if rotation:
        a.update(rotation=rotation, pivot=[round(value * SCALE, 5) for value in pivot])
    b["cubes"].append(a)
    base = palette[color]
    for yy in range(depth):
        for xx in range(width):
            tint = rng.choice([(0,0,0)]*9 + [(-7,-12,0),(-12,0,2),(3,-8,-4)]) if color=="coat" else (0,0,0)
            noise = rng.choice([-5,-2,0,0,3,6]) if color not in ("eye","glint","lash") else 0
            atlas.putpixel((x+xx,y+yy),tuple(max(0,min(255,k+t+noise)) for k,t in zip(base,tint))+(255,))
    if b["name"] == "horn":
        area = (x, y, x+width, y+depth)
        glow.paste(atlas.crop(area), (x,y))
    x += width+1
    height = max(height,depth)

body=bone("body",[0,18,0])
cube(body,[-4.62,12.35,-8],[9.24,9.65,17],"coat")
cube(body,[-4.34,11.85,-9.2],[8.68,9.9,4.4],"coat")
cube(body,[-4.39,12.44,6.08],[8.78,9.31,4.35],"coat")
neck=bone("neck",[0,18,-6],"body")
cube(neck,[-2.72,17.7,-10.14],[5.44,11.3,6.03],"coat",[17,0,0],[0,18,-6])
head=bone("head",[0,28,-9],"neck")
cube(head,[-2.55,25,-13],[5.1,5.9,5.5],"coat")
cube(head,[-2.16,25.05,-17.2],[4.32,3.7,5.8],"coat",[16,0,0],[0,27,-12])
cube(head,[-2.09,25.13,-17.58],[4.18,3.46,.64],"muzzle",[16,0,0],[0,27,-12])
for side in (-1,1):
    sx=side*1.49
    cube(head,[sx-.55,30.45,-9.65],[1.1,3.1,1.15],"coat",[10,0,side*-12],[sx,30.45,-9])
    cube(head,[sx-.31,30.95,-9.82],[.62,1.95,.25],"ear",[10,0,side*-12],[sx,30.45,-9])
    cube(head,[2.55 if side>0 else -2.73,27.75,-12.62],[.18,1.63,1.77],"lash")
    cube(head,[2.73 if side>0 else -2.83,27.81,-12.42],[.10,1.27,1.28],"eye")
    cube(head,[2.83 if side>0 else -2.91,28.59,-12.34],[.08,.37,.45],"glint")
    cube(head,[side*1.21-.32,26.46,-17.69],[.64,.56,.14],"lash",[16,0,0],[0,27,-12])
horn=bone("horn",[0,29.9,-13.1],"head")
horn["rotation"]=[32,0,0]
for i in range(9):
    width=1.92-i*.151
    y_base=29.9+i*1.36
    cube(horn,[-width/2,y_base,-13.1-width/2],[width,1.40,width],
         ("gold","pearl","lilac","pearl","rose")[i%5],[0,i*23,0],[0,y_base,-13.1])

mane=bone("mane",[0,29,-7],"neck")
for i in range(7):
    cube(mane,[-1.8+i*.021,30.65-i*1.42,-8.3+i*.49],[3.6-i*.042,1.71,4.6],("pearl","lilac","rose","ice")[i%4])
for side in (-1,1):
    for lock in range(3):
        for step in range(5+lock%2):
            sx=side*(2.38+step*.21+lock*.26)
            cube(mane,[sx-.69,29.43-lock*1.82-step*1.69,-9.4+lock*1.84+step*.33],[1.38,1.92,2.38],("lilac","rose","ice")[lock] if step else "pearl")
cube(head,[-1.25,29.85,-12.9],[2.55,1.1,3.8],"pearl")
cube(head,[.63,28.65,-12.6],[1.5,1.9,1.1],"lilac",[0,0,-24],[1.3,30,-12])

tail=bone("tail",[0,20,9],"body")
for lock in range(5):
    for step in range(7+(lock%2)):
        sx=(lock-2)*1.06+(step*.15 if lock>=2 else -step*.15)
        cube(tail,[sx-.74,20.4-step*2.44-(lock%2)*.67,9.2+min(step,5)*.97+lock*.43+step*.037],[1.48,2.71,3.22],("lilac","rose","pearl","ice","lilac")[lock] if step else "pearl")
for name,sx,z in [("front_l",-2.96,-6.55),("front_r",2.96,-6.55),("hind_l",-2.96,6.54),("hind_r",2.96,6.54)]:
    leg=bone(name,[sx,15,z],"body")
    # Keep the cannon and hoof on a shared vertical axis in the resting pose.
    cube(leg,[sx-1.42,7.05,z-1.45],[2.84,8.52,2.9],"coat")
    cube(leg,[sx-1.15,1.22,z-1.16],[2.3,6.18,2.32],"coat")
    cube(leg,[sx-1.32,0,z-1.37],[2.64,1.72,2.74],"hoof")
    cube(leg,[sx-1.27,1.65,z-1.29],[2.54,.64,2.58],"pearl")

saddle=bone("saddle",[0,22,0],"body")
cube(saddle,[-4.74,21.83,-2.9],[9.48,.5,6.75],"lilac")
cube(saddle,[-3.35,22.29,-2.46],[6.7,.55,5.8],"leather")
cube(saddle,[-3.22,22.72,-2.68],[6.44,.65,1.1],"binding")
cube(saddle,[-3.18,22.77,2.48],[6.36,.58,1.11],"binding")
for side in (-1,1):
    cube(saddle,[side*4.69-.19,17.15,-1.63],[.38,4.8,2.0],"leather")
    cube(saddle,[side*4.87-.17,16.8,-1.24],[.34,1.0,1.04],"gold")

for part in saddle["cubes"]:
    part["origin"][2] += 2.24 * SCALE

def write(name,obj):
    path=ROOT/name
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(obj,indent=2)+"\n",encoding="utf-8")
write("geo/unicorn.geo.json",{"format_version":"1.12.0","minecraft:geometry":[{"description":{"identifier":"geometry.witchersbestiary.unicorn","texture_width":SIZE,"texture_height":SIZE,"visible_bounds_width":4,"visible_bounds_height":4,"visible_bounds_offset":[0,1.57,0]},"bones":bones}]})
path=ROOT/"textures/entity/unicorn.png"
path.parent.mkdir(parents=True,exist_ok=True)
atlas.save(path)
glow.save(path.with_name("unicorn_glowmask.png"))
clips={}
def clip(name,length,tracks,loop=True):
    clips["animation.unicorn."+name]=dict(loop=loop,animation_length=length,bones=tracks)
def rot(keys): return {"rotation":{str(t):v for t,v in keys}}
clip("idle",3.2,{"head":rot([(0,[0,0,0]),(1.6,[2,3,-1]),(3.2,[0,0,0])]),"body":{"scale":{"0":[1,1,1],"1.6":[1.014,1.013,1],"3.2":[1,1,1]}}})
for gait,length,amplitude in [("walk",1.15,25),("run",.64,43)]:
    tracks={}
    for name,phase in zip(("front_l","front_r","hind_l","hind_r"),(0,.5,.68,.18) if gait=="walk" else (0,.18,.48,.67)):
        keys = []
        for i in range(9):
            swing = math.cos(2 * math.pi * (i/8 + phase))
            # The thicker fore and hind legs need clearance on the inward swing.
            inward = (swing > 0) == name.startswith("front")
            angle = (16 if inward else amplitude) * swing
            keys.append((round(length*i/8,4), [round(angle,2),0,0]))
        tracks[name]=rot(keys)
    tracks["neck"]=rot([(0,[1,0,0]),(length/2,[-4 if gait=="walk" else -8,0,0]),(length,[1,0,0])])
    clip(gait,length,tracks)
clip("hair",2.8,{"mane":rot([(0,[0,0,-1]),(1.4,[2,0,1.5]),(2.8,[0,0,-1])]),"tail":rot([(0,[1,-7,0]),(1.4,[-3,7,0]),(2.8,[1,-7,0])])})
clip("heal",1.25,{"head":rot([(0,[0,0,0]),(.48,[-14,0,0]),(.85,[-14,0,0]),(1.25,[0,0,0])])},False)
clip("tame",1.35,{"head":rot([(0,[0,0,0]),(.45,[12,7,1]),(.85,[-7,-4,0]),(1.35,[0,0,0])])},False)
write("animations/unicorn.animation.json",{"format_version":"1.8.0","animations":clips})
