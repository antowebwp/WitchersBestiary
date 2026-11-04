"""Original static volumetric medieval duel altar. Deterministic editable source; no reference images."""
from pathlib import Path
import json,random
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/witchersbestiary'
rng=random.Random(2104)
for material,color in [('stone',(70,68,65)),('slab',(90,86,80)),('metal',(108,99,86)),('carving',(57,54,51))]:
 im=Image.new('RGB',(32,32),color);d=ImageDraw.Draw(im)
 for y in range(32):
  for x in range(32):im.putpixel((x,y),tuple(max(0,min(255,c+rng.randint(-9,9))) for c in color))
 d.line((0,0,31,0),fill=tuple(min(255,c+25) for c in color));d.line((0,0,0,31),fill=tuple(min(255,c+18) for c in color));d.line((0,31,31,31),fill=(35,32,29));d.line((31,0,31,31),fill=(35,32,29))
 if material=='carving':
  # Two knuckled fists face inward, engraved in the top slab rather than a flat sign.
  for flip in [False,True]:
   def point(x,y):return (31-x if flip else x,y)
   for j in range(4):
    x=4+j*2;d.rectangle((point(x,9),point(x+1,15)) if not flip else (point(x+1,9),point(x,15)),fill=(135,126,111))
   d.rectangle((4 if not flip else 19,15,12 if not flip else 27,22),fill=(116,106,93));d.line((6 if not flip else 25,22,6 if not flip else 25,25),fill=(134,119,99),width=3)
  for x in [2,29]:d.line((x,4,x,27),fill=(116,39,30));d.line((4,3,14,3),fill=(132,43,31));d.line((18,28,27,28),fill=(132,43,31))
 im.save(root/f'textures/block/duel_{material}.png')
elements=[]
def box(a,b,material):
 elements.append({'from':a,'to':b,'faces':{face:{'texture':'#'+material,'uv':[0,0,16,16]} for face in ['north','south','east','west','up','down']}})
box([0,0,0],[16,3,16],'stone');box([1,3,1],[15,6,15],'slab');box([3,6,3],[13,10,13],'stone');box([.5,10,.5],[15.5,13,15.5],'slab');box([1,13,1],[15,14,15],'stone');box([3,14,3],[13,14.2,13],'carving')
for x in [2,12.5]:
 for z in [2,12.5]:box([x,9.5,z],[x+1.5,13.5,z+1.5],'metal')
for x in [3,7,11]:box([x,3.2,.8],[x+2,5.7,1.2],'stone');box([x,3.2,14.8],[x+2,5.7,15.2],'stone')
model={'parent':'minecraft:block/block','textures':{m:'witchersbestiary:block/duel_'+m for m in ['stone','slab','metal','carving']},'elements':elements,'display':{'gui':{'rotation':[30,225,0],'translation':[0,0,0],'scale':[.65,.65,.65]},'ground':{'translation':[0,2,0],'scale':[.3,.3,.3]},'fixed':{'rotation':[0,180,0],'scale':[.6,.6,.6]}}}
(root/'models/block/duel_stone.json').write_text(json.dumps(model,indent=2)+'\n');(root/'models/item/duel_stone.json').write_text('{"parent":"witchersbestiary:block/duel_stone"}\n');(root/'blockstates/duel_stone.json').write_text('{"variants":{"":{"model":"witchersbestiary:block/duel_stone"}}}\n')
data=root.parents[1]/'data/witchersbestiary'
(data/'recipe/duel_stone.json').write_text(json.dumps({'type':'minecraft:crafting_shaped','category':'building','pattern':['III','BSB','BBB'],'key':{'I':{'item':'minecraft:iron_ingot'},'B':{'item':'minecraft:stone_bricks'},'S':{'item':'minecraft:smooth_stone'}},'result':{'id':'witchersbestiary:duel_stone','count':1}},indent=2)+'\n')
(data/'loot_table/blocks/duel_stone.json').parent.mkdir(parents=True,exist_ok=True)
(data/'loot_table/blocks/duel_stone.json').write_text(json.dumps({'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'witchersbestiary:duel_stone'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]},indent=2)+'\n')
for file in ['src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json']:
 p=Path(__file__).resolve().parents[1]/file;d=json.loads(p.read_text()) if p.exists() else {'replace':False,'values':[]}
 if 'witchersbestiary:duel_stone' not in d['values']:d['values'].append('witchersbestiary:duel_stone')
 p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,indent=2)+'\n')
