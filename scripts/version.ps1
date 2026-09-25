# Windows PowerShell 5.1+: read, preview, or update the production Android version.
[CmdletBinding()]
param(
    [Parameter(Position = 0)][string]$Version = 'current',
    [switch]$Write,
    [string]$Tag
)
$ErrorActionPreference = 'Stop'
$path = Join-Path $PSScriptRoot '..\version.properties'
$text = [IO.File]::ReadAllText($path)
$names = [regex]::Matches($text, '(?m)^versionName=([^\r\n]+)\r?$')
$codes = [regex]::Matches($text, '(?m)^versionCode=([^\r\n]+)\r?$')
if ($names.Count -ne 1 -or $codes.Count -ne 1) { throw 'Expected one versionName and one versionCode.' }
$current = $names[0].Groups[1].Value
$codeText = $codes[0].Groups[1].Value
$semver = '\A(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\z'
if ($current -cnotmatch $semver) { throw 'versionName must be stable SemVer X.Y.Z.' }
if ($codeText -cnotmatch '\A[1-9][0-9]{0,9}\z' -or [long]$codeText -gt 2100000000) {
    throw 'versionCode must be in 1..2100000000.'
}
$code = [long]$codeText
$new = $current
if ($Version -cne 'current') {
    $parts = @($current.Split('.') | ForEach-Object { [bigint]::Parse($_) })
    switch -CaseSensitive ($Version) {
        'major' { $new = '{0}.0.0' -f ($parts[0] + 1) }
        'minor' { $new = '{0}.{1}.0' -f $parts[0], ($parts[1] + 1) }
        'patch' { $new = '{0}.{1}.{2}' -f $parts[0], $parts[1], ($parts[2] + 1) }
        default { $new = $Version }
    }
    if ($new -cnotmatch $semver) { throw 'Expected patch, minor, major, or stable SemVer X.Y.Z.' }
    $next = @($new.Split('.') | ForEach-Object { [bigint]::Parse($_) })
    $greater = $false
    for ($i = 0; $i -lt 3; $i++) {
        if ($next[$i] -ne $parts[$i]) { $greater = $next[$i] -gt $parts[$i]; break }
    }
    if (!$greater) { throw "Version must be greater than $current." }
    if ($code -eq 2100000000) { throw 'versionCode has reached the Android limit.' }
    $code++
}
if ($Tag -and ($Version -cne 'current' -or $Tag -cne "v$current")) {
    throw "Release tag must equal v$current from committed version.properties."
}
if ($Write) {
    if ($Version -ceq 'current') { throw 'Supply a version bump with -Write.' }
    $updated = [regex]::Replace($text, '(?m)^versionName=[^\r\n]+', "versionName=$new")
    $updated = [regex]::Replace($updated, '(?m)^versionCode=[^\r\n]+', "versionCode=$code")
    [IO.File]::WriteAllText($path, $updated, [Text.UTF8Encoding]::new($false))
}
[pscustomobject]@{ VersionName = $new; VersionCode = $code; Tag = "v$new" }