param(
    [string]$Jdk = 'C:\Program Files\Android\Android Studio\jbr',
    [string]$Directory = "$env:LOCALAPPDATA\KishiSwitch\signing"
)
$ErrorActionPreference = 'Stop'
$taskDirectory = [IO.Path]::GetFullPath($Directory)
$taskProperties = Join-Path $taskDirectory 'signing.properties'
$taskKey = Join-Path $taskDirectory 'kishi-personal.jks'
if (Test-Path -LiteralPath $taskProperties) { Write-Output $taskProperties; return }
if (Test-Path -LiteralPath $taskKey) { throw 'Clé existante sans ses propriétés : restaurez le fichier signing.properties ; ne remplacez pas la clé.' }
$null = New-Item -ItemType Directory -Path $taskDirectory -Force
# Restrict this private directory to the current Windows account and SYSTEM.
$taskSid = [Security.Principal.WindowsIdentity]::GetCurrent().User.Value
& icacls.exe $taskDirectory /inheritance:r /grant:r "*$($taskSid):(OI)(CI)F" '*S-1-5-18:(OI)(CI)F' | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Impossible de protéger le dossier de signature.' }
$taskRandom = New-Object byte[] 32
$taskRng = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $taskRng.GetBytes($taskRandom) } finally { $taskRng.Dispose() }
$taskPassword = [Convert]::ToBase64String($taskRandom)
$taskPreviousPassword = $env:KISHI_KEY_PASSWORD
try {
    $env:KISHI_KEY_PASSWORD = $taskPassword
    & "$Jdk\bin\keytool.exe" -genkeypair -keystore $taskKey -storetype JKS -alias kishi-personal -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Kishi Switch personal' -storepass:env KISHI_KEY_PASSWORD -keypass:env KISHI_KEY_PASSWORD *> $null
    if ($LASTEXITCODE -ne 0) { throw 'Création de la clé impossible.' }
    $taskText = "storeFile=$($taskKey.Replace('\', '/'))`nstorePassword=$taskPassword`nkeyAlias=kishi-personal`nkeyPassword=$taskPassword`n"
    [IO.File]::WriteAllText($taskProperties, $taskText, [Text.Encoding]::ASCII)
} finally {
    $env:KISHI_KEY_PASSWORD = $taskPreviousPassword
    $taskPassword = $null
}
Write-Output $taskProperties
