$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskTracked = & git -C $taskRoot -c core.quotepath=false ls-files
if ($LASTEXITCODE -ne 0) { throw 'Préparez la livraison depuis un clone Git du projet.' }
& git -C $taskRoot diff --quiet HEAD --
if ($LASTEXITCODE -ne 0) { throw 'Enregistrez les changements dans Git avant de préparer les sources de la livraison.' }
foreach ($taskRequired in @('LICENSE', 'NOTICE', 'THIRD_PARTY_NOTICES.md', 'licenses/Apache-2.0.txt', 'licenses/Shizuku-API-MIT.txt')) {
    if ($taskRequired -notin $taskTracked) { throw "Fichier légal absent du suivi Git : $taskRequired" }
}
$taskForbidden = $taskTracked | Where-Object {
    $_ -match '(^|/)(artifacts|dist|build|\.gradle|\.idea)(/|$)' -or
    $_ -match '(^|/)(signing\.properties|local\.properties|\.env(?:\..*)?)$' -or
    $_ -match '\.(jks|keystore|pem|p12|pfx|key|apk)$'
}
if ($taskForbidden) { throw 'Un fichier privé ou généré figure dans Git. Corrigez le suivi avant de préparer la livraison.' }
$taskApk = Join-Path $taskRoot 'app\build\outputs\apk\release\app-release.apk'
if (!(Test-Path -LiteralPath $taskApk)) { throw 'Exécutez scripts/build.ps1 -Release -Check avant de préparer la livraison.' }
$taskMetadata = Get-Content -Raw -LiteralPath (Join-Path $taskRoot 'app\build\outputs\apk\release\output-metadata.json') | ConvertFrom-Json
$taskVersion = $taskMetadata.elements[0].versionName
if ($taskVersion -notmatch '^\d+\.\d+\.\d+$') { throw 'Version de livraison invalide.' }
$taskDist = Join-Path $taskRoot 'dist'
$null = New-Item -ItemType Directory -Path $taskDist -Force
Copy-Item -LiteralPath $taskApk -Destination (Join-Path $taskDist "KishiSwitch-$taskVersion.apk")
foreach ($taskDoc in @('NOTICE.md', 'NOTICE.fr.md', 'VALIDATION.md', 'VALIDATION.fr.md')) {
    Copy-Item -LiteralPath (Join-Path $taskRoot "docs\$taskDoc") -Destination $taskDist
}
foreach ($taskLegal in @('LICENSE', 'NOTICE', 'THIRD_PARTY_NOTICES.md')) {
    Copy-Item -LiteralPath (Join-Path $taskRoot $taskLegal) -Destination $taskDist
}

$taskZipPath = Join-Path $taskDist "KishiSwitch-$taskVersion-sources.zip"
& git -C $taskRoot archive --format=zip "--output=$taskZipPath" HEAD
if ($LASTEXITCODE -ne 0) { throw 'Création des sources impossible.' }
$taskLegalZipPath = Join-Path $taskDist "KishiSwitch-$taskVersion-licenses.zip"
& git -C $taskRoot archive --format=zip "--output=$taskLegalZipPath" HEAD LICENSE NOTICE THIRD_PARTY_NOTICES.md licenses
if ($LASTEXITCODE -ne 0) { throw 'Création des mentions légales impossible.' }
$taskReleaseFiles = @("KishiSwitch-$taskVersion.apk", "KishiSwitch-$taskVersion-sources.zip", "KishiSwitch-$taskVersion-licenses.zip", 'LICENSE', 'NOTICE', 'THIRD_PARTY_NOTICES.md', 'NOTICE.md', 'NOTICE.fr.md', 'VALIDATION.md', 'VALIDATION.fr.md') |
    ForEach-Object { Get-Item -LiteralPath (Join-Path $taskDist $_) }
$taskHashes = $taskReleaseFiles |
    ForEach-Object { "$( (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant() )  $($_.Name)" }
[IO.File]::WriteAllLines((Join-Path $taskDist 'SHA256SUMS.txt'), $taskHashes, [Text.Encoding]::ASCII)
Get-ChildItem -LiteralPath $taskDist -File | Select-Object Name, Length
