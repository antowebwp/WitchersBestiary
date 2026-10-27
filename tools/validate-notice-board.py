"""Focused resource validation; does not require private-history asset tools."""
from pathlib import Path
import json
from PIL import Image
root=Path(__file__).resolve().parents[1];assets=root/'src/main/resources/assets/witchersbestiary'
model=json.loads((assets/'models/block/notice_board.json').read_text());elements=model['elements']
assert len(elements)>100
for e in elements:
 assert all(e['to'][i]>e['from'][i] for i in range(3)),e['name']
 assert all(-16<=v<=32 for p in ['from','to'] for v in e[p]),e['name']
 assert set(e['faces'])==set(['north','south','east','west','up','down'])
 for face in e['faces'].values():
  u,v,U,V=face['uv'];assert 0<=u<U<=16 and 0<=v<V<=16
for mode in ['gui','firstperson_righthand','firstperson_lefthand','thirdperson_righthand','thirdperson_lefthand','ground','fixed']:assert mode in model['display']
assert Image.open(assets/'textures/block/notice_board.png').size==(256,256)
assert Image.open(assets/'textures/gui/notice_board.png').size==(384,240)
variants=json.loads((assets/'blockstates/notice_board.json').read_text())['variants'];assert len(variants)==36
bb=json.loads((root/'art_source/notice_board/notice_board.bbmodel').read_text());assert len(bb['elements'])==len(elements)
assert json.loads((assets/'models/item/notice_board.json').read_text())['parent']=='witchersbestiary:block/notice_board'
print('PASS:',len(elements),'positive-volume cuboids; complete faces/UVs, texture sizes, display contexts, 36 states, editable source')
