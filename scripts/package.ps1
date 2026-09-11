$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskApk = Join-Path $taskRoot 'app\build\outputs\apk\release\app-release.apk'
if (!(Test-Path -LiteralPath $taskApk)) { throw 'Exécutez scripts/build.ps1 -Release -Check avant de préparer la livraison.' }
$taskMetadata = Get-Content -Raw -LiteralPath (Join-Path $taskRoot 'app\build\outputs\apk\release\output-metadata.json') | ConvertFrom-Json
$taskVersion = $taskMetadata.elements[0].versionName
if ($taskVersion -notmatch '^\d+\.\d+\.\d+$') { throw 'Version de livraison invalide.' }
$taskDist = Join-Path $taskRoot 'dist'
$null = New-Item -ItemType Directory -Path $taskDist -Force
Copy-Item -LiteralPath $taskApk -Destination (Join-Path $taskDist "KishiSwitch-$taskVersion.apk")
Copy-Item -LiteralPath (Join-Path $taskRoot 'docs\NOTICE.md') -Destination $taskDist
Copy-Item -LiteralPath (Join-Path $taskRoot 'docs\VALIDATION.md') -Destination $taskDist

Add-Type -AssemblyName System.IO.Compression
$taskZipPath = Join-Path $taskDist "KishiSwitch-$taskVersion-sources.zip"
$taskStream = [IO.File]::Open($taskZipPath, [IO.FileMode]::Create)
$taskArchive = [IO.Compression.ZipArchive]::new($taskStream, [IO.Compression.ZipArchiveMode]::Create)
try {
    $taskFiles = @('.gitignore', 'README.md', 'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'gradlew', 'gradlew.bat') |
        ForEach-Object { Get-Item -LiteralPath (Join-Path $taskRoot $_) }
    foreach ($taskFolder in @('app', 'engine', 'gradle', 'scripts', 'docs')) {
        $taskFiles += Get-ChildItem -LiteralPath (Join-Path $taskRoot $taskFolder) -File -Recurse -Force |
            Where-Object { $_.FullName -notmatch '[\\/](build|\.gradle|\.idea)[\\/]' -and $_.Extension -notin @('.jks', '.keystore') }
    }
    foreach ($taskFile in $taskFiles) {
        $taskRelative = $taskFile.FullName.Substring($taskRoot.Length + 1).Replace('\', '/')
        $taskEntry = $taskArchive.CreateEntry($taskRelative, [IO.Compression.CompressionLevel]::Optimal)
        $taskInput = [IO.File]::OpenRead($taskFile.FullName)
        $taskOutput = $taskEntry.Open()
        try { $taskInput.CopyTo($taskOutput) } finally { $taskInput.Dispose(); $taskOutput.Dispose() }
    }
} finally { $taskArchive.Dispose(); $taskStream.Dispose() }
$taskReleaseFiles = @("KishiSwitch-$taskVersion.apk", "KishiSwitch-$taskVersion-sources.zip", 'NOTICE.md', 'VALIDATION.md') |
    ForEach-Object { Get-Item -LiteralPath (Join-Path $taskDist $_) }
$taskHashes = $taskReleaseFiles |
    ForEach-Object { "$( (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant() )  $($_.Name)" }
[IO.File]::WriteAllLines((Join-Path $taskDist 'SHA256SUMS.txt'), $taskHashes, [Text.Encoding]::ASCII)
Get-ChildItem -LiteralPath $taskDist -File | Select-Object Name, Length
