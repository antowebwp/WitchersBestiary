"""Launch the remapped RC1 capture harness with original Sodium/Iris jars."""
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument("renderer_mods", type=Path)
parser.add_argument("--sodium-only", action="store_true")
args = parser.parse_args()
root = Path.cwd()
output = root / "build" / ("arcane-fx-v2-sodium" if args.sodium_only else "arcane-fx-v2-iris")
client = output / "client"
mods = client / "mods"
mods.mkdir(parents=True, exist_ok=True)
cache = Path.home() / ".gradle" / "caches"
plan = json.loads((root / "build/stage20e-network/One.launch.json").read_text())
launch_config = (root / ".gradle/loom-cache/launch.cfg").read_text().splitlines()


def one(parent, pattern):
    return next(p for p in parent.rglob(pattern) if p.is_file() and not p.name.endswith("-sources.jar"))


def configured(key):
    index = next(i for i, line in enumerate(launch_config) if line.strip() == key)
    return launch_config[index + 1].strip()


libraries = [
    one(cache, "minecraft-common-intermediary-1.21.1-*.jar"),
    one(cache, "minecraft-clientonly-intermediary-1.21.1-*.jar"),
    one(cache, "intermediary-1.21.1-v2.jar"),
]
for item in plan["classpath"].split(os.pathsep):
    path = Path(item)
    if not path.is_file() or path.suffix != ".jar" or path.name.startswith("minecraft-"):
        continue
    if "remapped_mods" in item or "minecraftMaven" in item or path.name == "mappings.jar":
        continue
    with zipfile.ZipFile(path) as jar:
        mod = "fabric.mod.json" in jar.namelist()
    if not mod or path.name.startswith(("fabric-loader-", "mixinextras-")):
        libraries.append(path)

artifacts = [
    one(cache, "fabric-api-0.116.17+1.21.1.jar"),
    one(cache, "geckolib-fabric-1.21.1-4.8.4.jar"),
    one(cache / "modules-2/files-2.1/maven.modrinth/nU0bVIaL", "*.jar"),
    one(cache / "modules-2/files-2.1/maven.modrinth/cascademc/oSSE53mh", "*.jar"),
    root / "build/libs/witchersbestiary-0.22.0-dev.jar",
    root / "build/arcane-rc1-harness/witchersbestiary-0.22.0-dev-arcane-rc1-review.jar",
    one(args.renderer_mods, "sodium-fabric-0.6.13*.jar"),
]
if not args.sodium_only:
    artifacts.append(one(args.renderer_mods, "iris-fabric-1.8.8*.jar"))
for artifact in artifacts:
    shutil.copy2(artifact, mods / artifact.name)

(client / "options.txt").write_text("tutorialStep:none\nlang:en_us\nsoundCategory_master:0.0\n")
java = shutil.which("java")
if java is None:
    raise RuntimeError("Java 21 not available")
command = [
    "-Xmx2G", "-Dfabric.development=false", "-Dwitchersbestiary.rc1Compat=true",
    "-cp", os.pathsep.join(str(p) for p in dict.fromkeys(libraries)),
    "net.fabricmc.loader.impl.launch.knot.KnotClient",
    "--username", "ArcaneRc1", "--width", "960", "--height", "540", "--accessToken", "0",
    "--version", "1.21.1", "--assetIndex", configured("--assetIndex"), "--assetsDir", configured("--assetsDir"),
]
argfile = output / "client.args"
argfile.write_text("\n".join('"' + arg.replace("\\", "\\\\").replace('"', '\\"') + '"' for arg in command))
with (output / "run.log").open("w") as log:
    subprocess.run([java, f"@{argfile}"], cwd=client, stdout=log, stderr=subprocess.STDOUT, check=True, timeout=320)
if not (output / "result.txt").is_file():
    raise RuntimeError("Production client exited without RC1 smoke results")
