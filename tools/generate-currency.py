"""Deterministic original voxel currency assets; no external source images."""
from pathlib import Path
import json,random
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/witchersbestiary'
for name,base in [('witcher_crown',(187,134,32)),('witcher_wallet',(76,43,29))]:
 rng=random.Random(2101);im=Image.new('RGB',(32,32));pix=im.load()
 for y in range(32):
  for x in range(32):
   n=rng.randrange(-12,13);pix[x,y]=tuple(max(0,min(255,c+n)) for c in base)
 d=ImageDraw.Draw(im)
 if name=='witcher_crown':
  d.rectangle((1,1,30,30),outline=(255,215,101),width=2);d.rectangle((4,4,27,27),outline=(107,68,12));d.polygon([(7,7),(13,11),(16,7),(19,11),(25,7),(23,22),(16,27),(9,22)],fill=(88,57,17),outline=(255,221,121));d.polygon([(10,12),(14,15),(11,17)],fill=(250,221,120));d.polygon([(22,12),(18,15),(21,17)],fill=(250,221,120));d.polygon([(14,20),(18,20),(16,24)],fill=(255,221,121))
 else:
  d.rectangle((1,1,30,30),outline=(139,91,51),width=2)
  for x in range(4,29,4):d.line((x,3,x,5),fill=(184,143,86));d.line((x,27,x,29),fill=(184,143,86))
  d.rectangle((12,3,19,29),fill=(44,25,20));d.rectangle((11,12,20,21),outline=(203,157,67),width=2)
 folder=root/'textures/item';folder.mkdir(parents=True,exist_ok=True);im.save(folder/(name+'.png'))
 def box(a,b):return {'from':a,'to':b,'faces':{side:{'uv':[0,0,16,16],'texture':'#body'} for side in ['north','south','east','west','up','down']}}
 if name=='witcher_crown':elements=[box([a,y,7],[16-a,y+1,9]) for y,a in enumerate([4,3,2,2,2,2,2,2,2,2,3,4],start=2)]
 else:elements=[box([3,2,4],[13,11,12]),box([4,11,5],[12,14,11]),box([6,13,6],[10,15,10]),box([7,6,3],[10,10,4])]
 if name=='witcher_crown':
  for element in elements:
   y=element['from'][1]
   for side in ['north','south']:element['faces'][side]['uv']=[0,(13-y)*16/12,16,(14-y)*16/12]
 model={'textures':{'body':'witchersbestiary:item/'+name,'particle':'witchersbestiary:item/'+name},'elements':elements,'display':{'gui':{'rotation':[15,25,0],'scale':[0.9,0.9,0.9]},'ground':{'translation':[0,2,0],'scale':[0.5,0.5,0.5]},'fixed':{'rotation':[0,180,0]},'thirdperson_righthand':{'rotation':[0,0,0],'scale':[0.6,0.6,0.6]},'firstperson_righthand':{'rotation':[0,-30,0],'translation':[1,2,0],'scale':[0.7,0.7,0.7]}}}
 folder=root/'models/item';folder.mkdir(parents=True,exist_ok=True);(folder/(name+'.json')).write_text(json.dumps(model,indent=2)+'\n')
