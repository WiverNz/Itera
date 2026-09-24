# Read-only Get-Content entry point. Paths are data, never PowerShell code.
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$PathsJson,
    [ValidateRange(0,2147483647)][int]$First = 0,
    [ValidateRange(0,2147483647)][int]$Tail = 0,
    [switch]$Raw
)
$ErrorActionPreference = 'Stop'
if (($First -gt 0 -and $Tail -gt 0) -or ($Raw -and ($First -gt 0 -or $Tail -gt 0))) {
    throw 'Choose only one of First, Tail or Raw.'
}
$paths = ConvertFrom-Json -InputObject $PathsJson -NoEnumerate
if ($paths -isnot [array] -or $paths.Count -eq 0) { throw 'PathsJson must be a nonempty JSON array.' }
foreach ($path in $paths) {
    if ($path -isnot [string] -or [string]::IsNullOrWhiteSpace($path)) { throw 'Each path must be a nonempty string.' }
    $item = Get-Item -LiteralPath $path -Force
    if ($item.PSProvider.Name -ne 'FileSystem' -or $item.PSIsContainer) { throw 'Only filesystem files can be read.' }
    $options = @{ LiteralPath = $item.FullName }
    if ($First -gt 0) { $options.TotalCount = $First }
    if ($Tail -gt 0) { $options.Tail = $Tail }
    if ($Raw) { $options.Raw = $true }
    Microsoft.PowerShell.Management\Get-Content @options
}
