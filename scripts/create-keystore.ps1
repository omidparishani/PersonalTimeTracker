# Creates a release keystore next to this script's parent folder.
# Run once:  powershell -ExecutionPolicy Bypass -File scripts/create-keystore.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$jks = Join-Path $root "ptt-release.jks"
if (Test-Path $jks) {
    Write-Host "Already exists: $jks"
    exit 0
}

$pass = Read-Host "Keystore / key password"
& keytool -genkeypair -v -storetype JKS -keystore $jks -alias ptt -keyalg RSA -keysize 2048 -validity 10000 `
    -storepass $pass -keypass $pass `
    -dname "CN=Personal Time Tracker, OU=PTT, O=PTT, L=Tehran, ST=Tehran, C=IR"

Write-Host ""
Write-Host "Keystore: $jks"
Write-Host "Copy keystore.properties.example to keystore.properties and fill in the password."
Write-Host "For GitHub Actions, add secrets:"
Write-Host "  KEYSTORE_BASE64   = base64 of ptt-release.jks"
Write-Host "  KEYSTORE_PASSWORD = the password"
Write-Host "  KEY_ALIAS         = ptt"
Write-Host "  KEY_PASSWORD      = the password"
Write-Host ""
Write-Host "Windows base64:"
Write-Host "  [Convert]::ToBase64String([IO.File]::ReadAllBytes('$jks')) | Set-Clipboard"
