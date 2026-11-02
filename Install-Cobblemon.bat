@echo off
title Rose Family Server - Cobblemon installer
powershell -NoProfile -ExecutionPolicy Bypass -Command "$s=[IO.File]::ReadAllText('%~f0'); Invoke-Expression $s.Substring($s.LastIndexOf('#PS-START#')+10)"
echo.
pause
exit /b
#PS-START#
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$MinecraftVersion = '1.21.1'
$LoaderVersion = '0.20.1'
$VersionId = "fabric-loader-$LoaderVersion-$MinecraftVersion"
$ProfileId = 'cobblemon-1.21.1'
$ProfileName = 'Cobblemon 1.21.1'
$ServerAddress = 'mc.greg.download'
$ServersDatBase64 = 'CgAACQAHc2VydmVycwoAAAABCAAEbmFtZQASUm9zZSBGYW1pbHkgU2VydmVyCAACaXAAEG1jLmdyZWcuZG93bmxvYWQAAA=='
$Mods = @'
[
 {
  "name": "Accessories",
  "version": "1.1.0-beta.54+1.21.1",
  "file": "accessories-fabric-1.1.0-beta.54+1.21.1.jar",
  "url": "https://cdn.modrinth.com/data/jtmvUHXj/versions/YZ9RWTNl/accessories-fabric-1.1.0-beta.54%2B1.21.1.jar",
  "sha512": "4B83F2F00DB833AC7DC7CABE2F74C9DF7067C35170B9E7AEB794A85038F7B34BC4D40CE92E16E142358C5CF4B6F3CA3866155592C4DC80BEB818F6E28BFA9799",
  "mb": 1.1,
  "license": "MIT",
  "page": "https://modrinth.com/mod/accessories"
 },
 {
  "name": "Architectury API",
  "version": "13.0.11+fabric",
  "file": "architectury-13.0.11-fabric.jar",
  "url": "https://cdn.modrinth.com/data/lhGA9TYQ/versions/Pzc2FP5K/architectury-13.0.11-fabric.jar",
  "sha512": "53314EB34890B11AD3C06C6F3BA2D01AE1CA756235CE4F0574E4B0544952A2AB3C628C81B521653D6487D62C5DC3055E06FED6367A3A499BAFAD909512F0C716",
  "mb": 0.6,
  "license": "LGPL-3.0-only",
  "page": "https://modrinth.com/mod/architectury-api"
 },
 {
  "name": "Athena",
  "version": "4.0.6",
  "file": "athena-fabric-1.21.1-4.0.6.jar",
  "url": "https://cdn.modrinth.com/data/b1ZV3DIJ/versions/JfyYsWKP/athena-fabric-1.21.1-4.0.6.jar",
  "sha512": "6FEC9964E4CE654F3B0C7FD849E34CE28AAEED15557DC253BBEF59C577ADD7B96FE2FB454ECE6183AEC8356BCFD8D9C7327F48CED154F48B75C78043DEA7EB10",
  "mb": 0.1,
  "license": "MIT",
  "page": "https://modrinth.com/mod/athena-ctm"
 },
 {
  "name": "CobbleFurnies",
  "version": "1.2",
  "file": "CobbleFurnies-fabric-1.2.jar",
  "url": "https://cdn.modrinth.com/data/AXY1OO9m/versions/54k31lEX/CobbleFurnies-fabric-1.2.jar",
  "sha512": "9CB5A3F1E4604BB30F89F1FA9ACD83C260BF6852201AC7A50B40D13AFC80B09549DA655F74F9DD8CA090E5756422F02865DB1A32EAD857C74C664E2C96325EE0",
  "mb": 2.3,
  "license": "MIT",
  "page": "https://modrinth.com/mod/cobblefurnies"
 },
 {
  "name": "Cobblemon",
  "version": "1.8.4F",
  "file": "Cobblemon-fabric-1.8.4+1.21.1.jar",
  "url": "https://cdn.modrinth.com/data/MdwFAVRL/versions/PdOzYHKU/Cobblemon-fabric-1.8.4%2B1.21.1.jar",
  "sha512": "8BA3BC7EB0BB6D52309CA339DFC9FB11ADCFFEBE36D183E0A8DF7C8E741EDBCBC436C60CC877FD2B42AA224670825CF4F55946676B58ECF0E998F9FEAE2173C2",
  "mb": 140.0,
  "license": "MPL-2.0",
  "page": "https://modrinth.com/mod/cobblemon"
 },
 {
  "name": "Cobblemon Battle Tower",
  "version": "1.11.23",
  "file": "cobblemon_battle_tower-fabric-1.11.23.jar",
  "url": "https://cdn.modrinth.com/data/v19DG9nW/versions/Srt2YL8E/cobblemon_battle_tower-fabric-1.11.23.jar",
  "sha512": "C09374D94A99C46579DCEA0680208A8E31301CE5D033ADC4F17D35E9BAE5F6AD9879C02B8CFA18D3E95CB6ABEBA573F0438BB81E64AA941E4B1EEE542CAEF807",
  "mb": 52.1,
  "license": "LicenseRef-All-Rights-Reserved",
  "page": "https://modrinth.com/mod/cobblemon-battle-tower"
 },
 {
  "name": "Cobblemon Raid Dens",
  "version": "0.13.5+1.21.1",
  "file": "cobblemonraiddens-fabric-0.13.5+1.21.1.jar",
  "url": "https://cdn.modrinth.com/data/GebWh45l/versions/q12LeWnP/cobblemonraiddens-fabric-0.13.5%2B1.21.1.jar",
  "sha512": "42EB613F80A872AAA0217F9ABACCA112365BB10506158110D50744A5945078AE07038AF567BE99E43418C3BF5EE3A23B151F8548D888DE2718D474FFC5497865",
  "mb": 2.8,
  "license": "MIT",
  "page": "https://modrinth.com/mod/cobblemonraiddens"
 },
 {
  "name": "Fabric API",
  "version": "0.116.17+1.21.1",
  "file": "fabric-api-0.116.17+1.21.1.jar",
  "url": "https://cdn.modrinth.com/data/P7dR8mSH/versions/Mys3P7lK/fabric-api-0.116.17%2B1.21.1.jar",
  "sha512": "98C478217DA19181F0E4DF3EA6C2DA6BDBDA271C7544A4AEA048513A49B444F1FF5B71DC0DF6303017C48E8FF69B7BB6AF6179BDF0D0E09AB3C1EE08411857A8",
  "mb": 2.5,
  "license": "Apache-2.0",
  "page": "https://modrinth.com/mod/fabric-api"
 },
 {
  "name": "Cobblemon Fight or Flight Reborn",
  "version": "0.11.1",
  "file": "fightorflight-fabric-0.11.1.jar",
  "url": "https://cdn.modrinth.com/data/cTdIg5HZ/versions/ot7BirRv/fightorflight-fabric-0.11.1.jar",
  "sha512": "C8C315A3125C90F52B81C642CCE483697CEA39E09BE48C9E1A2FE75E006ED743DAB0D887D169A4D6743A99240EAA88273703C60C4B7A06D3BF8BAA3C9FEDE839",
  "mb": 1.5,
  "license": "MIT",
  "page": "https://modrinth.com/mod/cobblemon-fight-or-flight-reborn"
 },
 {
  "name": "Follow me [Cobblemon]",
  "version": "2.2.1",
  "file": "followme-2.2.1.jar",
  "url": "https://cdn.modrinth.com/data/AKkRTKQc/versions/bD4BNSL3/followme-2.2.1.jar",
  "sha512": "EB92413D32134A1B3A918228AC9AEE95F201655F5F067F50833AD5CC13C5048224C7950B1D7F60D39905C9FDE799772FE5773537CBD6675A7393BA2852A0C5FE",
  "mb": 0.5,
  "license": "MIT",
  "page": "https://modrinth.com/mod/follow-me-cobblemon"
 },
 {
  "name": "Forge Config API Port",
  "version": "v21.1.6-1.21.1-Fabric",
  "file": "ForgeConfigAPIPort-v21.1.6-1.21.1-Fabric.jar",
  "url": "https://cdn.modrinth.com/data/ohNO6lps/versions/N5qzq0XV/ForgeConfigAPIPort-v21.1.6-1.21.1-Fabric.jar",
  "sha512": "CD9296E78BA969F7AED6E3692AA25EB61C102C79C55CA5F9592576BACAA26FEAB5D5D48FA30CF07CA852E0F1D42AFC4D4558FEFF69A67B225183D2BC15898CF9",
  "mb": 0.6,
  "license": "MPL-2.0",
  "page": "https://modrinth.com/mod/forge-config-api-port"
 },
 {
  "name": "Geckolib",
  "version": "4.9.3",
  "file": "geckolib-fabric-1.21.1-4.9.3.jar",
  "url": "https://cdn.modrinth.com/data/8BmcQJ2H/versions/tmxM7U4W/geckolib-fabric-1.21.1-4.9.3.jar",
  "sha512": "E26A1C15D10CF5C1234309DBB82FB23D4783B2594B2454B16A9C964FAB9E9912BE66904B03EB01EBB94DADA298D98DE18D66F5E3F4173F771579B56E13F7DA16",
  "mb": 0.7,
  "license": "MIT",
  "page": "https://modrinth.com/mod/geckolib"
 },
 {
  "name": "Cobblemon: Mega Showdown",
  "version": "1.3.3+1.8.4+1.21.1-release",
  "file": "mega_showdown-fabric-1.3.3+1.8.4+1.21.1-release.jar",
  "url": "https://cdn.modrinth.com/data/SszvX85I/versions/pToIDLtC/mega_showdown-fabric-1.3.3%2B1.8.4%2B1.21.1-release.jar",
  "sha512": "286DB78D509BC6A3501B88E4CCCA8BEACF22EBE0E44FF9AF475A4419819FC67C4D10562572155B80A95B8F8A49CE7FC296FD8194DAF1F557D85A8855CF21F7C3",
  "mb": 20.3,
  "license": "LicenseRef-MEGA-SHOWDOWN-LICENSE-v2.1",
  "page": "https://modrinth.com/mod/cobblemon-mega-showdown"
 },
 {
  "name": "o\u03c9o (owo-lib)",
  "version": "0.13.0-alpha.15+1.21",
  "file": "owo-lib-0.13.0-alpha.15+1.21.jar",
  "url": "https://cdn.modrinth.com/data/ccKDOlHs/versions/m3XmDd8j/owo-lib-0.13.0-alpha.15%2B1.21.jar",
  "sha512": "939299E72209F8E5A4937FBB2290ED63F7BC5CAF2677E6D9FBEA8BE8049D5AD39C1173F0B61ABAE7CFDDCF7CCEB9802F7E2D60B9E8A0FDDA399FCBAD8F0AC5B4",
  "mb": 2.0,
  "license": "MIT",
  "page": "https://modrinth.com/mod/owo-lib"
 },
 {
  "name": "Radical Cobblemon Trainers API",
  "version": "0.16.2-beta",
  "file": "rctapi-fabric-1.21.1-0.16.2-beta.jar",
  "url": "https://cdn.modrinth.com/data/CBfM2yw7/versions/DoQNDXAU/rctapi-fabric-1.21.1-0.16.2-beta.jar",
  "sha512": "E53F223229454F101A5CDC25C0A85A964B680535B4F85B490F7BCA0E9481309E4DB40C029BE0FD2ED6473C9F1B58657C5DC84E01D16F66E85DDCE232ABBB02B9",
  "mb": 0.3,
  "license": "LicenseRef-MCOML",
  "page": "https://modrinth.com/mod/rctapi"
 },
 {
  "name": "Radical Cobblemon Trainers",
  "version": "0.19.2-beta",
  "file": "rctmod-fabric-1.21.1-0.19.2-beta.jar",
  "url": "https://cdn.modrinth.com/data/lRwTUnD7/versions/rgfBfnW9/rctmod-fabric-1.21.1-0.19.2-beta.jar",
  "sha512": "8CC03101D02752B0091A16B687E0CCA3B516935B4E440B9767808A04FCC4C9C2E614AA88DDF900895A47C48E31427664361C26DA8A5F4D17D7D1015308B2FA12",
  "mb": 6.8,
  "license": "LicenseRef-MCOML",
  "page": "https://modrinth.com/mod/rctmod"
 },
 {
  "name": "Wild Battle API",
  "version": "1.1.2",
  "file": "wild_battle_api-fabric-1.1.2.jar",
  "url": "https://cdn.modrinth.com/data/v7VZE8kh/versions/GoEGm1Ea/wild_battle_api-fabric-1.1.2.jar",
  "sha512": "1154C26F932FF8A2DB09C6698A182E0CA0AA290A8A319C873E6F118EF482C20DD130C41426A63F36787D785FE9574BD52517F0053160F977B13FAA49852EF56E",
  "mb": 0.3,
  "license": "LicenseRef-All-Rights-Reserved",
  "page": "https://modrinth.com/mod/wild-battle-api"
 }
]
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
