"""Prepare production-namespace review plans using local caches and unmodified installed CTOV deps."""
import argparse,json,os,zipfile
from pathlib import Path
parser=argparse.ArgumentParser();parser.add_argument('--mods-dir',type=Path,required=True);parser.add_argument('--without-ctov',action='store_true');parser.add_argument('--with-moog',action='store_true');args=parser.parse_args()
root=Path(__file__).resolve().parents[1];cache=Path.home()/'.gradle/caches';base=root/'build/stage20e-network'
def one(pattern):
 files=[p for p in cache.rglob(pattern) if p.is_file() and not p.name.endswith(('-sources.jar','-javadoc.jar'))]
 if not files:raise RuntimeError('Missing cached artifact '+pattern)
 return files[0]
mandatory=[one('fabric-api-0.116.17+1.21.1.jar'),one('geckolib-fabric-1.21.1-4.8.4.jar'),next(args.mods_dir.glob('*Patchouli*.jar'))]
optional=[]
for pattern in ([] if args.without_ctov else ['*ctov*.jar','*lithostitched*.jar','*cloth*.jar']):
 values=list(args.mods_dir.glob(pattern))
 if len(values)!=1:raise RuntimeError('Expected one installed '+pattern)
 optional+=values
if args.with_moog:
 optional += [p for p in args.mods_dir.glob('*.jar') if 'moog' in p.name.lower()]
common=one('minecraft-common-intermediary-1.21.1-*.jar');client=one('minecraft-clientonly-intermediary-1.21.1-*.jar');mappings=one('intermediary-1.21.1-v2.jar')
lines=(root/'.gradle/loom-cache/launch.cfg').read_text().splitlines()
def configured(flag):return lines[next(n for n,line in enumerate(lines) if line.strip()==flag)+1].strip()
for role in ['Server','One','Two']:
 data=json.loads((base/(role+'.launch.json')).read_text());libs=[]
 for item in data['classpath'].split(os.pathsep):
  path=Path(item)
  if not path.is_file() or path.suffix!='.jar' or 'remapped_mods' in item or 'minecraftMaven' in item or path.name=='mappings.jar' or path.name.startswith('minecraft-'):continue
  with zipfile.ZipFile(path) as z:mod='fabric.mod.json' in z.namelist()
  if mod and not path.name.startswith(('fabric-loader-','mixinextras-')):continue
  libs.append(path)
 side='server' if role=='Server' else 'client';harnesses=list((root/'build/stage20g-production-harness').glob('*-network-'+side+'.jar'))
 if len(harnesses)!=1:raise RuntimeError('Missing intermediary harness '+side)
 game=[common]+([] if role=='Server' else [client]);classpath=os.pathsep.join(str(p) for p in dict.fromkeys(game+[mappings]+libs))
 mods=mandatory+optional+[root/'build/libs/witchersbestiary-0.20.0-rc3.jar',harnesses[0]]
 result={'main':'net.fabricmc.loader.impl.launch.knot.Knot'+('Server' if role=='Server' else 'Client'),'classpath':classpath,'mods':[str(p) for p in mods],'clientArgs':['--accessToken','0','--version','1.21.1','--assetIndex',configured('--assetIndex'),'--assetsDir',configured('--assetsDir')]}
 (base/(role+'.production.launch.json')).write_text(json.dumps(result,indent=2),encoding='utf-8')
print('Prepared production namespace with '+str(len(optional))+' original optional mod jars, Minecraft intermediary cache and RC3.')
