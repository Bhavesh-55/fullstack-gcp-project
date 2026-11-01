param([Parameter(ValueFromRemainingArguments = $true)][string[]]$GradleArgs)
$ErrorActionPreference = 'Stop'
$version = '8.14.6'
$project = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$cache = Join-Path $project '.gradle-bootstrap'
$gradle = Join-Path $cache "gradle-$version/bin/gradle.bat"
$archive = Join-Path $cache "gradle-$version-bin.zip"
$expectedSha256 = '7988ed071b2a07900e2ec715fca15c6ab72bce6db433af301fa7fa7e9407bd24'
if (-not (Test-Path $gradle)) {
    New-Item -ItemType Directory -Force -Path $cache | Out-Null
    Write-Host "Downloading Gradle $version (first run only)..."
    Invoke-WebRequest "https://services.gradle.org/distributions/gradle-$version-bin.zip" -OutFile $archive
    $actual = (Get-FileHash -Algorithm SHA256 $archive).Hash.ToLowerInvariant()
    if ($actual -ne $expectedSha256) { Remove-Item $archive; throw 'Gradle archive checksum mismatch' }
    Expand-Archive -Path $archive -DestinationPath $cache -Force
    Remove-Item $archive
}
& $gradle @GradleArgs
exit $LASTEXITCODE
