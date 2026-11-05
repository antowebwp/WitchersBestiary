"""Original articulated rock troll, adapted from the user's supplied turnaround.
Stone shelves, hunched silhouette, tusked face, rope and torn hide. No external assets.
"""
from pathlib import Path
from PIL import Image,ImageDraw
import json,random,math
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/witchersbestiary'
rng=random.Random(210321)
im=Image.new('RGB',(512,512),(50,48,45));draw=ImageDraw.Draw(im)
bones=[];shelf_x=2;shelf_y=2;shelf_h=0
palette={'stone':(124,119,111),'edge':(146,140,129),'core':(103,99,91),'skin':(119,91,75),'cloth':(93,53,32),'rope':(139,108,65),'rope_dark':(111,80,43),'tooth':(192,178,142),'mouth':(37,25,22),'eye':(182,119,55),'moss':(87,99,44),'mossstone':(119,117,103)}
def atlas(size,material):
 global shelf_x,shelf_y,shelf_h
 x,y,z=size;w=math.ceil(2*(x+z));h=math.ceil(y+z)
 if shelf_x+w+2>512:shelf_x=2;shelf_y+=shelf_h+2;shelf_h=0
 assert shelf_y+h+2<=512,'Atlas exhausted'
 u,v=shelf_x,shelf_y;shelf_x+=w+2;shelf_h=max(shelf_h,h)
 base=palette[material]
 for py in range(v,v+h):
  for px in range(u,u+w):
   n=rng.randrange(-10,11)
   b=base
   if material=='mossstone' and math.sin(px*.56)+math.cos(py*.41)>.85:b=palette['moss']
   im.putpixel((px,py),tuple(max(0,min(255,c+n)) for c in b))
 faces=[(u+z,v,x,z),(u+z+x,v,x,z),(u,v+z,z,y),(u+z,v+z,x,y),(u+z+x,v+z,z,y),(u+2*z+x,v+z,x,y)]
 for fx,fy,fw,fh in faces:
  if min(fw,fh)<1:continue
  a,b,c,d=map(int,(fx,fy,fx+fw-1,fy+fh-1))
  light=tuple(min(255,k+20) for k in base);dark=tuple(max(0,k-23) for k in base)
  draw.line((a,b,c,b),fill=light);draw.line((a,b,a,d),fill=light);draw.line((a,d,c,d),fill=dark);draw.line((c,b,c,d),fill=dark)
  if material in ('stone','edge','mossstone') and fw>3 and fh>3:
   cx=rng.randint(a+1,max(a+1,c-1));cy=rng.randint(b+1,max(b+1,d-1))
   draw.line((cx,b+1,cx-1,cy,cx+1,min(d,cy+3)),fill=(66,64,58))
  if material=='cloth' and fw>2:
   for yy in range(b+2,d,3):draw.line((a+1,yy,c-1,yy),fill=(105,62,38))
  if material.startswith('rope'):
   for xx in range(a,c,3):draw.line((xx,b,min(c,xx+2),d),fill=(174,139,85))
 return [u,v]
def cube(o,s,mat='stone',rot=None):
 c={'origin':o,'size':s,'uv':atlas(s,mat)}
 if rot:c.update(pivot=[o[i]+s[i]/2 for i in range(3)],rotation=rot)
 return c
def bone(n,parent,pivot,cubes):
 bones.append({'name':n,**({'parent':parent} if parent else {}),'pivot':pivot,'cubes':cubes})
bone('root',None,[0,0,0],[])
bone('hips','root',[0,16,0],[cube([-8,11,-5],[16,11,11],'core')])
bone('body','hips',[0,18,0],[cube([-11,20,-5.5],[22,11,13],'core'),cube([-12,30,-5],[24,8,14],'core')])
# Recess hidden core faces by0.25: cap/core y40 and foot/core z5 must not be coplanar.
# Low forward head nestles into the dome instead of sitting on a human neck.
bone('neck','body',[0,34,0],[cube([-6,31,-7],[12,6,10],'core')])
bone('head','neck',[0,35,-5],[cube([-7,35,-9],[14,6.75,9.75],'skin'),cube([-8,39,-10],[16,3,11],'mossstone'),cube([-8,32,-11],[3,7,6],'stone',[-5,0,-5]),cube([5,32,-11],[3,7,6],'stone',[-5,0,5]),cube([-5,31,-10.5],[10,5,1.5],'mouth'),cube([-2.5,35.5,-14],[5,4,5],'skin'),cube([-7,38.2,-12],[6,2.5,3],'edge',[0,0,-7]),cube([1,38.2,-12],[6,2.5,3],'edge',[0,0,7]),cube([-10,35,-6],[2.3,4,4],'skin',[0,0,-8]),cube([7.7,35,-6],[2.3,4,4],'skin',[0,0,8])])
bone('eyes','head',[0,37,-10],[cube([-5,37,-11.2],[2,1,1],'mouth'),cube([3,37,-11.2],[2,1,1],'mouth'),cube([-4.6,37.2,-11.35],[.7,.55,.4],'eye'),cube([3.9,37.2,-11.35],[.7,.55,.4],'eye')])
bone('jaw','head',[0,32,-7],[cube([-6.5,28.8,-12.5],[13,3,7],'stone'),cube([-7,30,-13.1],[2,5,3],'edge',[0,0,-7]),cube([5,30,-13.1],[2,5,3],'edge',[0,0,7]),cube([-5.8,32.5,-13.4],[1.5,5,1.8],'tooth',[0,0,-8]),cube([4.3,32.5,-13.4],[1.5,5,1.8],'tooth',[0,0,8])]+[cube([x,31.6,-13],[1.6,1.8,1.6],'tooth') for x in [-3.5,-1.2,1.2,3.5]])
# A forward crest bridges the rear shelves into a dome over the low face.
for b in bones:
 if b['name'] in ('head','eyes','jaw'):
  b['pivot'][1]-=2
  for c in b['cubes']:
   c['origin'][1]-=2
   if 'pivot' in c:c['pivot'][1]-=2
crest=[]
for sgn in (-1,1):
 for k in range(3):
  crest.append(cube([sgn*(7+k*3.5)-(5 if sgn<0 else 0),39-k*1.8,-4+k*.7],[5,5,9],'mossstone',[-8,sgn*5,sgn*(10+k*9)]))
crest += [cube([-7,41,-2],[7,4,8],'mossstone',[-8,-8,-5]),cube([.4,41,-2],[6.6,4,8],'mossstone',[-8,8,5])]
bone('forward_crest','body',[0,38,2],crest)
# Chest armour is a stepped V, with gaps exposing the dark stone core.
for row,(y,width) in enumerate([(23,15),(28,19),(33,22)]):
 tiles=[]
 for j in range(3):
  w=width/3-.45;tiles.append(cube([-width/2+j*(width/3),y,-8.2-row*.22],[w,4.8,3.2],'edge' if row==2 else 'stone',[0,(-1+j)*4,(-1+j)*5]))
 bone('chest_plates_'+str(row),'body',[0,y+2,0],tiles)
# Overlapping, offset rock shelves form the characteristic massive hump.
for row,(y,width,depth) in enumerate([(22,23,7),(28,27,9),(34,29,10),(39,23,9),(40,14,6)]):
 tiles=[]
 for j in range(4):
  w=width/4-.45;ox=-width/2+j*width/4
  tiles.append(cube([ox,y,5+row*.3],[w,5.2,depth],'mossstone' if row>=2 else 'stone',[-8+row*3,(-1.5+j)*6,(-1.5+j)*3]))
 bone('hump_'+str(row),'body',[0,y+2,8],tiles)
for side,sgn in [('left',1),('right',-1)]:
 x=2 if sgn==1 else -9
 bone(side+'_leg','hips',[sgn*5.5,13,0],[cube([x,3,-3],[7,10,7.75],'core'),cube([x-.3,6,-5],[7.6,5,3],'stone',[-5,0,sgn*4]),cube([x-.7,1,-7],[8.4,3.5,12],'stone')]+[cube([x-.65+k*2.15,0,-7.8],[1.9,2.7,3.6],'edge') for k in range(4)])
 ax=13 if sgn==1 else -23
 bone(side+'_arm','body',[sgn*14,32,0],[cube([ax,23,-4],[10,10,10],'core'),cube([ax-.6,24,-5.4],[11.2,7.4,4],'stone',[0,0,sgn*10])])
 fx=14 if sgn==1 else -24
 bone(side+'_forearm',side+'_arm',[sgn*19,23,0],[cube([fx,11,-5],[10,12,10],'core'),cube([fx-1,12,-7.2],[12,8.5,3.5],'stone',[-8,0,sgn*5]),cube([fx-.3,19,-6.3],[10.6,4,3],'edge',[0,0,-sgn*7])])
 hand=13 if sgn==1 else -26
 bone(side+'_fist',side+'_forearm',[sgn*19.5,12,0],[cube([hand,4,-6.5],[13,8,12],'core')]+[cube([hand+k*3.3,5,-9],[3,6.5,3.7],'stone',[rng.randint(-4,4),0,sgn*(k-1.5)*3]) for k in range(4)]+[cube([hand-1 if sgn==1 else hand+10.5,6,-3],[3.5,5,5],'edge',[0,0,-sgn*20])])
 shoulder=[]
 for k in range(3):
  ox=(11+k*3.5) if sgn==1 else (-18-k*3.5)
  shoulder.append(cube([ox,32-k*1.3,-6],[7,6,13],'mossstone' if k<2 else 'stone',[0,sgn*8,sgn*(8+k*12)]))
 bone(side+'_shoulder_plates','body',[sgn*13,33,0],shoulder)
 # Stone armour wraps around the limbs: side/rear views must not expose box cores.
 wrap=[]
 for j in range(2):
  wrap += [cube([fx-.8,12+j*5,3.5],[5.2,4.6,2.5],'stone',[0,sgn*6,sgn*4]),cube([fx+4.5,13+j*4.8,3.8],[5.9,4.3,2.3],'edge',[0,-sgn*5,-sgn*6])]
  wrap += [cube([fx-.9 if sgn<0 else fx+8.4,12+j*5,-4],[2.5,4.7,8.2],'stone',[0,0,sgn*7])]
 bone(side+'_forearm_wrap',side+'_forearm',[sgn*19,18,0],wrap)
 handplates=[]
 for j in range(3):
  handplates += [cube([hand+j*4.4,4.1,3.8],[4,7.2,2.3],'stone',[0,sgn*5,sgn*(j-1)*5]),cube([hand+j*4.4,3,-5.6],[4,2,8.5],'edge',[sgn*4,0,0])]
 handplates += [cube([hand-.8,4.8,-5.8],[2,6.4,10],'stone',[0,0,-5]),cube([hand+11.8,4.8,-5.8],[2,6.4,10],'stone',[0,0,5])]
 bone(side+'_finger_scales',side+'_fist',[sgn*19.5,8,0],handplates)
 bone(side+'_upper_arm_wrap',side+'_arm',[sgn*18,28,0],[cube([ax-.8 if sgn<0 else ax+8.2,24,-3.5],[2.6,7.7,9.5],'stone',[0,0,sgn*8]),cube([ax,24,4.2],[10,7.2,2.6],'mossstone',[0,sgn*5,sgn*8])])
 # Forearm slabs and rough knuckle scales remain attached to their own moving bones.
 for j in range(3):
  bone(side+'_wrist_plate_'+str(j),side+'_forearm',[sgn*19,16,0],[cube([fx-1+j*4.2,13+j%2,-8],[3.8,5.5,2],'mossstone' if j==1 else 'stone',[0,0,sgn*(j-1)*9])])
# Separate hanging hide strips make a ragged outline without zero-thickness planes.
cloth=[]
for face,z in [('front',-7.4),('back',6.8)]:
 for j in range(6):
  bottom=[11,8.5,10,7.8,9.2,11.5][j];cloth.append(cube([-9+j*3.05,bottom,z],[2.8,21-bottom,1.1],'cloth',[8 if face=='front' else -8,0,(-1 if j%2 else 1)*3]))
cloth += [cube([-10,12,-4.8],[1.2,9,10],'cloth',[0,0,-4]),cube([8.8,12,-4.8],[1.2,9,10],'cloth',[0,0,4])]
bone('loincloth','hips',[0,21,0],cloth)
rope=[]
for row in range(2):
 for i in range(20):
  t=i*math.tau/20;cx=10*math.cos(t);cz=6.8*math.sin(t)
  rope.append(cube([cx-1.45,20.3+row*1.1+(i%2)*.18,cz-.65],[2.9,1.05,1.3],'rope' if (i+row)%2 else 'rope_dark',[0,90-math.degrees(t),(-1 if i%2 else 1)*9]))
rope += [cube([3.5,19.2,-8.4],[4.8,2,2.2],'rope',[0,0,25]),cube([4,15.5,-8.1],[1.4,5,1.4],'rope_dark',[0,0,-16]),cube([7,13.5,-7.7],[1.4,6.5,1.4],'rope',[0,0,12]),cube([6.8,11.8,-8],[2,2.2,1.4],'stone')]
bone('rope_belt','hips',[0,21,0],rope)
geo={'format_version':'1.12.0','minecraft:geometry':[{'description':{'identifier':'geometry.rock_troll','texture_width':512,'texture_height':512,'visible_bounds_width':7,'visible_bounds_height':5,'visible_bounds_offset':[0,1.5,0]},'bones':bones}]}
(root/'geo/rock_troll.geo.json').write_text(json.dumps(geo,indent=2)+'\n')
im.save(root/'textures/entity/rock_troll.png')
a=json.loads((root/'animations/fighter.animation.json').read_text())
import copy
for name,factor in [('quick',2),('heavy',18/14)]:
 clip=copy.deepcopy(a['animations']['animation.fighter.'+name]);clip['loop']=False;clip['animation_length']*=factor
 for channels in clip['bones'].values():
  for channel,value in list(channels.items()):
   if isinstance(value,dict):channels[channel]={str(round(float(t)*factor,5)):v for t,v in value.items()}
 a['animations']['animation.fighter.wild_'+name]=clip
for k,seconds in [('throw',2)]:
 a['animations']['animation.fighter.'+k]={'animation_length':seconds,'bones':{'body':{'rotation':{'0':[0,0,0],'.6':[-20,0,0],'1.0':[25,0,0],'1.8':[0,0,0]}},'right_arm':{'rotation':{'0':[0,0,0],'.6':[-130,0,0],'1.0':[30,0,0],'1.8':[0,0,0]}}}}
a['animations']['animation.fighter.death']={'animation_length':1,'bones':{'root':{'rotation':{'0':[0,0,0],'.8':[80,0,0]},'position':{'0':[0,0,0],'.2':[0,2.5,0],'.4':[0,5,0],'.6':[0,8,0],'.8':[0,11,0]}}}}
a['animations']['animation.fighter.stomp']={'animation_length':2,'bones':{'right_leg':{'rotation':{'0':[0,0,0],'.7':[-50,0,0],'1.1':[15,0,0],'1.5':[0,0,0]}},'body':{'rotation':{'0':[0,0,0],'.7':[-12,0,0],'1.1':[15,0,0],'1.7':[0,0,0]}}}}
a['animations']['animation.fighter.knockout']={'loop':False,'animation_length':1.5,'bones':{'root':{'rotation':{'0':[0,0,0],'.2':[-15,0,0],'.6':[-80,0,0],'1.5':[-80,0,0]},'position':{'0':[0,0,0],'.2':[0,1,0],'.4':[0,6,0],'.6':[0,10,0],'1.5':[0,10,0]}},'left_arm':{'rotation':{'0':[0,0,0],'.4':[0,0,-12]}},'right_arm':{'rotation':{'0':[0,0,0],'.4':[0,0,12]}},'left_forearm':{'rotation':{'0':[0,0,0],'.4':[-15,0,0]}},'right_forearm':{'rotation':{'0':[0,0,0],'.4':[-15,0,0]}}}}

# Dedicated facial and wrist motion follows the server contact frames.
a['animations']['animation.fighter.alert']={'animation_length':1,'bones':{'head':{'rotation':{'0':[0,0,0],'.3':[-7,-12,0],'.65':[-4,12,0],'1':[0,0,0]}},'jaw':{'rotation':{'0':[0,0,0],'.3':[12,0,0],'1':[0,0,0]}}}}
for name,contact in [('wild_quick',.4),('wild_heavy',.9),('throw',1.0)]:
 clip=a['animations']['animation.fighter.'+name]
 clip['bones']['right_fist']={'rotation':{'0':[0,0,0],str(contact*.65):[-12,0,8],str(contact):[12,0,-8],str(clip['animation_length']):[0,0,0]}}
 clip['bones']['jaw']={'rotation':{'0':[0,0,0],str(contact):[10,0,0],str(clip['animation_length']):[0,0,0]}}
a['animations']['animation.fighter.throw']['bones']['right_forearm']={'rotation':{'0':[0,0,0],'.6':[-65,0,0],'1.0':[15,0,0],'1.8':[0,0,0]}}
(root/'animations/rock_troll.animation.json').write_text(json.dumps(a,indent=2)+'\n')
(root/'models/item/rock_troll_spawn_egg.json').write_text('{"parent":"minecraft:item/template_spawn_egg"}\n')
for lang,name in [('en_us','Rock Troll'),('ru_ru','Скальный тролль')]:
 p=root/'lang'/f'{lang}.json';d=json.loads(p.read_text(encoding='utf-8'));d['entity.witchersbestiary.rock_troll']=name;d['item.witchersbestiary.rock_troll_spawn_egg']=name+(' Spawn Egg' if lang=='en_us' else ' — яйцо призыва');p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
