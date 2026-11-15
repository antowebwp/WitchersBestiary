"""Exercise RC1 sign and bomb visual packets with a server and two localhost clients."""
import json
from pathlib import Path
import shutil
import socket
import subprocess
import time

root = Path.cwd()
plans = root / "build/stage20e-network"
out = root / "build/arcane-fx-v2-network"
out.mkdir(parents=True, exist_ok=True)
java = shutil.which("java")
if not java:
    raise RuntimeError("Java 21 is required")

with socket.socket() as sock:
    sock.bind(("127.0.0.1", 0))
    port = sock.getsockname()[1]

processes = []
logs = []


def start(plan_name, folder, properties, args):
    plan = json.loads((plans / f"{plan_name}.launch.json").read_text())
    cwd = out / folder
    cwd.mkdir(exist_ok=True)
    jvm = [value for value in plan["jvm"] if "witchersbestiary.stage20e" not in value]
    command = jvm + properties + ["-cp", plan["classpath"], plan["main"]] + args
    argfile = out / f"{folder}.args"
    argfile.write_text("\n".join('"' + arg.replace("\\","\\\\").replace('"','\\"') + '"' for arg in command))
    log = (out / f"{folder}.log").open("w", encoding="utf-8")
    logs.append(log)
    process = subprocess.Popen([java, f"@{argfile}"], cwd=cwd, stdout=log, stderr=subprocess.STDOUT)
    processes.append(process)
    return process


server_dir = out / "server"
server_dir.mkdir(exist_ok=True)
(server_dir / "eula.txt").write_text("eula=true\n")
(server_dir / "server.properties").write_text(
    f"server-ip=127.0.0.1\nserver-port={port}\nonline-mode=false\nenforce-secure-profile=false\n"
    "view-distance=4\nsimulation-distance=4\nmax-players=2\nlevel-name=Rc1Network\n"
    "level-type=minecraft\\:flat\ngenerate-structures=false\nspawn-protection=0\ndifficulty=peaceful\n")
try:
    server = start("Server", "server", ["-Dwitchersbestiary.rc1NetworkServer=true"], ["--nogui"])
    deadline = time.monotonic() + 100
    while True:
        if server.poll() is not None:
            raise RuntimeError("Dedicated server exited before startup")
        if "Done (" in (out / "server.log").read_text(errors="replace"):
            break
        if time.monotonic() >= deadline:
            raise TimeoutError("Dedicated server startup timed out")
        time.sleep(.5)
    for plan_name, name in (("One", "Rc1One"), ("Two", "Rc1Two")):
        directory = out / name
        directory.mkdir(exist_ok=True)
        (directory / "options.txt").write_text("tutorialStep:none\nsoundCategory_master:0.0\n")
        plan = json.loads((plans / f"{plan_name}.launch.json").read_text())
        args = plan["args"][:]
        args[args.index("--username") + 1] = name
        start(plan_name, name, [f"-Dwitchersbestiary.rc1NetworkClient=127.0.0.1:{port}"], args)
    deadline = time.monotonic() + 160
    for process in processes:
        if process.wait(timeout=max(1, deadline - time.monotonic())) != 0:
            raise RuntimeError(f"Localhost process exited with {process.returncode}")
    for name in ("Rc1One", "Rc1Two"):
        path = out / f"{name}.csv"
        if not path.is_file():
            raise RuntimeError(f"Missing real packet recipient result: {name}")
        print(path.read_text().strip())
finally:
    for process in processes:
        if process.poll() is None:
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()
    for log in logs:
        log.close()
