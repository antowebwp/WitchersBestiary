"""Build isolated Stage21F production plans using only existing local dependency jars."""
from pathlib import Path
import json,shutil,subprocess,sys,zipfile,os
root=Path(__file__).resolve().parents[1];dest=root/('build/stage21g-production-mods' if os.environ.get('STAGE21G_SPELL_COMPAT')=='1' else 'build/stage21f-production-mods');dest.mkdir(exist_ok=True)
ids={'bettercombat','playeranimator','cloth-config','architectury','fzzy_config','fabric-language-kotlin','simplyswords','simplytooltips'}
selected=[]
if os.environ.get('STAGE21G_SPELL_COMPAT')=='1':ids|={'spell_engine','spell_power'}
for p in (root/'build/stage20e-compat/mods').glob('*.jar'):
 with zipfile.ZipFile(p) as z:
  if 'fabric.mod.json' in z.namelist() and json.loads(z.read('fabric.mod.json')).get('id') in ids:shutil.copyfile(p,dest/p.name);selected.append(dest/p.name)
patchouli=next((p for p in (root/'build/stage20e-network').glob('run-*/server/mods/*Patchouli*.jar') if '93' in p.name),None)
if patchouli is None:raise RuntimeError('No cached production Patchouli93; protected game installation must remain untouched')
shutil.copyfile(patchouli,dest/patchouli.name)
subprocess.run([sys.executable,str(root/'tools/prepare-stage20g-production.py'),'--mods-dir',str(dest),'--without-ctov','--with-better-combat'],check=True,cwd=root)
for role in ['Server','One','Two']:
 p=root/'build/stage20e-network'/f'{role}.production.launch.json';data=json.loads(p.read_text(encoding='utf-8'))
 data['mods']=list(dict.fromkeys(data['mods']+[str(p) for p in selected]))
 p.write_text(json.dumps(data,indent=2),encoding='utf-8')
print('Stage21F original optional jars, no CTOV, no access to user game world')
