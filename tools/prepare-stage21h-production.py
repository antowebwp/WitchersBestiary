"""Pin actual supplied Skill Tree stack into isolated production test plans; no runtime dependency."""
from pathlib import Path
import argparse,json,zipfile,hashlib,subprocess,sys,os
root=Path(__file__).resolve().parents[1]
a=argparse.ArgumentParser();a.add_argument('--without-skill-tree',action='store_true');args=a.parse_args()
os.environ['STAGE21G_SPELL_COMPAT']='1'
subprocess.run([sys.executable,str(root/'tools/prepare-stage21f-production.py')],cwd=root,check=True)
pins={'skill_tree_rpgs':'1.4.4+1.21.1','puffish_skills':'0.17.3','puffish_attributes':'0.8.2','default_skill_trees':'1.1','ranged_weapon_api':'2.3.3+1.21.1'}
mods=[];record=[]
if not args.without_skill_tree:
 for p in (root/'build/stage21h-compat/mods').glob('*.jar'):
  with zipfile.ZipFile(p) as z:m=json.loads(z.read('fabric.mod.json'))
  if m['id'] not in pins or m['version']!=pins[m['id']]:raise RuntimeError('Unpinned artifact '+p.name)
  mods.append(str(p));record.append({'id':m['id'],'version':m['version'],'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
 if {r['id'] for r in record}!=set(pins):raise RuntimeError('Exact supplied Skill Tree stack incomplete')
for role in ['Server','One','Two']:
 p=root/'build/stage20e-network'/f'{role}.production.launch.json';data=json.loads(p.read_text(encoding='utf-8'));data['mods']+=mods;p.write_text(json.dumps(data,indent=2),encoding='utf-8')
(root/'build/stage21h-compat/manifest.json').write_text(json.dumps({'skill_tree_enabled':not args.without_skill_tree,'mods':record},indent=2),encoding='utf-8')
print('Skill Tree isolated profile:', 'WITHOUT' if args.without_skill_tree else 'WITH exact supplied stack')
