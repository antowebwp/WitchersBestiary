"""Original rock troll geometry and stone atlas; deterministic editable source."""
from pathlib import Path
from PIL import Image,ImageDraw
import json,random
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/witchersbestiary'
bones=[]
def bone(n,p,pivot,cubes):
 bones.append(dict(name=n,**({'parent':p} if p else {}),pivot=pivot,cubes=[dict(origin=o,size=s,uv=u) for o,s,u in cubes]))
bone('root',None,[0,0,0],[])
bone('hips','root',[0,14,0],[([-8,10,-5],[16,7,10],[0,64])])
bone('body','hips',[0,16,0],[([-11,17,-6],[22,15,13],[0,64]),([-9,30,-4],[18,4,10],[0,64])])
bone('head','body',[0,31,-1],[([-7,31,-8],[14,10,12],[0,0]),([-6,30,-10],[12,5,6],[0,0]),([-2,34,-11],[4,4,4],[0,0]),([-6,38,-9],[5,2,2],[64,0]),([1,38,-9],[5,2,2],[64,0])])
for side,sgn in [('left',1),('right',-1)]:
 x=2 if sgn==1 else -9
 bone(side+'_leg','hips',[sgn*5,13,0],[([x,3,-4],[7,10,8],[0,64]),([x-1,0,-7],[9,4,12],[0,64])])
 ax=11 if sgn==1 else -19
 bone(side+'_arm','body',[sgn*12,29,0],[([ax,20,-5],[8,12,10],[0,64])])
 bone(side+'_forearm',side+'_arm',[sgn*15,21,0],[([ax-1,10,-6],[10,11,12],[0,64]),([ax-2,7,-8],[12,5,15],[0,64])])
bone('eyes','head',[0,37,-8],[([-5,36,-9.2],[3,1.5,1],[100,0]),([2,36,-9.2],[3,1.5,1],[100,0])])
for i in range(5):
 bone('back_plate_'+str(i),'body',[0,25,5],[([-10+i*4,18+(i%2)*3,6],[3.5,11,3],[64,64])])
for sgn in [-1,1]:
 bone('shoulder_'+str(sgn),'body',[sgn*12,30,0],[([sgn*12-5,30,-6.4],[10,5,13.8],[64,64])])
geo={'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.rock_troll','texture_width':128,'texture_height':128,'visible_bounds_width':6,'visible_bounds_height':5,'visible_bounds_offset':[0,1.5,0]},'bones':bones}]}
(root/'geo/rock_troll.geo.json').write_text(json.dumps(geo,indent=2)+'\n')
r=random.Random(2103);im=Image.new('RGB',(128,128));pix=im.load()
for y in range(128):
 for x in range(128):
  n=r.randrange(-14,15);moss=y>64 and r.random()<.12
  base=(72,85,51) if moss else (108,107,98)
  pix[x,y]=tuple(max(0,min(255,v+n)) for v in base)
d=ImageDraw.Draw(im)
for i in range(130):
 x=r.randrange(128);y=r.randrange(128);d.line((x,y,x+r.randrange(-4,5),y+r.randrange(2,8)),fill=(65,66,60))
d.rectangle((100,0,110,8),fill=(37,32,22))
im.save(root/'textures/entity/rock_troll.png')
a=json.loads((root/'animations/fighter.animation.json').read_text())
import copy
for name,factor in [('quick',2),('heavy',18/14)]:
 clip=copy.deepcopy(a['animations']['animation.fighter.'+name]);clip['loop']=False;clip['animation_length']*=factor
 for channels in clip['bones'].values():
  for channel,value in list(channels.items()):
   if isinstance(value,dict):channels[channel]={str(round(float(t)*factor,5)):v for t,v in value.items()}
 a['animations']['animation.fighter.wild_'+name]=clip
for k,seconds in [('throw',2),('stomp',2),('alert',1),('death',2)]:
 a['animations']['animation.fighter.'+k]={'animation_length':seconds,'bones':{'body':{'rotation':{'0':[0,0,0],'.6':[-20,0,0],'1.0':[25,0,0],'1.8':[0,0,0]}},'right_arm':{'rotation':{'0':[0,0,0],'.6':[-130,0,0],'1.0':[30,0,0],'1.8':[0,0,0]}}}}
a['animations']['animation.fighter.death']={'animation_length':2,'bones':{'root':{'rotation':{'0':[0,0,0],'1.2':[80,0,0]},'position':{'0':[0,0,0],'1.2':[0,-4,0]}}}}
a['animations']['animation.fighter.stomp']={'animation_length':2,'bones':{'right_leg':{'rotation':{'0':[0,0,0],'.7':[-50,0,0],'1.1':[15,0,0],'1.5':[0,0,0]}},'body':{'rotation':{'0':[0,0,0],'.7':[-12,0,0],'1.1':[15,0,0],'1.7':[0,0,0]}}}}
(root/'animations/rock_troll.animation.json').write_text(json.dumps(a,indent=2)+'\n')
(root/'models/item/rock_troll_spawn_egg.json').write_text('{"parent":"minecraft:item/template_spawn_egg"}\n')
for lang,name in [('en_us','Rock Troll'),('ru_ru','Скальный тролль')]:
 p=root/'lang'/f'{lang}.json';d=json.loads(p.read_text(encoding='utf-8'));d['entity.witchersbestiary.rock_troll']=name;d['item.witchersbestiary.rock_troll_spawn_egg']=name+(' Spawn Egg' if lang=='en_us' else ' — яйцо призыва');p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
