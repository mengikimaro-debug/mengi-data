# Builds the native offline Android APK for Mengidata.
#
# Usage (from the project root):
#   powershell -ExecutionPolicy Bypass -File scripts\build-apk.ps1
#
# Requirements: JDK 17+, Android SDK (build-tools + platform 35).
# The APK bundles the offline React UI in a local Android WebView.

[CmdletBinding()]
param(
    [string]$SdkRoot = "",
    [string]$VersionName = "2.1.0",
    [int]$VersionCode = 13
)

$ErrorActionPreference = "Continue"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Stop-Build([string]$message) {
    Write-Host ""
    Write-Host "ERROR: $message" -ForegroundColor Red
    exit 1
}

Write-Host "Inajenga UI ya app..." -ForegroundColor Cyan
npm run build
if ($LASTEXITCODE -ne 0) {
    Stop-Build "UI ya React haikuweza kujengwa."
}

function Invoke-Tool([string]$exe, [string[]]$toolArgs) {
    Write-Host (">> " + (Split-Path $exe -Leaf)) -ForegroundColor DarkGray
    & $exe @toolArgs 2>&1 | ForEach-Object { Write-Host $_ }
    if ($LASTEXITCODE -ne 0) {
        Stop-Build ("$exe imeshindwa (exit code $LASTEXITCODE).")
    }
}

# ---------------------------------------------------------------- locate SDK
$sdkCandidates = @(
    $SdkRoot,
    $env:ANDROID_SDK_ROOT,
    $env:ANDROID_HOME,
    (Join-Path $env:LOCALAPPDATA "Android\Sdk"),
    (Join-Path $env:USERPROFILE "AppData\Local\Android\Sdk")
)
$sdk = $sdkCandidates | Where-Object { $_ -and (Test-Path (Join-Path $_ "platforms")) } | Select-Object -First 1
if (-not $sdk) {
    Stop-Build "Android SDK haipatikani. Weka ANDROID_SDK_ROOT au tumia -SdkRoot."
}
Write-Host "SDK: $sdk"

$buildToolsDir = Get-ChildItem (Join-Path $sdk "build-tools") -Directory -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -match '^\d+(\.\d+)+$' -and (Test-Path (Join-Path $_.FullName "aapt2.exe")) } |
    Sort-Object { [version]$_.Name } -Descending |
    Select-Object -First 1
if (-not $buildToolsDir) {
    Stop-Build "build-tools haipatikani kwenye SDK."
}
$bt = $buildToolsDir.FullName
Write-Host "build-tools: $($buildToolsDir.Name)"

$platformDir = $null
$preferredJar = Join-Path $sdk "platforms\android-35\android.jar"
if (Test-Path $preferredJar) {
    $androidJar = $preferredJar
    Write-Host "platform: android-35"
} else {
    $platformDir = Get-ChildItem (Join-Path $sdk "platforms") -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch 'ext|canary|beta|rc' -and (Test-Path (Join-Path $_.FullName "android.jar")) } |
        Sort-Object {
            $n = $_.Name -replace '^android-', ''
            if ($n -match '^\d+$') { [int]$n } else { 0 }
        } -Descending |
        Select-Object -First 1
    if (-not $platformDir) {
        Stop-Build "Android platform (android.jar) haipatikani kwenye SDK."
    }
    $androidJar = Join-Path $platformDir.FullName "android.jar"
    Write-Host "platform: $($platformDir.Name)"
}

# ---------------------------------------------------------------- locate Java
$javaCandidates = @()
if ($env:JAVA_HOME) { $javaCandidates += (Join-Path $env:JAVA_HOME "bin\javac.exe") }
$javaCandidates += "C:\Program Files\Java\jdk-21\bin\javac.exe"
$javaCandidates += "C:\Program Files\Java\jdk-17\bin\javac.exe"
foreach ($base in @("C:\Program Files\Java", "C:\Program Files\Eclipse Adoptium", "C:\Program Files\Microsoft")) {
    if (Test-Path $base) {
        Get-ChildItem $base -Directory -ErrorAction SilentlyContinue | ForEach-Object {
            $javaCandidates += (Join-Path $_.FullName "bin\javac.exe")
        }
    }
}
$javac = $javaCandidates | Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1
if (-not $javac) {
    Stop-Build "JDK (javac) haipatikani. Sakinisha JDK 17 au weka JAVA_HOME."
}
$javaHome = Split-Path (Split-Path $javac)
$env:JAVA_HOME = $javaHome
$env:PATH = (Join-Path $javaHome "bin") + ";" + $env:PATH
Write-Host "Java: $javaHome"

$keytool = Join-Path $javaHome "bin\keytool.exe"
$apksigner = Join-Path $bt "apksigner.bat"
$zipalign = Join-Path $bt "zipalign.exe"
$aapt2 = Join-Path $bt "aapt2.exe"
$d8 = Join-Path $bt "d8.bat"

foreach ($tool in @($aapt2, $d8, $zipalign, $apksigner, $keytool)) {
    if (-not (Test-Path $tool)) { Stop-Build "Chombo hakipo: $tool" }
}

# ---------------------------------------------------------------- native build dirs
$buildDir = Join-Path $root "android\build"
$classesDir = Join-Path $buildDir "classes"
$dexDir = Join-Path $buildDir "dex"
$generatedDir = Join-Path $buildDir "generated"

if (Test-Path $buildDir) { Remove-Item $buildDir -Recurse -Force }
New-Item -ItemType Directory -Force -Path $buildDir, $classesDir, $dexDir, $generatedDir | Out-Null

# ---------------------------------------------------------------- resources
Write-Host "Inaandaa rasilimali..." -ForegroundColor Cyan
$resZip = Join-Path $buildDir "res.zip"
$baseApk = Join-Path $buildDir "base.apk"
$manifest = Join-Path $root "android\AndroidManifest.xml"
$resDir = Join-Path $root "android\res"

# aapt2 on Windows mishandles absolute paths that contain non-ASCII characters
# (the project folder has an em dash), so feed it paths relative to the root.
$resZipArg = "android\build\res.zip"
$baseApkArg = "android\build\base.apk"
$manifestArg = "android\build\AndroidManifest.xml"
$resDirArg = "android\res"
$assetsDir = Join-Path $root "android\assets"
$manifestText = Get-Content (Join-Path $root "android\AndroidManifest.xml") -Raw
$manifestText = $manifestText -replace '<manifest\s+xmlns:android="([^"]+)"', '<manifest xmlns:android="$1" package="tz.mengidata.app"'
Set-Content -Path (Join-Path $buildDir "AndroidManifest.xml") -Value $manifestText -Encoding UTF8

Invoke-Tool $aapt2 @("compile", "--dir", $resDirArg, "-o", $resZipArg)
Invoke-Tool $aapt2 @(
    "link", "-o", $baseApkArg, "-I", $androidJar,
    "--java", "android\build\generated",
    "--manifest", $manifestArg,
    "--min-sdk-version", "24", "--target-sdk-version", "34",
    "--version-code", "$VersionCode", "--version-name", $VersionName,
    "--auto-add-overlay", $resZipArg
)

# ---------------------------------------------------------------- java + dex
Write-Host "Inakusanya Java..." -ForegroundColor Cyan
$javaFiles = Get-ChildItem (Join-Path $root "native\java") -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$javaFiles += Get-ChildItem $generatedDir -Recurse -Filter *.java | ForEach-Object { $_.FullName }
Invoke-Tool $javac (@("-nowarn", "-encoding", "UTF-8", "-source", "11", "-target", "11", "-classpath", $androidJar, "-d", $classesDir) + $javaFiles)

Write-Host "Inatengeneza dex..." -ForegroundColor Cyan
$classFiles = Get-ChildItem $classesDir -Recurse -Filter *.class | ForEach-Object { $_.FullName }
Invoke-Tool $d8 (@("--release", "--min-api", "24", "--lib", $androidJar, "--output", $dexDir) + $classFiles)
$dexFile = Join-Path $dexDir "classes.dex"
if (-not (Test-Path $dexFile)) { Stop-Build "classes.dex haikutengenezwa." }

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open($baseApk, [System.IO.Compression.ZipArchiveMode]::Update)
try {
    $existing = $zip.GetEntry("classes.dex")
    if ($existing) { $existing.Delete() }
    [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
        $zip, $dexFile, "classes.dex", [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
    foreach ($entry in @($zip.Entries | Where-Object { $_.FullName.StartsWith("assets/") })) {
        $entry.Delete()
    }
    Get-ChildItem $assetsDir -Recurse -File | ForEach-Object {
        $relative = $_.FullName.Substring($assetsDir.Length).TrimStart('\') -replace '\\', '/'
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
            $zip, $_.FullName, "assets/$relative", [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
    $webDir = Join-Path $root "dist"
    Get-ChildItem $webDir -Recurse -File | ForEach-Object {
        $relative = $_.FullName.Substring($webDir.Length).TrimStart('\') -replace '\\', '/'
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
            $zip, $_.FullName, "assets/www/$relative", [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
} finally {
    $zip.Dispose()
}

$alignedApk = Join-Path $buildDir "aligned.apk"
Invoke-Tool $zipalign @("-f", "-p", "4", $baseApk, $alignedApk)

# ---------------------------------------------------------------- signing
$keystore = Join-Path $root "android\keystore\mengi-data.keystore"
$storePass = $env:MENGI_SIGNING_STORE_PASSWORD
$keyPass = $env:MENGI_SIGNING_KEY_PASSWORD
$alias = $env:MENGI_SIGNING_KEY_ALIAS
if (-not $storePass) {
    Stop-Build "Weka MENGI_SIGNING_STORE_PASSWORD kwenye environment kabla ya kusaini APK."
}
if (-not $keyPass) { $keyPass = $storePass }
if (-not $alias) { $alias = "mengidata" }

if (-not (Test-Path $keystore)) {
    Write-Host "Inaunda keystore ya kudumu..." -ForegroundColor Cyan
    New-Item -ItemType Directory -Force (Split-Path $keystore) | Out-Null
    Invoke-Tool $keytool @(
        "-genkeypair", "-keystore", $keystore,
        "-storepass", $storePass, "-keypass", $keyPass,
        "-alias", $alias, "-keyalg", "RSA", "-keysize", "4096",
        "-validity", "10000",
        "-dname", "CN=HOME SATELLITE TV, OU=Mobile, O=HOME SATELLITE TV, L=Dar es Salaam, C=TZ"
    )
}

$signedApk = Join-Path $buildDir ("Mengidata-" + $VersionName + ".apk")
Invoke-Tool $apksigner @(
    "sign",
    "--ks", $keystore,
    "--ks-key-alias", $alias,
    "--ks-pass", ("pass:" + $storePass),
    "--key-pass", ("pass:" + $keyPass),
    "--out", $signedApk,
    $alignedApk
)
Invoke-Tool $apksigner @("verify", "--verbose", "--print-certs", $signedApk)

# ---------------------------------------------------------------- deliver
$finalApk = Join-Path $root "Mengidata.apk"
Move-Item $signedApk $finalApk -Force
$size = [math]::Round((Get-Item $finalApk).Length / 1MB, 2)
$hash = (Get-FileHash $finalApk -Algorithm SHA256).Hash

Write-Host ""
Write-Host "APK tayari:" -ForegroundColor Green
Write-Host "  $finalApk  ($size MB)"
Write-Host "  SHA-256: $hash"
Write-Host ""
Write-Host "Hamishia faili hili kwenye simu na ulikubali usakinishaji wa 'Unknown apps'."
