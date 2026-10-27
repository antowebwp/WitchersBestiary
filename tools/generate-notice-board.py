"""Original notice-board cuboids and procedural atlas. Never reads reference pixels."""
from pathlib import Path
from PIL import Image, ImageDraw
import json, random, base64, uuid
R=Path(__file__).resolve().parents[1]; A=R/'src/main/resources/assets/witchersbestiary'; rng=random.Random(2002)
for p in ['models/block','models/item','textures/block','textures/gui','blockstates']: (A/p).mkdir(parents=True,exist_ok=True)
img=Image.new('RGB',(256,256),(55,37,25));draw=ImageDraw.Draw(img)
colors={'wood':(94,60,32),'dark':(61,40,25),'roof':(111,70,34),'stone':(100,103,97),'iron':(49,53,51),'gold':(197,132,41),'light':(255,214,105),'paper0':(198,165,107),'paper1':(218,187,132),'paper2':(179,148,96),'red':(116,33,23)}
uv={}
for n,(name,color) in enumerate(colors.items()):
 x=(n%8)*32;y=(n//8)*64;w=32;h=64;uv[name]=[x/16,y/16,(x+w)/16,(y+h)/16]
 for py in range(y,y+h):
  for px in range(x,x+w):
   noise=rng.randrange(-14,15);grain=6 if px%7==0 else 0
   img.putpixel((px,py),tuple(max(0,min(255,v+noise-grain)) for v in color))
 if name in ('wood','dark','roof'):
  for j in range(10):
   xx=x+rng.randrange(2,30);yy=y+rng.randrange(2,56);draw.line((xx,yy,xx+rng.choice([-1,0,1]),min(y+63,yy+rng.randrange(8,23))),fill=tuple(max(0,v-24) for v in color))
 if name.startswith('paper'):
  # Original ink silhouettes, horns and handwritten line marks; no external imagery.
  ink=(73,57,39);cx=x+16;cy=y+20
  draw.ellipse((cx-4,cy-8,cx+4,cy),outline=ink,width=2)
  draw.line([(cx,cy),(cx-5,cy+12),(cx+5,cy+12),(cx,cy)],fill=ink,width=2)
  for dx in [-1,1]:
   draw.line([(cx+dx*3,cy-6),(cx+dx*9,cy-13),(cx+dx*6,cy-6)],fill=ink,width=1)
   draw.line([(cx+dx*3,cy+2),(cx+dx*10,cy+9)],fill=ink,width=2)
   draw.line([(cx+dx*3,cy+11),(cx+dx*7,cy+20)],fill=ink,width=2)
  for yy in range(y+43,y+60,3):draw.line((x+4,yy,x+rng.randrange(19,29),yy),fill=ink)
img.save(A/'textures/block/notice_board.png')
elements=[];bounds=[]
def box(name,a,b,mat='wood'):
 assert all(b[i]>a[i] for i in range(3)); aa=[a[0]-16,a[1]-16,a[2]];bb=[b[0]-16,b[1]-16,b[2]]
 elements.append({'name':name,'from':aa,'to':bb,'faces':{face:{'uv':uv[mat],'texture':'#atlas'} for face in ['north','south','east','west','up','down']}});bounds.append((a,b))
for x in [3,41]:
 for yy in range(0,6,2):
  for xx in range(x-2,x+5,3): box('foundation',(xx,yy,3),(min(xx+2.8,x+5),yy+1.9,13),'stone')
 box('oak upright',(x,5,6),(x+4,40,10),'dark')
 for yy in [11,32,37]:
  box('iron strap',(x-.3,yy,5.7),(x+4.3,yy+1.6,10.3),'iron')
  for xx in [x+.6,x+3]:box('rivet',(xx,yy+.5,5.35),(xx+.6,yy+1.1,5.7),'gold')
for n in range(7):
 y=10+n*3.5;box('individual backing plank',(7,y,7.6),(41,y+3.3,10),'wood' if n%2 else 'dark')
for y in [7,35]:box('cross beam',(5,y,6.3),(43,y+3,10.8),'wood')
# Segmented diagonal rear brace with visibly separate depth.
for i in range(13):
 x=8+i*2.35;y=11+i*1.8;box('rear diagonal brace',(x,y,10.3),(x+3.2,y+3.2,12.1),'roof')
for z in range(0,16,2):
 y=39+min(z,14-z)*1.05
 box('roof underlay',(.04,y-.8,z+.04),(47.96,y+1.95,min(z+2.1,16)-.04),'dark')
 for x in range(0,48,6):box('overlapping roof shingle',(x,y,z),(x+5.8,y+2.3,z+2.1 if z<14 else 16),'roof')
for x in range(1,48,8):box('ridge cap',(x,47,6.6),(min(x+7.8,48),48,9.4),'dark')
for x in [5,41]:
 for z in [1,12]:box('rafter',(x,38,z),(x+2,40,z+3),'dark')
for i,(x,y,w,h) in enumerate([(9,21,10,12),(21,18,13,15),(34,24,6,10),(13,12,7,8)]):
 box('parchment notice',(x,y,7.15-i*.02),(x+w,y+h,7.45-i*.02),'paper'+str(i%3))
 box('wax seal',(x+w-2,y+.6,6.9-i*.02),(x+w-.4,y+2.1,7.15-i*.02),'red')
 box('paper tack',(x+w/2,y+h-.8,6.95-i*.02),(x+w/2+.5,y+h-.3,7.15-i*.02),'iron')
box('lantern bracket',(1,33,4),(6,34,5),'iron')
box('lantern hanging chain',(1.8,30,4.1),(2.4,33,4.7),'iron')
box('lantern glowing heart',(.6,24,3),(3.5,29,6),'light')
for x in [0,3.4]:
 for z in [2.6,5.8]:box('lantern corner',(x,23.7,z),(x+.5,29.4,z+.5),'iron')
box('lantern base',(0,23.2,2.5),(4,24,6.5),'iron');box('lantern cap',(0,29,2.5),(4,30,6.5),'iron')
model={'textures':{'atlas':'witchersbestiary:block/notice_board','particle':'witchersbestiary:block/notice_board'},'elements':elements,'display':{'gui':{'rotation':[20,35,0],'translation':[0,-1,0],'scale':[.28,.28,.28]},'firstperson_righthand':{'rotation':[0,35,0],'translation':[0,0,0],'scale':[.25,.25,.25]},'firstperson_lefthand':{'rotation':[0,-35,0],'translation':[0,0,0],'scale':[.25,.25,.25]},'thirdperson_righthand':{'rotation':[70,0,0],'translation':[0,2,0],'scale':[.25,.25,.25]},'thirdperson_lefthand':{'rotation':[70,0,0],'translation':[0,2,0],'scale':[.25,.25,.25]},'ground':{'translation':[0,3,0],'scale':[.2,.2,.2]},'fixed':{'scale':[.3,.3,.3]}}}
def save(path,obj):path.write_text(json.dumps(obj,indent=2)+'\n',encoding='utf-8')
save(A/'models/block/notice_board.json',model);save(A/'models/block/notice_board_empty.json',{'textures':{'particle':'witchersbestiary:block/notice_board'},'elements':[]})
save(A/'models/item/notice_board.json',{'parent':'witchersbestiary:block/notice_board'})
variants={}
for facing,angle in [('north',0),('east',90),('south',180),('west',270)]:
 for i in range(9):variants[f'facing={facing},part={i}']={'model':'witchersbestiary:block/notice_board' if i==4 else 'witchersbestiary:block/notice_board_empty','y':angle}
save(A/'blockstates/notice_board.json',{'variants':variants})
# Exact clipped collision/outline from the same cuboids; voids remain passable.
shapes=[]
for row in range(3):
 for col in range(3):
  cell=[]
  for a,b in bounds:
   lo=[max(a[0],col*16)-col*16,max(a[1],row*16)-row*16,max(a[2],0)];hi=[min(b[0],(col+1)*16)-col*16,min(b[1],(row+1)*16)-row*16,min(b[2],16)]
   if all(hi[k]>lo[k] for k in range(3)):cell.append(lo+hi)
  shapes.append(cell)
code='package com.witchersbestiary.board;\nimport net.minecraft.util.shape.*;\nimport net.minecraft.util.math.Direction;\npublic final class NoticeBoardShapes {\n private static final VoxelShape[][] SHAPES=new VoxelShape[9][4];\n static {\n'
for part,boxes in enumerate(shapes):
 for turn in range(4):
  entries=[]
  for values in boxes:
   x,y,z,X,Y,Z=values
   for _ in range(turn):x,z,X,Z=16-Z,x,16-z,X
   entries.append('VoxelShapes.cuboid('+','.join(f'{v/16:.6f}' for v in [x,y,z,X,Y,Z])+')')
  code+=f'  SHAPES[{part}][{turn}]=VoxelShapes.union(VoxelShapes.empty(),new VoxelShape[]{{'+','.join(entries)+'}).simplify();\n'
code+=' }\n public static VoxelShape get(int part,Direction facing){int turn=switch(facing){case EAST->1;case SOUTH->2;case WEST->3;default->0;};return SHAPES[part][turn];}\n}\n'
(R/'src/main/java/com/witchersbestiary/board/NoticeBoardShapes.java').write_text(code,encoding='utf-8')
# Editable source with embedded original texture.
art=R/'art_source/notice_board';art.mkdir(parents=True,exist_ok=True)
shutil=None
(art/'notice_board.png').write_bytes((A/'textures/block/notice_board.png').read_bytes())
bb={'meta':{'format_version':'4.10','model_format':'java_block','box_uv':False},'name':'Witcher notice board','resolution':{'width':256,'height':256},'elements':[],'outliner':[],'textures':[{'path':'notice_board.png','name':'notice_board.png','id':'0','width':256,'height':256,'source':'data:image/png;base64,'+base64.b64encode((art/'notice_board.png').read_bytes()).decode()}]}
for e in elements:
 uid=str(uuid.uuid5(uuid.NAMESPACE_URL,str(e)));el={**e,'uuid':uid,'type':'cube','origin':[8,8,8]};el['faces']={face:{'uv':[v*16 for v in data['uv']],'texture':0} for face,data in e['faces'].items()};bb['elements'].append(el);bb['outliner'].append(uid)
save(art/'notice_board.bbmodel',bb)
# Original parchment panel texture; client draws text and item/monster marks separately.
gui=Image.new('RGB',(384,240),(39,27,20));d=ImageDraw.Draw(gui)
for y in range(4,236):
 for x in range(4,380):
  n=rng.randrange(-7,8);base=(206,177,127) if x>130 else (83,56,35);gui.putpixel((x,y),tuple(v+n for v in base))
for i in range(4):d.rectangle((i,i,383-i,239-i),outline=(65+i*7,61+i*5,49+i*3))
for x in [7,376]:
 for y in [7,232]:d.rectangle((x-2,y-2,x+2,y+2),fill=(133,125,100))
d.ellipse((350,9,370,29),fill=(119,32,24),outline=(70,23,19),width=2)
gui.save(A/'textures/gui/notice_board.png')
print('Original model elements:',len(elements),'3x3x1 blocks; 256px atlas; 384x240 GUI')
