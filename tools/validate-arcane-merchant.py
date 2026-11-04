from pathlib import Path
import json,math
from PIL import Image
r=Path(__file__).resolve().parents[1]/'src/main/resources/assets/witchersbestiary'
g=json.loads((r/'geo/arcane_merchant.geo.json').read_text())['minecraft:geometry'][0];names={b['name'] for b in g['bones']};assert len(names)==len(g['bones'])
assert Image.open(r/'textures/entity/arcane_merchant.png').size==(256,256)
count=0
for b in g['bones']:
 assert b.get('parent','root') in names
 for c in b.get('cubes',[]):
  assert all(n>0 for n in c['size'])
  u,v=c['uv'];x,y,z=c['size'];assert min(u,v)>=0 and u+2*(x+z)<=256 and v+y+z<=256
  assert all(math.isfinite(n) for n in c['origin']+c['size']);count+=1
clips=json.loads((r/'animations/arcane_merchant.animation.json').read_text())['animations']
assert set(clips)=={'animation.merchant.idle','animation.merchant.walk','animation.merchant.trade'}
for clip in clips.values():
 assert clip['animation_length']>0
 assert all(b in names for b in clip['bones'])
print(f'Arcane Merchant PASS: {count} volumes, bounded UVs/atlas, parent bones and three clips')
