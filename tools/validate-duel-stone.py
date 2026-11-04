from pathlib import Path
import json
from PIL import Image
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/witchersbestiary'
m=json.loads((root/'models/block/duel_stone.json').read_text());assert len(m['elements'])>=10
for e in m['elements']:
 assert all(0<=a<b<=16 for a,b in zip(e['from'],e['to']))
 assert set(e['faces'])=={'north','south','east','west','up','down'}
 for face in e['faces'].values():assert face['texture'][1:] in m['textures'] and all(0<=v<=16 for v in face['uv'])
for texture in m['textures'].values():
 path=root/'textures'/(texture.split(':')[1]+'.png');im=Image.open(path);assert im.size==(32,32)
print('Duel Stone:',len(m['elements']),'bounded volume elements; textures/UV PASS')
