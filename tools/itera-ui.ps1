# Parameterized emulator checks for Itera. No arbitrary shell command or user-data deletion.
[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidateSet('Tap','Swipe','Back','Theme','Dump','Capture','Size','Launch','Tests003')][string]$Action,
    [ValidateSet('Production','Prototype')][string]$App = 'Production',
    [ValidateRange(0,10000)][int]$X = 0,
    [ValidateRange(0,10000)][int]$Y = 0,
    [ValidateRange(0,10000)][int]$ToX = 0,
    [ValidateRange(0,10000)][int]$ToY = 0,
    [ValidateSet('yes','no','auto')][string]$Night = 'auto',
    [ValidatePattern('^[A-Za-z0-9_-]{1,64}$')][string]$Name = 'ui-check',
    [switch]$DryRun
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repo = 'D:\Projects\Itera'
if ((Split-Path $PSScriptRoot -Parent) -ne $repo) { throw 'Wrong checkout.' }
function Invoke-Adb([string[]]$Arguments) {
    if ($DryRun) { Write-Output ('adb -e ' + ($Arguments -join ' ')); return }
    & 'G:\Android\SDK\platform-tools\adb.exe' -e @Arguments
    if ($LASTEXITCODE -ne 0) { throw "ADB failed: $LASTEXITCODE" }
}
switch ($Action) {
    'Tap' { Invoke-Adb @('shell','input','tap',"$X","$Y") }
    'Swipe' { Invoke-Adb @('shell','input','swipe',"$X","$Y","$ToX","$ToY",'500') }
    'Back' { Invoke-Adb @('shell','input','keyevent','4') }
    'Theme' { Invoke-Adb @('shell','cmd','uimode','night',$Night) }
    'Size' { Invoke-Adb @('shell','wm','size') }
    'Launch' {
        $package = if ($App -eq 'Production') { 'com.wivernz.itera' } else { 'com.itera.app' }
        Invoke-Adb @('shell','am','start','-W','-n',"$package/.MainActivity")
    }
    'Dump' {
        Invoke-Adb @('shell','uiautomator','dump','/sdcard/itera-ui.xml')
        Invoke-Adb @('shell','cat','/sdcard/itera-ui.xml')
    }
    'Capture' {
        $destination = Join-Path $repo "captures\$Name.png"
        Invoke-Adb @('shell','screencap','-p','/sdcard/itera-ui.png')
        Invoke-Adb @('pull','/sdcard/itera-ui.png',$destination)
    }
    'Tests003' {
        if ($DryRun) { Write-Output 'Run design-system and navigation host tests only.'; break }
        Push-Location $repo
        try {
            & 'D:\Projects\Itera\gradlew.bat' testDebugUnitTest --tests 'com.wivernz.itera.core.designsystem.*' --tests 'com.wivernz.itera.core.navigation.*' --console=plain
            if ($LASTEXITCODE -ne 0) { throw "Gradle failed: $LASTEXITCODE" }
        } finally { Pop-Location }
    }
}
