$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$mavenExecutable = Get-Command mvn.cmd -ErrorAction SilentlyContinue
$demoJdk = $null
if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/javac.exe'))) {
    $demoJdk = $env:JAVA_HOME
}
$ideaInstalls = Get-ChildItem -LiteralPath 'C:\Program Files\JetBrains' -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending
foreach ($ideaInstall in $ideaInstalls) {
    $bundledJdk = Join-Path $ideaInstall.FullName 'jbr'
    $bundledMaven = Join-Path $ideaInstall.FullName 'plugins/maven-plugin/lib/maven3/bin/mvn.cmd'
    if (-not $demoJdk -and (Test-Path -LiteralPath (Join-Path $bundledJdk 'bin/javac.exe'))) { $demoJdk = $bundledJdk }
    if (-not $mavenExecutable -and (Test-Path -LiteralPath $bundledMaven)) { $mavenExecutable = $bundledMaven }
}
if (-not $demoJdk -or -not $mavenExecutable) { throw 'Instala JDK 21 o compatible y Maven, o configura el JDK de IntelliJ.' }
$env:JAVA_HOME = $demoJdk
Push-Location (Join-Path $projectRoot 'backend')
try {
    & $mavenExecutable "-Dmaven.repo.local=$projectRoot\.tools\m2" --no-transfer-progress spring-boot:run '-Dspring-boot.run.profiles=demo'
} finally { Pop-Location }
