"""Expose existing bundled runtime libraries for Loom's development launcher. No downloads."""
from pathlib import Path
import json,zipfile
base=Path(__file__).resolve().parents[1]/'build'
out=base/'stage21d-compat';out.mkdir(exist_ok=True)
for name in ['bettercombat-fabric-2.3.2+1.21.1.jar','player-animation-lib-fabric-2.0.4+1.21.1.jar','cloth-config-15.0.140-fabric.jar']:
 with zipfile.ZipFile(base/'stage20e-compat/mods'/name) as z:
  for entry in json.loads(z.read('fabric.mod.json')).get('jars',[]):
   target=out/Path(entry['file']).name;target.write_bytes(z.read(entry['file']));print(target.name)
