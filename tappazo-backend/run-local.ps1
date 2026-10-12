$ErrorActionPreference = 'Stop'

$javaHome = 'C:\Users\jakle\.jdk\jdk-25\jdk-25.0.4.1+1'
if (-not (Test-Path (Join-Path $javaHome 'bin\java.exe'))) {
    throw "JDK 25 was not found at $javaHome."
}

$defaultRoute = Get-NetRoute -DestinationPrefix '0.0.0.0/0' |
    Where-Object State -eq 'Alive' |
    Sort-Object RouteMetric |
    Select-Object -First 1
if ($null -eq $defaultRoute) {
    throw 'No active IPv4 route was found. Connect this PC to the router first.'
}
$lanAddress = Get-NetIPAddress -AddressFamily IPv4 -InterfaceIndex $defaultRoute.InterfaceIndex |
    Where-Object {
        $_.AddressState -eq 'Preferred' -and
        $_.IPAddress -notmatch '^(127\.|169\.254\.)'
    } |
    Select-Object -First 1
if ($null -eq $lanAddress) {
    throw 'Could not find the PC LAN IPv4 address.'
}

$securePassword = Read-Host 'MySQL root password (input hidden)' -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)

try {
    $env:LOCAL_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    if ([string]::IsNullOrWhiteSpace($env:LOCAL_DB_PASSWORD)) {
        throw 'A MySQL root password is required.'
    }

    $env:JAVA_HOME = $javaHome
    $env:Path = "$javaHome\bin;$env:Path"
    $env:SPRING_PROFILES_ACTIVE = 'dev,local-auth'
    $env:LOCAL_DB_HOST = '127.0.0.1'
    $env:LOCAL_DB_PORT = '3306'
    $env:LOCAL_DB_NAME = 'tappazo_local'
    $env:LOCAL_DB_USER = 'root'
    $env:LOCAL_LIVEKIT_SERVER_URL = "wss://$($lanAddress.IPAddress):5173/livekit"

    Set-Location $PSScriptRoot
    Write-Host "Local LiveKit client URL: $env:LOCAL_LIVEKIT_SERVER_URL"
    .\mvnw.cmd spring-boot:run
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    Remove-Item Env:LOCAL_DB_PASSWORD -ErrorAction SilentlyContinue
}
