"""Original layered brawler geometry, skins and timed animations. No external assets."""
from pathlib import Path
import json,random
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/witchersbestiary'
for tier in range(3):
 rng=random.Random(2120+tier);im=Image.new('RGB',(128,128));pix=im.load();colors=[(171,119,83),(79,47,29),(128,106,75),(222,210,178),(51,55,63),(167,120,40),(86,27,29),(95,88,73)]
 for y in range(128):
  for x in range(128):
   color=colors[(y//32)*2+x//64];noise=rng.randrange(-12,13);pix[x,y]=tuple(max(0,min(255,v+noise)) for v in color)
 d=ImageDraw.Draw(im)
 # Skin face, explicit eyes/nose and brows; clothing seams and wrapped cloth.
 for x in [10,13]:d.rectangle((x,10,x+1,11),fill=(38,30,24));d.line((x,9,x+2,9),fill=(65,41,24))
 d.rectangle((12,12,13,13),fill=(133,77,53));d.line((10,15,14,15),fill=(70,39,26))
 for y in range(2,30,4):d.line((67,y,125,y),fill=(99,68,42));d.line((65,y+32,126,y+32),fill=(214,192,125))
 folder=root/'textures/entity';folder.mkdir(parents=True,exist_ok=True);im.save(folder/f'fighter_{tier}.png')
 bones=[]
 def bone(name,parent,pivot,cubes):bones.append({'name':name,**({'parent':parent} if parent else {}),'pivot':pivot,'cubes':[{'origin':o,'size':s,'uv':uv} for o,s,uv in cubes]})
 bone('root',None,[0,0,0],[]);bone('hips','root',[0,12,0],[([-4,10,-2],[8,3,4],[64,32])]);bone('body','hips',[0,12,0],[([-4,12,-2],[8,10,4],[64,0])]);bone('head','body',[0,22,0],[([-4,22,-4],[8,8,8],[0,0]),([-4.5,28,-4.5],[9,3,9],[0,32]),([-5,24,-1],[1,3,2],[0,0]),([4,24,-1],[1,3,2],[0,0])])
 for side,sign in [('left',1),('right',-1)]:
  x=0 if sign==1 else -4;armx=4 if sign==1 else -8
  bone(side+'_leg','hips',[sign*2,12,0],[([x,0,-2],[4,11,4],[0,64]),([x-.2,0,-3],[4.4,3,6],[0,96])])
  bone(side+'_arm','body',[sign*5,21,0],[([armx,15,-2],[4,7,4],[0,0])]);bone(side+'_forearm',side+'_arm',[sign*6,16,0],[([armx,11,-2],[4,5,4],[64,32]),([armx-.2,10,-2.2],[4.4,2,4.4],[64,32])])
 if tier==0:bone('rag_tunic','body',[0,18,0],[([-4.3,12,-2.3],[8.6,7,4.6],[0,32]),([-4.4,11,-2.4],[8.8,1,4.8],[64,96])])
 if tier==1:
  bone('vest','body',[0,20,0],[([-4.6,13,-2.6],[2,9,5.2],[0,96]),([2.6,13,-2.6],[2,9,5.2],[0,96])]);bone('beard','head',[0,24,0],[([-3,22,-4.4],[6,3,1],[0,32])]);bone('bandana','head',[0,28,0],[([-4.6,27,-4.6],[9.2,1.2,9.2],[64,96]),([3,24,4],[1,5,2],[64,96])])
 if tier==2:
  bone('cape','body',[0,22,3],[([-5,7,2.5],[10,15,1.2],[0,96])]);bone('mantle','body',[0,22,0],[([-6,20,-3],[12,3,6],[64,96])]+[([-6+i*1.5,19.5,-3.5],[1.3,3,1.8],[64,96]) for i in range(8)]);bone('champion_band','head',[0,29,0],[([-4.6,28,-4.6],[9.2,1,9.2],[0,64]),([-1,28,-5],[2,2,1],[64,32])]);bone('belt','hips',[0,12,0],[([-4.5,11,-2.5],[9,2,5],[0,96]),([-1.5,11,-3],[3,2,1],[64,32])])
 geo={'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':f'geometry.witchersbestiary.fighter_{tier}','texture_width':128,'texture_height':128,'visible_bounds_width':5,'visible_bounds_height':3,'visible_bounds_offset':[0,1,0]},'bones':bones}]}
 folder=root/'geo';folder.mkdir(exist_ok=True);(folder/f'fighter_{tier}.geo.json').write_text(json.dumps(geo,indent=2)+'\n')
animations={}
stance={'left_arm':{'rotation':[-60,0,-15]},'right_arm':{'rotation':[-60,0,15]},'left_forearm':{'rotation':[-70,0,0]},'right_forearm':{'rotation':[-70,0,0]}}
animations['idle']={'loop':True,'animation_length':2,'bones':{'body':{'rotation':{'0':[1,0,0],'1':[-1,0,0],'2':[1,0,0]}}}}
animations['walk']={'loop':True,'animation_length':1,'bones':{name:{'rotation':{'0':[sign*25,0,0],'.5':[-sign*25,0,0],'1':[sign*25,0,0]}} for name,sign in [('left_leg',1),('right_leg',-1),('left_arm',-1),('right_arm',1)]}}
animations['stance']={'loop':True,'animation_length':1,'bones':stance}
animations['combat_walk']={'loop':True,'animation_length':1,'bones':{**stance,**{k:v for k,v in animations['walk']['bones'].items() if k.endswith('_leg')}}}
for name,length,contact in [('quick',.5,.2),('counter',.7,.25),('heavy',1.4,.7)]:
 bones=dict(stance);bones['right_arm']={'rotation':{'0':[-60,0,15],str(contact*.65):[-135,-20,10] if name=='heavy' else [-70,-20,15],str(contact):[-95,15,-5],str(length):[-60,0,15]}};bones['right_forearm']={'rotation':{'0':[-70,0,0],str(contact):[0,0,0],str(length):[-70,0,0]}}
 animations[name]={'loop':True,'animation_length':length,'bones':bones}
animations['block']={'loop':True,'animation_length':1,'bones':{**stance,'left_arm':{'rotation':[-90,0,-25]},'right_arm':{'rotation':[-90,0,25]}}}
animations['dodge']={'loop':True,'animation_length':.5,'bones':{**stance,'body':{'rotation':{'0':[0,0,0],'.15':[12,0,25],'.5':[0,0,0]}}}}
animations['stagger']={'loop':True,'animation_length':.9,'bones':{'body':{'rotation':{'0':[0,0,0],'.2':[-25,0,5],'.9':[0,0,0]}}}}
animations['knockout']={'loop':True,'animation_length':1.5,'bones':{'root':{'rotation':{'0':[0,0,0],'.5':[0,0,85],'1.5':[0,0,85]},'position':{'0':[0,0,0],'.5':[0,8,0],'1.5':[0,8,0]}}}}
animations['victory']={'loop':True,'animation_length':1,'bones':{'left_arm':{'rotation':[-165,0,-20]},'right_arm':{'rotation':[-165,0,20]}}}
folder=root/'animations';folder.mkdir(exist_ok=True);(folder/'fighter.animation.json').write_text(json.dumps({'format_version':'1.8.0','animations':{'animation.fighter.'+k:v for k,v in animations.items()}},indent=2)+'\n')
for name in ['novice','experienced','champion']:(root/'models/item'/f'{name}_fighter_spawn_egg.json').write_text(json.dumps({'parent':'minecraft:item/template_spawn_egg'})+'\n')
