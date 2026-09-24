# Validate approvals only; never execute the commands under test.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$policyFiles = @(
    'C:\Users\User\.codex\rules\itera-local.rules',
    'C:\Users\User\.codex\rules\itera-reusable.rules',
    'D:\Projects\Itera\.codex\rules\no-git-commit.rules'
)
$policyArgs = @('execpolicy','check')
foreach ($file in $policyFiles) { $policyArgs += @('--rules', $file) }
$bridge = '/mnt/c/Program Files/PowerShell/7/pwsh.exe'
$session = 'D:\Projects\Itera\tools\itera-session.ps1'
$passed = 0
function Assert-Decision([string]$Label, [string[]]$Command, [string]$Expected) {
    $json = & codex @policyArgs -- @Command
    if ($LASTEXITCODE) { throw "Policy parser failed: $Label" }
    $result = $json | ConvertFrom-Json
    $actual = if ($result.PSObject.Properties.Name -contains 'decision') { $result.decision } else { 'none' }
    if ($actual -ne $Expected) { throw "$Label expected $Expected, got $actual" }
    $script:passed++
    Write-Output "$Label : $actual"
}
foreach ($exe in @('pwsh','C:\Program Files\PowerShell\7\pwsh.exe',$bridge)) {
    foreach ($payload in @('W10=', 'W3sicGF0aCI6ImRpZmZlcmVudC5rdCJ9XQ==')) {
        Assert-Decision 'Variable write payload' @($exe,'-NoProfile','-File',$session,'-Action','WriteFiles','-ChangesBase64',$payload) 'allow'
    }
    Assert-Decision 'Variable read path' @($exe,'-NoProfile','-File',$session,'-Action','Read','-Paths','app/src/test/java/com/wivernz/itera/core/navigation/NavigationTest.kt','-First','10000') 'allow'
    Assert-Decision 'Gallery options' @($exe,'-NoProfile','-File','D:\Projects\Itera\tools\itera-gallery.ps1','-Pages','TimeRow','-Dark','true') 'allow'
}
Assert-Decision 'Combined build' @('D:\Projects\Itera\gradlew.bat','--no-daemon','spotlessApply','build','lintDebug','spotlessCheck','--console=plain') 'allow'
Assert-Decision 'Filtered test' @('D:\Projects\Itera\gradlew.bat','--no-daemon','spotlessApply','testDebugUnitTest','--tests','com.wivernz.itera.core.designsystem.ComponentTest','--console=plain') 'allow'
Assert-Decision 'Multiple file reads' @('Get-Content','docs/history/issues-detailed/004-design-system-components.md,','docs/delivery/02-definition-of-done.md') 'allow'
Assert-Decision 'ADB alternate port' @('G:\Android\SDK\platform-tools\adb.exe','-P','5038','connect','127.0.0.1:5555') 'allow'
Assert-Decision 'Arbitrary PowerShell stays unallowed' @($bridge,'-NoProfile','-Command','Write-Output arbitrary') 'none'
Assert-Decision 'Unknown script stays unallowed' @($bridge,'-NoProfile','-File','D:\Projects\Itera\tools\unknown.ps1') 'none'
Assert-Decision 'Git commit remains forbidden' @('git','commit','-m','probe') 'forbidden'
Assert-Decision 'Git push remains forbidden' @('git','push') 'forbidden'
Write-Output "$passed approval checks passed."

$reader = 'D:\Projects\Itera\tools\itera-read.ps1'
foreach ($exe in @('pwsh','C:\Program Files\PowerShell\7\pwsh.exe',$bridge)) {
    Assert-Decision 'Any literal file via read helper' @($exe,'-NoProfile','-File',$reader,'-PathsJson','["C:/arbitrary/path with (parentheses).txt","D:/another.txt"]') 'allow'
}
Assert-Decision 'Opaque Get-Content wrapper explains mismatch' @($bridge,'-NoProfile','-Command',"Get-Content 'file with (parentheses).xml', another.kt") 'none'
Write-Output "$passed approval checks passed including read helper."
