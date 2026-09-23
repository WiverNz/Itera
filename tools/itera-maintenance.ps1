[CmdletBinding()]
param(
    [ValidateSet('CI','CleanBuild','BuildTools','FormatCheck','Format','Dependencies','FreshBuild','NormalizeLf')]
    [string]$Action = 'CI',
    [switch]$DryRun
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repo = 'D:\Projects\Itera'
if ((Split-Path $PSScriptRoot -Parent) -ne $repo) { throw 'This helper is specific to the Itera checkout.' }
$taskSets = @{
    CI = @('--no-daemon','spotlessCheck','lintDebug','testDebugUnitTest','verifyRoborazziDebug','koverVerify','assembleDebug')
    CleanBuild = @('clean','build')
    BuildTools = @('-p','buildSrc','--no-daemon','test')
    FormatCheck = @('spotlessCheck')
    Format = @('spotlessApply')
    Dependencies = @(':app:dependencies')
}
function Invoke-Gradle([string]$Directory, [string[]]$Tasks) {
    Write-Host "Gradle in ${Directory}: $($Tasks -join ' ')"
    if ($DryRun) { return }
    Push-Location $Directory
    try {
        & (Join-Path $Directory 'gradlew.bat') @Tasks --console=plain
        if ($LASTEXITCODE -ne 0) { throw "Gradle failed: $LASTEXITCODE" }
    } finally { Pop-Location }
}
$oldSdk = $env:ANDROID_HOME
try {
    $env:ANDROID_HOME = 'G:\Android\SDK'
    if ($Action -eq 'CI') { Invoke-Gradle $repo $taskSets.BuildTools }
    if ($taskSets.ContainsKey($Action)) {
        Invoke-Gradle $repo $taskSets[$Action]
    } elseif ($Action -eq 'FreshBuild') {
        if ($DryRun) { Write-Host 'Copy Git-listed source to a new temporary directory; run clean build. No deletion.'; exit 0 }
        $copyRoot = Join-Path ([IO.Path]::GetTempPath()) ('itera-fresh-' + [guid]::NewGuid().ToString('N'))
        $null = New-Item -ItemType Directory -Path $copyRoot
        $names = & git -C $repo -c core.quotepath=false ls-files --cached --others --exclude-standard
        if ($LASTEXITCODE -ne 0) { throw 'Cannot list source files.' }
        foreach ($name in $names) {
            $source = [IO.Path]::GetFullPath((Join-Path $repo $name))
            $target = [IO.Path]::GetFullPath((Join-Path $copyRoot $name))
            if (-not $source.StartsWith($repo + '\', [StringComparison]::OrdinalIgnoreCase) -or
                -not $target.StartsWith($copyRoot + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe source path.' }
            if (Test-Path -LiteralPath $source -PathType Leaf) {
                $null = New-Item -ItemType Directory -Path (Split-Path $target -Parent) -Force
                Copy-Item -LiteralPath $source -Destination $target
            }
        }
        [IO.File]::WriteAllText((Join-Path $copyRoot 'local.properties'), "sdk.dir=G\:/Android/SDK`n", [Text.UTF8Encoding]::new($false))
        Write-Host "Fresh source copy retained at $copyRoot"
        Invoke-Gradle $copyRoot @('clean','build')
    } else {
        $names = @(& git -C $repo -c core.quotepath=false diff HEAD --name-only)
        if ($LASTEXITCODE -ne 0) { throw 'Cannot list modified files.' }
        $names += @(& git -C $repo -c core.quotepath=false ls-files --others --exclude-standard)
        if ($LASTEXITCODE -ne 0) { throw 'Cannot list new files.' }
        $utf8 = [Text.UTF8Encoding]::new($false, $true)
        foreach ($name in ($names | Sort-Object -Unique)) {
            $file = [IO.Path]::GetFullPath((Join-Path $repo $name))
            if (-not $file.StartsWith($repo + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe file path.' }
            if (-not (Test-Path -LiteralPath $file -PathType Leaf)) { continue }
            if ((Get-Item -LiteralPath $file).Attributes -band [IO.FileAttributes]::ReparsePoint) { continue }
            if ([IO.Path]::GetExtension($file) -notin @('.xml','.kt','.kts','.java','.md','.txt','.toml','.yml','.yaml','.properties','.json','.rules','.ps1','.py','')) { continue }
            $attribute = & git -C $repo check-attr eol -- $name
            if ($LASTEXITCODE -ne 0) { throw 'Cannot read line-ending attribute.' }
            if ($attribute -notmatch ': eol: lf$') { continue }
            $bytes = [IO.File]::ReadAllBytes($file)
            if ($bytes -contains 0) { continue }
            $content = $utf8.GetString($bytes)
            if ($content.Contains("`r`n")) {
                Write-Host "Normalize LF: $name"
                if (-not $DryRun) { [IO.File]::WriteAllBytes($file, $utf8.GetBytes($content.Replace("`r`n", "`n"))) }
            }
        }
    }
} catch {
    Write-Error $_ -ErrorAction Continue
    exit 1
} finally { $env:ANDROID_HOME = $oldSdk }
