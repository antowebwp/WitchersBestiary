"""Original articulated travelling alchemist. Deterministic geometry, UV atlas and GeckoLib clips."""
from pathlib import Path
from PIL import Image,ImageDraw
import json,random,math
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources/assets/witchersbestiary'
image=Image.new('RGB',(256,256),(28,23,20));draw=ImageDraw.Draw(image);rng=random.Random(2105)
materials={'leather':(78,49,32),'cloth':(53,46,38),'skin':(172,124,86),'metal':(147,124,75),'glass':(66,119,109),'boot':(43,31,24),'beard':(92,78,62)}
u=v=row=0;bones=[]
def cube(origin,size,mat):
 global u,v,row
 w=math.ceil(2*(size[0]+size[2]));h=math.ceil(size[1]+size[2])
 if u+w+2>256:u=0;v+=row+2;row=0
 assert v+h<256
 uv=[u,v];color=materials[mat]
 for yy in range(v,v+h):
  for xx in range(u,u+w):image.putpixel((xx,yy),tuple(max(0,min(255,c+rng.randint(-7,7))) for c in color))
 draw.line((u,v,u+w-1,v),fill=tuple(min(255,c+18) for c in color));u+=w+2;row=max(row,h)
 return {'origin':origin,'size':size,'uv':uv}
def bone(name,parent,pivot,boxes):
 b={'name':name,'pivot':pivot,'cubes':[cube(*box) for box in boxes]}
 if parent:b['parent']=parent
 bones.append(b)
bone('root',None,[0,0,0],[])
bone('body','root',[0,12,0],[([-4,12,-2.5],[8,12,5],'leather'),([-4.5,12,-3],[9,2,6],'leather'),([-1,12.2,-3.4],[2,1.6,.4],'metal'),([-5,18,2.7],[10,7,1.2],'cloth'),([-5,9,3.2],[10,9,1],'cloth'),([-4,9,-2.5],[8,3,5],'cloth'),([4,9,1],[3,6,4],'leather'),([4.2,14.6,1.1],[2.6,.5,3.7],'metal')])
# Narrow straps connect the satchel to the torso; bottles are attached to the front belt.
for i,x in enumerate([-3,-.5,2]):bone('bottle'+str(i),'body',[x,14,-3],[([x,13.5,-4],[1.5,3,1.5],'glass'),([x+.3,16.5,-3.7],[.9,.7,.9],'metal')])
bone('head','body',[0,24,0],[([-3,24,-3],[6,7,5.8],'skin'),([-4,23.8,-3.2],[1,9,7],'cloth'),([3,23.8,-3.2],[1,9,7],'cloth'),([-3,31.8,-3.2],[6,1,7],'cloth'),([-3,24,2.9],[6,8,1],'cloth'),([-3,24,-3.25],[6,1.5,.25],'beard'),([-.5,26,-3.7],[1,1.8,.7],'skin')])
for x in [-1.9,1.1]:bone('eye'+str(x),'head',[0,24,0],[([x,28,-3.3],[.8,.6,.2],'boot'),([x-.2,28.8,-3.3],[1.2,.5,.2],'beard')])
for side,sign in [('left',1),('right',-1)]:
 x=4.1 if sign==1 else -7.1
 bone(side+'_arm','body',[5.6*sign,23,0],[([x,15,-2],[3,9,4],'cloth'),([x,12,-2],[3,3,4],'leather'),([x,17.8,-2.1],[3,1.2,4.2],'leather')])
 x=.3 if sign==1 else -3.8
 bone(side+'_leg','root',[2*sign,12,0],[([x,3,-2],[3.5,9,4],'cloth'),([x-.2,0,-2.7],[3.9,4,5],'boot'),([x-.1,3,-2.2],[3.7,1,4.4],'leather')])
geo={'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.arcane_merchant','texture_width':256,'texture_height':256,'visible_bounds_width':3,'visible_bounds_height':3,'visible_bounds_offset':[0,1,0]},'bones':bones}]}
(ROOT/'geo/arcane_merchant.geo.json').write_text(json.dumps(geo,indent=2)+'\n');image.save(ROOT/'textures/entity/arcane_merchant.png')
animations={}
animations['animation.merchant.idle']={'loop':True,'animation_length':4,'bones':{'body':{'rotation':{'0':[0,0,0],'2':[1.5,0,0],'4':[0,0,0]}},'left_arm':{'rotation':{'0':[0,0,-3],'2':[1,0,-4],'4':[0,0,-3]}},'right_arm':{'rotation':[0,0,3]}}}
walk={'loop':True,'animation_length':1.6,'bones':{}}
for name,sign in [('left_leg',1),('right_leg',-1),('left_arm',-1),('right_arm',1)]:walk['bones'][name]={'rotation':{'0':[18*sign,0,0],'.8':[-18*sign,0,0],'1.6':[18*sign,0,0]}}
animations['animation.merchant.walk']=walk
animations['animation.merchant.trade']={'loop':True,'animation_length':3,'bones':{'right_arm':{'rotation':{'0':[-25,0,12],'1':[-58,-12,8],'2':[-58,12,8],'3':[-25,0,12]}},'left_arm':{'rotation':{'0':[-15,0,-8],'1.5':[-35,0,-8],'3':[-15,0,-8]}}}}
(ROOT/'animations/arcane_merchant.animation.json').write_text(json.dumps({'format_version':'1.8.0','animations':animations},indent=2)+'\n')
print(f'Arcane Merchant: {len(bones)} bones, {sum(len(b["cubes"]) for b in bones)} volumes, original256px atlas, three clips')
