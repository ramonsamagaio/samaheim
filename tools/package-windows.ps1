param(
    [switch]$SkipTests
)

$ErrorActionPreference = 'Stop'
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Push-Location $RepoRoot
try {
    $gradle = Get-Command gradle -ErrorAction Stop
    if ($SkipTests) {
        & $gradle.Source clean installDist --stacktrace
    } else {
        & $gradle.Source clean test installDist --stacktrace
    }
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed with exit code $LASTEXITCODE" }

    $inputDir = Join-Path $RepoRoot 'build\install\Samaheim\lib'
    if (-not (Test-Path $inputDir)) { throw "Distribution library directory not found: $inputDir" }

    $mainJar = Get-ChildItem -Path $inputDir -Filter 'samaheim-*.jar' |
        Where-Object { $_.Name -notmatch '(sources|javadoc)' } |
        Select-Object -First 1
    if ($null -eq $mainJar) { throw 'Could not locate the Samaheim application JAR.' }

    $jpackage = Get-Command jpackage -ErrorAction Stop
    $dest = Join-Path $RepoRoot 'build\jpackage'
    if (Test-Path $dest) { Remove-Item $dest -Recurse -Force }
    New-Item -ItemType Directory -Path $dest | Out-Null

    & $jpackage.Source `
        --type app-image `
        --name Samaheim `
        --dest $dest `
        --input $inputDir `
        --main-jar $mainJar.Name `
        --main-class com.samaheim.SamaheimCaveGame `
        --app-version 0.7.0 `
        --vendor Samaheim `
        --description 'First-person medieval fantasy survival sandbox' `
        --java-options '-Xms256m' `
        --java-options '-Xmx3g'
    if ($LASTEXITCODE -ne 0) { throw "jpackage failed with exit code $LASTEXITCODE" }

    $appDir = Join-Path $dest 'Samaheim'
    $exe = Join-Path $appDir 'Samaheim.exe'
    if (-not (Test-Path $exe)) { throw "Expected executable was not generated: $exe" }

    $zip = Join-Path $RepoRoot 'build\Samaheim-Windows.zip'
    if (Test-Path $zip) { Remove-Item $zip -Force }
    Compress-Archive -Path $appDir -DestinationPath $zip -CompressionLevel Optimal

    Write-Host ''
    Write-Host 'Samaheim Windows build created successfully:' -ForegroundColor Green
    Write-Host "  EXE: $exe"
    Write-Host "  ZIP: $zip"
    Write-Host 'Unzip the archive and double-click Samaheim\Samaheim.exe. Java and Gradle are not required on the target PC.'
}
finally {
    Pop-Location
}
