"""Isolated production campaign. Exact AdventureZ artifact adds real third-party hostile targets."""
from pathlib import Path
import subprocess,sys,json,zipfile,hashlib,urllib.request
root=Path(__file__).resolve().parents[1]
subprocess.run([sys.executable,str(root/'tools/prepare-stage21h-production.py')],cwd=root,check=True)
folder=root/'build/stage21i-compat';folder.mkdir(exist_ok=True)
req=urllib.request.Request('https://api.modrinth.com/v2/version/LxZfB2Kk',headers={'User-Agent':'WitchersBestiary-local-compat/0.21.0'})
version=json.load(urllib.request.urlopen(req));artifact=next(f for f in version['files'] if f['primary']);jar=folder/artifact['filename']
if not jar.exists():jar.write_bytes(urllib.request.urlopen(artifact['url']).read())
assert hashlib.sha512(jar.read_bytes()).hexdigest()==artifact['hashes']['sha512']
(folder/'adventurez-source.json').write_text(json.dumps(version,indent=2),encoding='utf-8')
for role in ['Server','One','Two']:
 p=root/'build/stage20e-network'/f'{role}.production.launch.json';data=json.loads(p.read_text(encoding='utf-8'));data['mods'].append(str(jar))
 if '--standalone' in sys.argv:
  retained=[]
  for mod in data['mods']:
   with zipfile.ZipFile(mod) as z: ident=json.loads(z.read('fabric.mod.json')).get('id')
   if ident not in {'spell_engine','spell_power','skill_tree_rpgs','puffish_skills','puffish_attributes','ranged_weapon_api','default_skill_trees'}:retained.append(mod)
  data['mods']=retained
 p.write_text(json.dumps(data,indent=2),encoding='utf-8')
print('Stage21I: AdventureZ1.4.24; '+('standalone magic' if '--standalone' in sys.argv else 'Spell Engine/Power and Skill Tree'))
