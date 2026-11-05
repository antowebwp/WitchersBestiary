"""Bounded WITH/WITHOUT compatibility campaigns, reusing Stage21G native scenarios."""
from pathlib import Path
import argparse,subprocess,sys,os,json
root=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--with-world',type=Path,required=True);a=p.parse_args()
summary=root/'build/stage21h-summary.json';state=json.loads(summary.read_text(encoding='utf-8')) if summary.exists() else {}
base=a.with_world.resolve().parent.parent
proof=json.loads((base/'complete.json').read_text(encoding='utf-8'));assert proof.get('complete') and proof.get('skill_tree_rpgs')=='1.4.4+1.21.1'
state['with_gameplay']={'directory':str(base),'result':proof}
def save():summary.write_text(json.dumps(state,indent=2),encoding='utf-8')
def run(name,profile,visual=False,world=None):
 if name in state and state[name]['result'].get('complete'):return Path(state[name]['directory'])/'server/world'
 env=os.environ.copy();env.update(STAGE20G_PRODUCTION='1',STAGE21G_NETWORK='1',STAGE21H_PROFILE=profile,STAGE21G_SPELL_COMPAT='1')
 for key in ['STAGE21G_VISUAL','STAGE21G_RECOVER_WORLD']:env.pop(key,None)
 if visual:env['STAGE21G_VISUAL']='1';env['STAGE21G_RECOVER_WORLD']=str(world)
 args=[sys.executable,str(root/'tools/prepare-stage21h-production.py')]+(['--without-skill-tree'] if profile=='without' else [])
 subprocess.run(args,cwd=root,env=env,check=True)
 manifest=json.loads((root/'build/stage21h-compat/manifest.json').read_text(encoding='utf-8'))
 subprocess.run([sys.executable,str(root/'tools/run-stage20e-network.py')],cwd=root,env=env,check=True)
 case=Path(json.loads((root/'build/stage20e-network/current-run.json').read_text())['directory']);result=json.loads((case/'complete.json').read_text());assert result['complete'];assert result.get('skill_tree_rpgs')==('1.4.4+1.21.1' if profile=='with' else 'absent')
 state[name]={'directory':str(case),'result':result,'profile':manifest};save();return case/'server/world'
save();run('with_visual','with',True,a.with_world)
world=run('without_gameplay','without');run('without_visual','without',True,world)
print('All four actual native WITH/WITHOUT campaigns complete:',summary)
