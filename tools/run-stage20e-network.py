"""Bounded localhost review using two real native clients and up to three server JVM runs.
Requires JAVA_HOME (21), and Gradle stage20ePrepareLaunch -Pstage19fProfile=A first.
All generated settings/worlds/logs stay in a fresh ignored build/ directory.
"""
import os, json, subprocess, time, datetime, shutil
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'build/stage20e-network'
CASE = BASE / ('run-' + datetime.datetime.now().strftime('%Y%m%d-%H%M%S'))
JAVA = Path(os.environ['JAVA_HOME']) / 'bin/java.exe'
CASE.mkdir(parents=True)
(BASE / 'current-run.json').write_text(json.dumps({'directory':str(CASE)}))

def quote(value):
    return '"' + str(value).replace('\\', '\\\\').replace('"','\\"') + '"'

plans = {}
for role in ['Server', 'One', 'Two']:
    production=os.environ.get('STAGE20G_PRODUCTION') == '1'
    data = json.loads((BASE / (f'{role}.production.launch.json' if production else f'{role}.launch.json')).read_text())
    env = 'server' if role == 'Server' else 'client'
    cwd = CASE / role.lower()
    cwd.mkdir()
    if role != 'Server': (cwd/'options.txt').write_text('tutorialStep:none\nlang:'+('ru_ru' if (os.environ.get('STAGE21E_NETWORK')=='1' or os.environ.get('STAGE21G_NETWORK')=='1') and role=='Two' else 'en_us')+'\nsoundCategory_master:0.0\n')
    jvm = [f'-Dfabric.dli.config={ROOT}/.gradle/loom-cache/launch.cfg',
           f'-Dfabric.dli.env={env}', f'-Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.Knot{env.capitalize()}',
           f'-Dwitchersbestiary.stage20eOutput={CASE}', '-Xmx2G' if role == 'Server' else '-Xmx1G']
    if production:
        jvm=[a for a in jvm if not a.startswith('-Dfabric.dli.')] + ['-Dfabric.development=false']
        mods=cwd/'mods';mods.mkdir()
        for source in data['mods']: shutil.copyfile(source,mods/Path(source).name)
    if os.environ.get('STAGE20G_VILLAGE') == '1': jvm += ['-Dwitchersbestiary.stage20gVillage=true']
    if os.environ.get('STAGE20G_CTOV_ONLY') == '1': jvm += ['-Dwitchersbestiary.stage20gCtovOnly=true']
    for env,key in [('STAGE20G_VILLAGE_ID','stage20gVillageId'),('STAGE20G_VILLAGE_TAG','stage20gVillageTag')]:
        if os.environ.get(env): jvm += [f'-Dwitchersbestiary.{key}={os.environ[env]}']
    if os.environ.get('STAGE20G_VILLAGE_ONLY') == '1': jvm += ['-Dwitchersbestiary.stage20gVillageOnly=true']
    if os.environ.get('STAGE21E_CTOV') == '1': jvm += ['-Dwitchersbestiary.stage21eCtov=true']
    if os.environ.get('STAGE21E_NATURAL') == '1': jvm += ['-Dwitchersbestiary.stage21eNatural=true']
    if os.environ.get('COMBINED_HOTFIX')=='1': jvm += ['-Dwitchersbestiary.combinedHotfix=true']
    if os.environ.get('STAGE21H_PROFILE'): jvm += ['-Dwitchersbestiary.stage21h='+os.environ['STAGE21H_PROFILE']]
    if os.environ.get('STAGE21G_VISUAL') == '1': jvm += ['-Dwitchersbestiary.stage21gVisual=true','-Dwitchersbestiary.duelReview=true']
    if os.environ.get('STAGE21E_NETWORK') == '1': jvm += ['-Dwitchersbestiary.duelReview=true']
    if os.environ.get('DUEL_NETWORK') == '1': jvm += ['-Dwitchersbestiary.duelReview=true']
    if os.environ.get('CONTRACT_RELIABILITY_REVIEW') == '1': jvm += ['-Dwitchersbestiary.contractReliabilityReview=true']
    if os.environ.get('STAGE21C_MODEL_ONLY') == '1': jvm += ['-Dwitchersbestiary.stage21cModelOnly=true']
    if os.environ.get('STAGE21C_RESUME_TROLL') == '1': jvm += ['-Dwitchersbestiary.stage21cResumeTroll=true']
    if os.environ.get('STAGE21C_NATURAL_ONLY') == '1': jvm += ['-Dwitchersbestiary.stage21cNaturalOnly=true']
    if os.environ.get('STAGE21C_BOUT_ONLY') == '1': jvm += ['-Dwitchersbestiary.stage21cBoutOnly=true']
    if os.environ.get('STAGE21B_HUD_ONLY') == '1': jvm += ['-Dwitchersbestiary.stage21bHudOnly=true']
    if role != 'Server' and os.environ.get('STAGE21B_RECOVER_CONFIG'):
        source_config=Path(os.environ['STAGE21B_RECOVER_CONFIG'])/role.lower()/'config'
        if source_config.is_dir(): shutil.copytree(source_config,cwd/'config',dirs_exist_ok=True)
    if role == 'Server':
        jvm += ['-Dwitchersbestiary.stage21gNetwork=true'] if os.environ.get('STAGE21G_NETWORK')=='1' else ['-Dwitchersbestiary.stage21fNetwork=true'] if os.environ.get('STAGE21F_NETWORK')=='1' else ['-Dwitchersbestiary.stage21eNetwork=true'] if os.environ.get('STAGE21E_NETWORK')=='1' else ['-Dwitchersbestiary.duelNetwork=true'] if os.environ.get('DUEL_NETWORK')=='1' else ['-Dwitchersbestiary.contractReliabilityNetwork=true'] if os.environ.get('CONTRACT_RELIABILITY_NETWORK')=='1' else ['-Dwitchersbestiary.stage21aNetwork=true'] if os.environ.get('STAGE21A_NETWORK')=='1' else ['-Dwitchersbestiary.stage21bNetwork=true'] if os.environ.get('STAGE21B_NETWORK')=='1' else ['-Dwitchersbestiary.stage20eServer=true']
        if os.environ.get('STAGE20G_VILLAGE') == '1': jvm += ['-Dwitchersbestiary.stage20gVillage=true']
        args = ['--nogui']
        (cwd / 'eula.txt').write_text('eula=true\n')
        (cwd / 'server.properties').write_text('server-ip=127.0.0.1\nserver-port=25583\nonline-mode=false\nenforce-secure-profile=false\nview-distance=4\nsimulation-distance=4\nmax-players=2\nlevel-name=world\nlevel-seed=202001\nlevel-type=minecraft\\:normal\ngenerate-structures=true\nspawn-protection=0\ndifficulty=normal\nenable-rcon=false\n')
    else:
        jvm += [f'-Dwitchersbestiary.stage20eClient=WB{role}']
        args = ['--username', f'WB{role}', '--width','1280' if (os.environ.get('DUEL_NETWORK')=='1' or os.environ.get('STAGE21E_NETWORK')=='1' or os.environ.get('STAGE21G_VISUAL')=='1') and role=='Two' else '960','--height','900' if (os.environ.get('DUEL_NETWORK')=='1' or os.environ.get('STAGE21E_NETWORK')=='1' or os.environ.get('STAGE21G_VISUAL')=='1') and role=='Two' else '640']
    if production and role != 'Server': args += data['clientArgs']
    if role == 'Server' and os.environ.get('STAGE21G_RECOVER_WORLD'):
        source=Path(os.environ['STAGE21G_RECOVER_WORLD']).resolve()
        if not source.is_relative_to(BASE.resolve()) or source.name!='world' or source.parent.name!='server':raise RuntimeError('Signs recovery must use isolated review world')
        shutil.copytree(source,cwd/'world')
    if role == 'Server' and (os.environ.get('STAGE21A_RECOVER_WORLD') or os.environ.get('STAGE21B_RECOVER_WORLD')):
        source=Path(os.environ.get('STAGE21A_RECOVER_WORLD') or os.environ['STAGE21B_RECOVER_WORLD']).resolve()
        if not source.is_relative_to(BASE.resolve()) or source.name!='world' or source.parent.name!='server': raise RuntimeError('Recovery must use an isolated review world')
        shutil.copytree(source,cwd/'world')
        marker='fist-resume.json' if os.environ.get('STAGE21B_RECOVER_WORLD') else 'wallet-resume.json'
        if os.environ.get("CONTRACT_RELIABILITY_REVIEW") != "1": shutil.copyfile(source.parent.parent/marker,CASE/marker)
    if role == 'Server' and os.environ.get('STAGE21E_RECOVER_NATURAL_WORLD'):
        source=Path(os.environ['STAGE21E_RECOVER_NATURAL_WORLD']).resolve()
        if not source.is_relative_to(BASE.resolve()) or source.name!='world' or source.parent.name!='server': raise RuntimeError('Natural review recovery must use an isolated world')
        shutil.copytree(source,cwd/'world')
    if role == 'Server' and os.environ.get('STAGE21E_RECOVER_WORLD'):
        source=Path(os.environ['STAGE21E_RECOVER_WORLD']).resolve()
        if not source.is_relative_to(BASE.resolve()) or source.name!='world' or source.parent.name!='server': raise RuntimeError('Stage21E recovery must use an isolated review world')
        shutil.copytree(source,cwd/'world')
        for marker in ['alchemy-resume.json','alchemy-results.json']:
            shutil.copyfile(source.parent.parent/marker,CASE/marker)
    if role == 'Server' and os.environ.get('DUEL_NETWORK') == '1':
        config=cwd/'server.properties';config.write_text(config.read_text()+'pvp='+os.environ.get('DUEL_PVP','true')+'\n')
    if role == 'Server' and os.environ.get('STAGE20G_VILLAGE') == '1':
        config=cwd/'server.properties'
        config.write_text(config.read_text().replace('view-distance=4','view-distance=8'))
    if role == 'Server' and os.environ.get('STAGE20G_SEED'):
        config=cwd/'server.properties';config.write_text(config.read_text().replace('level-seed=202001','level-seed='+os.environ['STAGE20G_SEED']))
    argfile = CASE / f'{role}.args'
    argfile.write_text('\n'.join(quote(a) for a in jvm + ['-cp',data['classpath'],data['main']] + args))
    plans[role] = (cwd, argfile)

processes = []
counts = {}
def launch(role):
    count = counts.get(role,0)+1
    counts[role] = count
    cwd, argfile = plans[role]
    log = (CASE / f'{role}-{count}.log').open('wb')
    process = subprocess.Popen([str(JAVA), '@'+str(argfile)], cwd=cwd, stdin=subprocess.PIPE, stdout=log, stderr=subprocess.STDOUT,
                               creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
    processes.append((role,process,log))
    (CASE / 'processes.json').write_text(json.dumps([{'role':r,'pid':p.pid} for r,p,l in processes]))
    print(f'{role} JVM run {count}, PID {process.pid}',flush=True)
    return process

started = time.monotonic()
try:
    server = launch('Server')
    time.sleep(3)
    launch('One'); launch('Two')
    while time.monotonic()-started < 900:
        failures = list(CASE.glob('*.failure.txt')) + list(CASE.glob('*.timeout.txt'))
        if (CASE/'server-failure.json').exists(): failures.append(CASE/'server-failure.json')
        if (os.environ.get('STAGE21G_NETWORK') == '1' or os.environ.get('STAGE21F_NETWORK') == '1' or os.environ.get('STAGE21E_NETWORK') == '1' or os.environ.get('DUEL_NETWORK') == '1' or os.environ.get('CONTRACT_RELIABILITY_NETWORK') == '1' or os.environ.get('STAGE21A_NETWORK') == '1' or os.environ.get('STAGE21B_NETWORK') == '1') and (CASE/'complete.json').exists() and server.poll() is None:
            try: server.stdin.write(b'stop\n'); server.stdin.flush()
            except OSError: pass
        if any(p.poll() is not None for r,p,l in processes if r != 'Server') and not (CASE/'complete.json').exists(): raise RuntimeError('A native client exited before completion; inspect its log')
        if failures: raise RuntimeError('Review failure: '+', '.join(str(p) for p in failures))
        if server.poll() is not None:
            if (CASE/'complete.json').exists():
                print((CASE/'complete.json').read_text(),flush=True)
                break
            if ((CASE/'resume.json').exists() or (CASE/'village-resume.json').exists()) and counts['Server'] < 3:
                if os.environ.get('STAGE20G_VILLAGE_ONLY') == '1' and counts['Server'] == 1:
                    ledger=(CASE/'server/world/data/witchersbestiary_village_boards.dat').resolve()
                    backup=(CASE/'pre-retro-village-ledger.dat').resolve()
                    if not ledger.is_relative_to(CASE.resolve()) or not backup.is_relative_to(CASE.resolve()): raise RuntimeError('Unsafe review ledger path')
                    ledger.rename(backup) # Preserve prior ledger outside isolated world to emulate pre-install village.
                print('Restarting dedicated server from the same saved world.',flush=True)
                server = launch('Server')
            else:
                raise RuntimeError('Server exited without a valid completion/restart marker; see '+str(CASE))
        time.sleep(1)
    else: raise TimeoutError('900-second orchestration limit')
finally:
    for role,process,log in processes:
        if process.poll() is None and role != 'Server' and not (CASE/'complete.json').exists():
            try: (CASE/f'WB{role}.command.json').write_text(json.dumps({'nonce':int(time.time()*1000),'action':'stop'}))
            except OSError: pass
    for role,process,log in processes:
        if role == "Server" and process.poll() is None:
            try: process.stdin.write(b"stop\n"); process.stdin.flush()
            except OSError: pass
    deadline=time.monotonic()+30
    while time.monotonic()<deadline and any(p.poll() is None for _,p,_ in processes): time.sleep(.5)
    for role,process,log in processes:
        if process.poll() is None: process.terminate()
        log.close()
    print('Artifacts: '+str(CASE),flush=True)
