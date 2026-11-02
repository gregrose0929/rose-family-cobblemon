$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$MinecraftVersion = '1.21.1'
$LoaderVersion = '0.20.1'
$VersionId = "fabric-loader-$LoaderVersion-$MinecraftVersion"
$ProfileId = 'cobblemon-1.21.1'
$ProfileName = 'Cobblemon 1.21.1'
$ServerAddress = 'mc.greg.download'
$ServersDatBase64 = '@@SERVERS_DAT@@'
$Mods = @'
@@MODS_JSON@@
'@ | ConvertFrom-Json

$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$MinecraftDir = Join-Path $env:APPDATA '.minecraft'
$GameDir = Join-Path $env:APPDATA '.minecraft-cobblemon'
$ModsDir = Join-Path $GameDir 'mods'

function Say($text) { Write-Host $text }

function Download($url, $path) {
    for ($try = 1; $try -le 3; $try++) {
        try {
            Invoke-WebRequest -Uri $url -OutFile $path -UseBasicParsing -TimeoutSec 300
            return
        } catch {
            if ($try -eq 3) { throw }
            Start-Sleep -Seconds 3
        }
    }
}

function Install-Mods {
    New-Item -ItemType Directory -Force -Path $ModsDir | Out-Null
    $wanted = @($Mods | ForEach-Object { $_.file })
    Get-ChildItem -LiteralPath $ModsDir -Filter '*.jar' | Where-Object { $wanted -notcontains $_.Name } | ForEach-Object {
        Say "  Removing old mod: $($_.Name)"
        Remove-Item -LiteralPath $_.FullName -Force
    }
    $number = 0
    foreach ($mod in $Mods) {
        $number++
        $target = Join-Path $ModsDir $mod.file
        if ((Test-Path -LiteralPath $target) -and ((Get-FileHash -LiteralPath $target -Algorithm SHA512).Hash -eq $mod.sha512)) {
            Say "  [$number/$($Mods.Count)] $($mod.name): already installed"
            continue
        }
        Say "  [$number/$($Mods.Count)] $($mod.name): downloading ($($mod.mb) MB)"
        $temp = "$target.download"
        Download $mod.url $temp
        if ((Get-FileHash -LiteralPath $temp -Algorithm SHA512).Hash -ne $mod.sha512) {
            Remove-Item -LiteralPath $temp -Force
            throw "$($mod.name) failed its safety check. Nothing was installed for this mod. Please try again."
        }
        Move-Item -LiteralPath $temp -Destination $target -Force
    }
}

function Install-Fabric {
    $folder = Join-Path (Join-Path $MinecraftDir 'versions') $VersionId
    New-Item -ItemType Directory -Force -Path $folder | Out-Null
    $url = "https://meta.fabricmc.net/v2/versions/loader/$MinecraftVersion/$LoaderVersion/profile/json"
    $target = Join-Path $folder "$VersionId.json"
    Download $url "$target.download"
    Move-Item -LiteralPath "$target.download" -Destination $target -Force
}

function Set-LauncherProfile {
    $file = Join-Path $MinecraftDir 'launcher_profiles.json'
    if (Test-Path -LiteralPath $file) {
        $backup = "$file.before-cobblemon"
        if (-not (Test-Path -LiteralPath $backup)) { Copy-Item -LiteralPath $file -Destination $backup }
        $data = [IO.File]::ReadAllText($file) | ConvertFrom-Json
    } else {
        $data = New-Object PSObject
    }
    if (-not $data.PSObject.Properties['profiles']) {
        $data | Add-Member -NotePropertyName 'profiles' -NotePropertyValue (New-Object PSObject)
    }
    $memoryGb = [Math]::Round((Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory / 1GB)
    $heap = if ($memoryGb -ge 12) { '-Xmx6G -Xms2G' } elseif ($memoryGb -ge 8) { '-Xmx4G -Xms2G' } else { '-Xmx3G -Xms2G' }
    $now = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.000Z")
    $entry = [PSCustomObject]@{
        name = $ProfileName; type = 'custom'; created = $now; lastUsed = $now; icon = 'Grass'
        lastVersionId = $VersionId; gameDir = $GameDir; javaArgs = $heap
    }
    $data.profiles | Add-Member -NotePropertyName $ProfileId -NotePropertyValue $entry -Force
    [IO.File]::WriteAllText($file, ($data | ConvertTo-Json -Depth 32), $Utf8NoBom)
}

function Add-ServerToList {
    $file = Join-Path $GameDir 'servers.dat'
    if (Test-Path -LiteralPath $file) {
        $existing = [Text.Encoding]::ASCII.GetString([IO.File]::ReadAllBytes($file))
        if ($existing.Contains($ServerAddress)) { return }
        Copy-Item -LiteralPath $file -Destination "$file.old" -Force
    }
    [IO.File]::WriteAllBytes($file, [Convert]::FromBase64String($ServersDatBase64))
}

function Test-MinecraftRunning {
    if (Get-Process -Name 'Minecraft', 'MinecraftLauncher' -ErrorAction SilentlyContinue) { return $true }
    $java = Get-CimInstance Win32_Process -Filter "Name='javaw.exe' OR Name='java.exe'" -ErrorAction SilentlyContinue
    return [bool]($java | Where-Object { $_.CommandLine -match '\.minecraft' })
}

function Wait-ForLauncherToClose {
    if (-not (Test-MinecraftRunning)) { return }
    Say 'Please close Minecraft and the Minecraft Launcher.'
    Say 'This will continue by itself once they are closed...'
    while (Test-MinecraftRunning) { Start-Sleep -Seconds 2 }
    Start-Sleep -Seconds 2
}

try {
    Say '================================================'
    Say '  Rose Family Server: Cobblemon installer'
    Say '================================================'
    Say ''
    if (-not (Test-Path -LiteralPath $MinecraftDir)) {
        Say 'Minecraft Java Edition was not found on this computer.'
        Say 'Install it, open it once, close it, and run this installer again.'
        return
    }
    Wait-ForLauncherToClose
    Say ''
    Say 'Step 1 of 3: Downloading mods (a few minutes on the first run)...'
    Install-Mods
    Say ''
    Say 'Step 2 of 3: Setting up Fabric...'
    Install-Fabric
    Say ''
    Say 'Step 3 of 3: Adding the launcher profile and server...'
    Set-LauncherProfile
    Add-ServerToList
    Say ''
    Say '================================================'
    Say '  All done!'
    Say '================================================'
    Say ''
    Say "1. Open the Minecraft Launcher."
    Say "2. Choose the '$ProfileName' profile and press Play."
    Say "3. Go to Multiplayer and join 'Rose Family Server'."
    Say ''
    Say 'Your other Minecraft worlds and mods were not changed.'
} catch {
    Say ''
    Say 'Something went wrong:'
    Say "  $($_.Exception.Message)"
    Say ''
    Say 'Nothing is broken. Check your internet connection and run this again.'
    Say 'If it keeps happening, send a photo of this window to Greg.'
}
