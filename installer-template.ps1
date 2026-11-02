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

function Add-MapWaypoints {
    # name, initials, x, z, color, dimension folder
    $places = @(
        @('Cerulean City', 'C', 624, 128, 9, 'dim%0'),
        @('Vermilion City', 'V', 592, -464, 9, 'dim%0'),
        @('Viridian City', 'V', -752, -816, 9, 'dim%0'),
        @('Saffron City', 'S', -336, -1296, 9, 'dim%0'),
        @('Cinnabar Island', 'C', 640, 1520, 9, 'dim%0'),
        @('Fuchsia City', 'F', 96, -1888, 9, 'dim%0'),
        @('Pewter City', 'P', 1968, -2768, 9, 'dim%0'),
        @('Celadon City', 'C', -2272, -3200, 9, 'dim%0'),
        @('Cerulean Gym', 'G', 752, 208, 14, 'dim%0'),
        @('Vermilion Gym', 'G', 160, -464, 14, 'dim%0'),
        @('Celadon Gym', 'G', -880, 80, 14, 'dim%0'),
        @('Fuchsia Gym', 'G', -816, 848, 14, 'dim%0'),
        @('Saffron Gym', 'G', -1392, -320, 14, 'dim%0'),
        @('Pewter Gym', 'G', 1344, -2208, 14, 'dim%0'),
        @('Cinnabar Gym', 'G', -272, -160, 14, 'dim%-1'),
        @('Blackthorn Gym', 'G', 1200, -480, 14, 'dim%1'),
        @('Kanto League', 'L', -416, 1904, 12, 'dim%1')
    )
    $root = Join-Path (Join-Path $GameDir 'XaeroWaypoints') "Multiplayer_$ServerAddress"
    foreach ($group in ($places | Group-Object { $_[5] })) {
        $folder = Join-Path $root $group.Name
        New-Item -ItemType Directory -Force -Path $folder | Out-Null
        $file = Join-Path $folder 'mw$default_1.txt'
        $text = if (Test-Path -LiteralPath $file) { [IO.File]::ReadAllText($file) } else {
            "#`n#waypoint:name:initials:x:y:z:color:disabled:type:set:rotate_on_tp:tp_yaw:visibility_type:destination`n#`nsets:gui.xaero_default`n#`n"
        }
        foreach ($p in $group.Group) {
            if (-not $text.Contains("waypoint:$($p[0]):")) {
                $text += "waypoint:$($p[0]):$($p[1]):$($p[2]):~:$($p[3]):$($p[4]):false:0:gui.xaero_default:false:0:0:false`n"
            }
        }
        [IO.File]::WriteAllText($file, $text, $Utf8NoBom)
    }
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
    Add-MapWaypoints
    Say ''
    Say '================================================'
    Say '  All done!'
    Say '================================================'
    Say ''
    Say "1. Open the Minecraft Launcher."
    Say "2. Choose the '$ProfileName' profile and press Play."
    Say "3. Go to Multiplayer and join 'Rose Family Server'."
    Say "4. Press M in the game to open the map. Towns and gyms are marked on it."
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
