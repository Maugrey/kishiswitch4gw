param(
    [switch]$Release,
    [switch]$Check,
    [string]$Jdk = 'C:\Program Files\Android\Android Studio\jbr',
    [string]$Sdk = "$env:LOCALAPPDATA\Android\Sdk"
)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
if (!(Test-Path -LiteralPath "$Jdk\bin\java.exe")) { throw "JDK introuvable : $Jdk" }
if (!(Test-Path -LiteralPath "$Sdk\platforms\android-36\android.jar")) { throw "Installez le SDK Android 36 dans $Sdk" }
$env:JAVA_HOME = $Jdk
$env:ANDROID_HOME = $Sdk
$taskGradle = @()
if ($Check) { $taskGradle += ':engine:test' }
if ($Release) {
    if (!$env:KISHI_SIGNING_PROPERTIES) {
        $env:KISHI_SIGNING_PROPERTIES = & "$PSScriptRoot\prepare-signing.ps1" -Jdk $Jdk
    }
    if (!(Test-Path -LiteralPath $env:KISHI_SIGNING_PROPERTIES)) { throw 'Fichier de signature absent.' }
    $taskGradle += ':app:assembleRelease'
    if ($Check) { $taskGradle += ':app:lintRelease' }
} else {
    $taskGradle += ':app:assembleDebug'
    if ($Check) { $taskGradle += ':app:lintDebug' }
}
Push-Location -LiteralPath $taskRoot
try {
    & .\gradlew.bat @taskGradle --console=plain
    if ($LASTEXITCODE -ne 0) { throw "La compilation a échoué ($LASTEXITCODE)." }
} finally { Pop-Location }
