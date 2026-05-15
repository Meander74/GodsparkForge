$ErrorActionPreference = "Stop"

$InstanceDir = "C:\Users\Suttawat\AppData\Roaming\PrismLauncher\instances\Create'a Colony"
$ModsDir = Join-Path $InstanceDir "minecraft\mods"

if (!(Test-Path $ModsDir)) {
    throw "Mods directory not found: $ModsDir"
}

$Jar = Get-ChildItem ".\build\libs" -Filter "*.jar" |
    Where-Object { $_.Name -notlike "*sources*" -and $_.Name -notlike "*javadoc*" } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if ($null -eq $Jar) {
    throw "No built jar found in build\libs. Run '.\gradlew.bat build' first."
}

Get-ChildItem $ModsDir -Filter "godspark*.jar" | Remove-Item -Force

Copy-Item $Jar.FullName $ModsDir -Force

Write-Host "Deployed $($Jar.Name) to $ModsDir"
