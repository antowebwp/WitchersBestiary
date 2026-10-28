$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$outDir = Join-Path $repoRoot 'build/hunt-search-unit'
New-Item -ItemType Directory -Force $outDir | Out-Null
$javaBin = Join-Path $env:JAVA_HOME 'bin'
& (Join-Path $javaBin 'javac.exe') -d $outDir (Join-Path $repoRoot 'src/main/java/com/witchersbestiary/contracts/hunt/HuntSearchQueue.java') (Join-Path $repoRoot 'src/huntUnit/java/HuntSearchQueueTest.java')
if ($LASTEXITCODE -ne 0) { throw 'Search queue compilation failed' }
& (Join-Path $javaBin 'java.exe') -ea -cp $outDir HuntSearchQueueTest
if ($LASTEXITCODE -ne 0) { throw 'Search queue checks failed' }
