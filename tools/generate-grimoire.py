"""Original 3D leather grimoire and pixel atlas; no external artwork."""
from pathlib import Path
from PIL import Image,ImageDraw
import json,random
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/witchersbestiary'
rng=random.Random(211);im=Image.new('RGBA',(64,64),(38,23,47,255));d=ImageDraw.Draw(im)
for y in range(32):
 for x in range(32):
  v=rng.randrange(-6,7);im.putpixel((x,y),(53+v,29+v,58+v,255))
d.rectangle((1,1,30,30),outline=(133,107,65),width=1);d.rectangle((3,3,28,28),outline=(89,62,75),width=1)
# Etched six-point arcane seal, deliberately distinct from the wolf emblem.
d.line([(16,6),(25,23),(7,23),(16,6)],fill=(120,187,235),width=1);d.line([(16,26),(7,10),(25,10),(16,26)],fill=(159,130,217),width=1);d.ellipse((12,12,20,20),outline=(189,219,241))
d.rectangle((32,0,47,15),fill=(150,130,90));d.rectangle((33,1,46,14),outline=(211,192,137));d.rectangle((48,0,63,15),fill=(37,23,43))
d.rectangle((0,32,31,63),fill=(180,166,134))
for y in range(33,64,3):d.line((0,y,31,y),fill=(120,105,84))
im.save(root/'textures/item/grimoire.png')
faces=['north','south','east','west','up','down'];elements=[]
def cube(name,a,b,uv):elements.append({'name':name,'from':a,'to':b,'faces':{f:{'uv':uv,'texture':'#atlas'} for f in faces}})
cube('lower_leather_cover',[2,4,2],[14,4.8,14],[0,0,8,8]);cube('aged_pages',[2.6,4.8,2.6],[13.7,6.5,13.5],[0,8,8,16]);cube('upper_leather_cover',[2,6.5,2],[14,7.3,14],[0,0,8,8]);cube('stitched_spine',[1.8,4.7,2],[2.8,6.6,14],[12,0,16,4])
for x in [2.1,12.5]:
 for z in [2.1,12.5]:cube('metal_corner',[x,7.31,z],[x+1.4,7.6,z+1.4],[8,0,12,4])
# Low raised metal centre, no coplanar overlay with the engraved cover.
cube('arcane_seal',[7.5,7.31,7.5],[8.5,7.6,8.5],[8,0,12,4])
reference=json.loads((root/'models/item/bestiary.json').read_text(encoding='utf-8'))
model={'textures':{'atlas':'witchersbestiary:item/grimoire','particle':'witchersbestiary:item/grimoire'},'elements':elements,'display':reference['display'],'ambientocclusion':False}
(root/'models/item/grimoire.json').write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
for e in elements:assert all(b>a for a,b in zip(e['from'],e['to']))
print('Original grimoire: 9 positive-volume elements, 64px atlas.')
