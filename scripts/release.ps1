# Human-operated release entry point. Agents must only use --dry-run.
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false

function Invoke-ReleaseGit {
    param([string[]]$GitArguments, [switch]$AllowFailure)
    $output = @(& git @GitArguments)
    $code = $LASTEXITCODE
    if ($code -ne 0 -and !$AllowFailure) { throw "git $($GitArguments -join ' ') failed (exit $code)" }
    [pscustomobject]@{ Output = $output; ExitCode = $code }
}

try {
    $spec = 'patch'
    $specified = $false
    $message = ''
    $dryRun = $false
    $yes = $false
    $push = $false
    for ($i = 0; $i -lt $args.Count; $i++) {
        $argument = [string]$args[$i]
        switch -CaseSensitive ($argument) {
            { $_ -in '-h', '--help' } {
                Write-Output 'Usage: .\scripts\release.ps1 [patch|minor|major|X.Y.Z] [-n|--dry-run] [-y|--yes] [--push] [-m|--message TEXT]'
                Write-Output 'Bump version.properties, commit only that file, create annotated vX.Y.Z, optionally push branch then tag.'
                exit 0
            }
            { $_ -in '-n', '--dry-run' } { $dryRun = $true; break }
            { $_ -in '-y', '--yes' } { $yes = $true; break }
            '--push' { $push = $true; break }
            { $_ -in '-m', '--message' } {
                $i++
                if ($i -ge $args.Count) { throw "$argument requires text" }
                $message = [string]$args[$i]
                break
            }
            default {
                if ($argument.StartsWith('-') -or $specified) { throw "Unexpected argument: $argument" }
                $spec = $argument
                $specified = $true
            }
        }
    }
    Push-Location -LiteralPath (Join-Path $PSScriptRoot '..')
    try {
        $null = Invoke-ReleaseGit @('rev-parse', '--verify', 'HEAD')
        $branch = Invoke-ReleaseGit @('symbolic-ref', '--short', 'HEAD')
        $shallow = Invoke-ReleaseGit @('rev-parse', '--is-shallow-repository')
        if (($shallow.Output -join '') -cne 'false') { throw 'Fetch full history and tags before releasing.' }
        $status = Invoke-ReleaseGit @('status', '--porcelain')
        if ($status.Output.Count) {
            if (!$dryRun) { throw 'Working tree is dirty; review and commit changes before releasing.' }
            Write-Warning 'Working tree is dirty; a real release would be refused.'
        }
        $current = & "$PSScriptRoot\version.ps1"
        $next = & "$PSScriptRoot\version.ps1" $spec
        if ($next.VersionCode -le $current.VersionCode) { throw 'A release requires a version bump.' }
        $existing = Invoke-ReleaseGit @('show-ref', '--verify', '--quiet', "refs/tags/$($next.Tag)") -AllowFailure
        if ($existing.ExitCode -eq 0) { throw "Tag $($next.Tag) already exists." }
        if ($existing.ExitCode -ne 1) { throw 'Could not check existing tag.' }
        # Reject a versionCode already superseded by any fetched release.
        $tags = Invoke-ReleaseGit @('tag', '--list', 'v*')
        foreach ($tag in $tags.Output) {
            if ($tag -cnotmatch '\Av(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\z') { continue }
            $old = Invoke-ReleaseGit @('show', ('{0}:version.properties' -f $tag))
            $oldCode = [regex]::Match(($old.Output -join [Environment]::NewLine), '(?m)^versionCode=([1-9][0-9]*)\r?$')
            if (!$oldCode.Success -or [bigint]$oldCode.Groups[1].Value -ge $next.VersionCode) {
                throw "versionCode must exceed the code at $tag. Release from the latest version."
            }
        }
        if ($push) { $null = Invoke-ReleaseGit @('remote', 'get-url', 'origin') }
        if (!$message) { $message = "Release $($next.Tag)" }
        Write-Output "Version: $($current.VersionName) ($($current.VersionCode)) -> $($next.VersionName) ($($next.VersionCode))"
        Write-Output "Commit: Release $($next.Tag); annotated tag: $($next.Tag); push: $push"
        if ($dryRun) { Write-Output 'Dry run: no files, refs or remotes changed.'; exit 0 }
        if (!$yes -and (Read-Host 'Update version, commit and tag this release? [y/N]') -cnotmatch '\A[Yy]\z') {
            throw 'Aborted.'
        }
        $null = & "$PSScriptRoot\version.ps1" $spec -Write
        $null = Invoke-ReleaseGit @('add', '--', 'version.properties')
        $null = Invoke-ReleaseGit @('commit', '--only', '-m', "Release $($next.Tag)", '--', 'version.properties')
        $committed = Invoke-ReleaseGit @('show', 'HEAD:version.properties')
        $committedText = $committed.Output -join [Environment]::NewLine
        if ($committedText -cnotmatch "(?m)^versionName=$([regex]::Escape($next.VersionName))\r?$" -or
            $committedText -cnotmatch "(?m)^versionCode=$($next.VersionCode)\r?$") {
            throw 'Committed version does not match the planned release; no tag created.'
        }
        $null = Invoke-ReleaseGit @('tag', '-a', $next.Tag, '-m', $message)
        if ($push) {
            $null = Invoke-ReleaseGit @('push', 'origin', ($branch.Output -join ''))
            $null = Invoke-ReleaseGit @('push', 'origin', "refs/tags/$($next.Tag):refs/tags/$($next.Tag)")
        } else {
            Write-Output "Push when ready: git push origin $($branch.Output -join '')"
            Write-Output "Then: git push origin refs/tags/$($next.Tag):refs/tags/$($next.Tag)"
        }
    } finally { Pop-Location }
} catch {
    [Console]::Error.WriteLine("error: $($_.Exception.Message)")
    exit 1
}
exit 0