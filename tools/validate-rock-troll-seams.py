"""Targeted coplanar-face and UV check; companion to actual GeckoLib client review."""
from pathlib import Path
import json,struct
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/witchersbestiary'
g=json.loads((root/'geo/rock_troll.geo.json').read_text())['minecraft:geometry'][0];bones={b['name']:b for b in g['bones']}
for name,a,b,axis in [('head',0,1,1),('head',0,1,2),('left_leg',0,2,2),('right_leg',0,2,2)]:
 core=bones[name]['cubes'][a];shell=bones[name]['cubes'][b];gap=shell['origin'][axis]+shell['size'][axis]-core['origin'][axis]-core['size'][axis]
 assert gap>=.20,(name,axis,gap)
 assert not core.get('rotation') and not shell.get('rotation'),'Same moving bone preserves separation under every animation'
for bone in bones.values():
 for c in bone.get('cubes',[]):
  u,v=c['uv'];x,y,z=c['size'];assert x>0 and y>0 and z>0;assert 0<=u and 0<=v and u+2*(x+z)<=512 and v+y+z<=512
assert struct.unpack('>II',(root/'textures/entity/rock_troll.png').read_bytes()[16:24])==(512,512)
print('Four offending planes separated >=0.20 model units; positive volume and512x512 UV bounds PASS. Native views still required.')
