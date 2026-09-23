# Fixed local operations for the Itera checkout; no arbitrary shell/script execution.
[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidateSet('Read','Search','Files','Results','StartEmulator','DeviceTest','BuildLint','Coverage','WriteFiles')][string]$Action,
    [string[]]$Paths = @(),
    [string]$Pattern,
    [ValidateRange(0,1000000)][int]$Skip = 0,
    [ValidateRange(1,10000)][int]$First = 200,
    [ValidatePattern('^com\.(wivernz\.itera|itera\.app)\.[A-Za-z0-9_.$]+(#[A-Za-z0-9_]+)?$')][string]$TestClass,
    [string]$ChangesBase64,
    [switch]$DryRun
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$repo = 'D:\Projects\Itera'
$utf8 = [Text.UTF8Encoding]::new($false, $true)
if ((Split-Path $PSScriptRoot -Parent) -ne $repo) { throw 'Wrong checkout.' }
function Resolve-ProjectPath([string]$Relative) {
    if ([string]::IsNullOrWhiteSpace($Relative) -or [IO.Path]::IsPathRooted($Relative) -or $Relative.Contains(':')) { throw 'A repository-relative path is required.' }
    if ($Relative -match '(^|[\\/])\.\.?([\\/]|$)') { throw 'Relative traversal is not allowed.' }
    $full = [IO.Path]::GetFullPath((Join-Path $repo $Relative))
    if (-not $full.StartsWith($repo + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Path escapes checkout.' }
    $current = $full
    while ($current -and $current -ne $repo) {
        if (Test-Path -LiteralPath $current) {
            if ((Get-Item -Force -LiteralPath $current).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Reparse points are not allowed.' }
        }
        $current = Split-Path $current -Parent
    }
    return $full
}
function Invoke-ProjectGradle([string[]]$Tasks) {
    if ($DryRun) { Write-Output ('Gradle: ' + ($Tasks -join ' ')); return }
    Push-Location $repo
    try {
        & 'D:\Projects\Itera\gradlew.bat' @Tasks --console=plain
        if ($LASTEXITCODE -ne 0) { throw "Gradle failed: $LASTEXITCODE" }
    } finally { Pop-Location }
}
switch ($Action) {
    'Read' {
        if (-not $Paths.Count) { throw 'Supply -Paths.' }
        foreach ($relative in $Paths) {
            $full = Resolve-ProjectPath $relative
            Write-Output "${relative}:"
            Write-Output "SHA256: $((Get-FileHash -LiteralPath $full -Algorithm SHA256).Hash)"
            Get-Content -LiteralPath $full -Encoding utf8 | Select-Object -Skip $Skip -First $First
        }
    }
    'Search' {
        if (-not $Paths.Count -or [string]::IsNullOrEmpty($Pattern)) { throw 'Supply -Paths and -Pattern.' }
        $targets = @($Paths | ForEach-Object { Resolve-ProjectPath $_ })
        & rg --no-config --line-number -- $Pattern @targets
        if ($LASTEXITCODE -gt 1) { throw "rg failed: $LASTEXITCODE" }
    }
    'Files' {
        if (-not $Paths.Count) { throw 'Supply -Paths.' }
        $targets = @($Paths | ForEach-Object { Resolve-ProjectPath $_ })
        & rg --no-config --files -- @targets
        if ($LASTEXITCODE -gt 1) { throw "rg failed: $LASTEXITCODE" }
    }
    'Results' {
        $tests = 0; $failures = 0; $errors = 0; $suites = 0
        $results = Join-Path $repo 'app\build\test-results\testDebugUnitTest'
        if (Test-Path -LiteralPath $results) {
            foreach ($file in Get-ChildItem -LiteralPath $results -Filter 'TEST-*.xml') {
                [xml]$xml = [IO.File]::ReadAllText($file.FullName, $utf8)
                $tests += [int]$xml.testsuite.tests
                $failures += [int]$xml.testsuite.failures
                $errors += [int]$xml.testsuite.errors
                $suites++
            }
            Write-Output "Host tests: $tests; failures: $failures; errors: $errors; suites: $suites"
        } else { Write-Output 'No host test results available.' }
        $lint = Join-Path $repo 'app\build\reports\lint-results-debug.xml'
        if (Test-Path -LiteralPath $lint) {
            [xml]$xml = [IO.File]::ReadAllText($lint, $utf8)
            Write-Output "Lint issues: $($xml.SelectNodes('/issues/issue').Count)"
        }
        $schemas = Join-Path $repo 'app\schemas'
        foreach ($file in Get-ChildItem -LiteralPath $schemas -Recurse -Filter '*.json') {
            $schema = [IO.File]::ReadAllText($file.FullName, $utf8) | ConvertFrom-Json
            Write-Output "Schema $($file.Name): $($schema.database.entities.Count) tables"
        }
        Write-Output 'These are existing reports, not a new verification run.'
    }
    'StartEmulator' {
        if ($DryRun) { Write-Output 'Start Medium_Phone_API_36.1 headlessly if no emulator is connected.'; break }
        $devices = & 'G:\Android\SDK\platform-tools\adb.exe' devices
        if ($LASTEXITCODE -ne 0) { throw 'ADB discovery failed.' }
        if ($devices -match '^emulator-\d+\s') { Write-Output 'An emulator is already connected.'; break }
        Start-Process -FilePath 'G:\Android\SDK\emulator\emulator.exe' -ArgumentList '-avd','Medium_Phone_API_36.1','-no-window','-no-audio' -WindowStyle Hidden
    }
    'DeviceTest' {
        if (-not $TestClass) { throw 'Supply a single -TestClass; this helper does not launch a full device audit.' }
        Invoke-ProjectGradle @('connectedDebugAndroidTest', "-Pandroid.testInstrumentationRunnerArguments.class=$TestClass")
    }
    'BuildLint' { Invoke-ProjectGradle @('build','lintDebug') }
    'Coverage' { Invoke-ProjectGradle @('koverXmlReport') }
    'WriteFiles' {
        if (-not $ChangesBase64) { throw 'Supply -ChangesBase64 containing a UTF-8 JSON array.' }
        $changes = @($utf8.GetString([Convert]::FromBase64String($ChangesBase64)) | ConvertFrom-Json)
        if (-not $changes.Count) { throw 'No changes supplied.' }
        $pending = @(); $seen = [Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
        foreach ($change in $changes) {
            if ($change.path -isnot [string] -or $change.content -isnot [string] -or $change.expectedSha256 -isnot [string]) { throw 'Each change needs path, content, and expectedSha256 strings.' }
            $full = Resolve-ProjectPath $change.path
            $relative = [IO.Path]::GetRelativePath($repo, $full).Replace('\','/')
            if ($relative -notmatch '^(app/(src|schemas)/|docs/|design/app/src/)') { throw 'Write target must be app/src, app/schemas, docs, or design/app/src.' }
            if ([IO.Path]::GetExtension($full) -notin @('.kt','.java','.xml','.json','.md','.txt','.properties')) { throw 'Only source/document text extensions are writable.' }
            if ($relative -match '(^|/)(AGENTS(?:\.local)?\.md|CLAUDE\.md|SKILL\.md|\.[^/]+)(/|$)') { throw 'Instruction and hidden paths are not writable through this helper.' }
            if (-not $seen.Add($full)) { throw 'Duplicate write target.' }
            if (Test-Path -LiteralPath $full) {
                if ($change.expectedSha256 -notmatch '^[a-fA-F0-9]{64}$') { throw 'Existing files need their current SHA-256.' }
                if ((Get-FileHash -LiteralPath $full -Algorithm SHA256).Hash -ne $change.expectedSha256) { throw 'File changed since read; refusing stale write.' }
            } elseif ($change.expectedSha256 -ne 'new') { throw 'New files need expectedSha256=new.' }
            $pending += [pscustomobject]@{ Path = $full; Content = $change.content.Replace("`r`n", "`n"); Expected = $change.expectedSha256 }
        }
        foreach ($change in $pending) {
            Write-Output "Write: $($change.Path)"
            if ($DryRun) { continue }
            if (Test-Path -LiteralPath $change.Path) {
                if ($change.Expected -eq 'new' -or (Get-FileHash -LiteralPath $change.Path -Algorithm SHA256).Hash -ne $change.Expected) { throw 'File changed before write.' }
            } elseif ($change.Expected -ne 'new') { throw 'File disappeared before write.' }
            $null = New-Item -ItemType Directory -Force -Path (Split-Path $change.Path -Parent)
            [IO.File]::WriteAllText($change.Path, $change.Content, $utf8)
        }
    }
}
