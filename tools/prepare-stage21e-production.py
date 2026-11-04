"""Prepare isolated production-format Stage21E compatibility mods entirely from local caches."""
from pathlib import Path
import shutil,zipfile,json,subprocess,sys
root=Path(__file__).resolve().parents[1];dest=root/'build/stage21e-production-mods';dest.mkdir(exist_ok=True)
files=list((root/'build/stage20g-compat/mods').glob('*.jar'))
files += [p for p in (root/'build/stage20e-compat/mods').glob('*.jar') if any(word in p.name.lower() for word in ['bettercombat','player-animation'])]
patchouli=[]
for p in (root/'build/stage20e-network').glob('run-*/server/mods/*Patchouli*.jar'):
 with zipfile.ZipFile(p) as z:
  if json.loads(z.read('fabric.mod.json')).get('version')=='1.21.1-93-FABRIC':patchouli=[p];break
if not patchouli:raise RuntimeError('Missing local production Patchouli93 copy; do not read the user game installation')
for p in files+patchouli:shutil.copyfile(p,dest/p.name)
subprocess.run([sys.executable,str(root/'tools/prepare-stage20g-production.py'),'--mods-dir',str(dest),'--with-better-combat'],cwd=root,check=True)
