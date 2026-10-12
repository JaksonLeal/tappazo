$ErrorActionPreference = 'Stop'

function Get-LanIPv4Address {
    $defaultRoute = Get-NetRoute -DestinationPrefix '0.0.0.0/0' |
        Where-Object State -eq 'Alive' |
        Sort-Object RouteMetric |
        Select-Object -First 1

    if ($null -eq $defaultRoute) {
        throw 'No active IPv4 default route was found. Connect this PC to the router first.'
    }

    $address = Get-NetIPAddress -AddressFamily IPv4 -InterfaceIndex $defaultRoute.InterfaceIndex |
        Where-Object {
            $_.AddressState -eq 'Preferred' -and
            $_.IPAddress -notmatch '^(127\.|169\.254\.)'
        } |
        Select-Object -First 1

    if ($null -eq $address) {
        throw 'Could not find a usable IPv4 address on the active network interface.'
    }

    return $address.IPAddress
}

$lanIp = Get-LanIPv4Address
$certificateDirectory = Join-Path $env:LOCALAPPDATA 'Tappazo\livekit-test'
New-Item -ItemType Directory -Force -Path $certificateDirectory | Out-Null

$javaHome = 'C:\Users\jakle\.jdk\jdk-25\jdk-25.0.4.1+1'
$keytool = Join-Path $javaHome 'bin\keytool.exe'
if (-not (Test-Path $keytool)) {
    throw "JDK keytool was not found at $keytool."
}

$caStorePath = Join-Path $certificateDirectory 'ca.p12'
$caPasswordPath = Join-Path $certificateDirectory 'ca-password.txt'
$caCertificateFile = Join-Path $certificateDirectory 'tappazo-local-dev-ca.cer'
$caCertificatePem = Join-Path $certificateDirectory 'ca-cert.pem'
$serverStorePath = Join-Path $certificateDirectory 'server.p12'
$serverCsrPath = Join-Path $certificateDirectory 'server.csr'
$serverCertificatePath = Join-Path $certificateDirectory 'server-cert.pem'

if (-not (Test-Path $caStorePath) -or -not (Test-Path $caPasswordPath)) {
    $caPassword = [Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')
    [System.IO.File]::WriteAllText($caPasswordPath, $caPassword)

    & $keytool -genkeypair -alias ca -dname 'CN=Tappazo Local Development CA' `
        -keyalg RSA -keysize 3072 -validity 1825 -ext 'BC=ca:true' `
        -keystore $caStorePath -storetype PKCS12 -storepass $caPassword -keypass $caPassword
    if ($LASTEXITCODE -ne 0) { throw 'Could not create the local development certificate authority.' }
} else {
    $caPassword = (Get-Content -Raw $caPasswordPath).Trim()
}

$caCertificateStore = [System.Security.Cryptography.X509Certificates.X509Certificate2Collection]::new()
$caCertificateStore.Import($caStorePath, $caPassword, 'PersistKeySet')
$caCertificate = $caCertificateStore |
    Where-Object Subject -Like '*CN=Tappazo Local Development CA*' |
    Select-Object -First 1
if ($null -eq $caCertificate) { throw 'Could not read the local development CA certificate.' }

& $keytool -exportcert -alias ca -keystore $caStorePath -storetype PKCS12 `
    -storepass $caPassword -file $caCertificateFile
if ($LASTEXITCODE -ne 0) { throw 'Could not export the local CA certificate.' }
& $keytool -exportcert -rfc -alias ca -keystore $caStorePath -storetype PKCS12 `
    -storepass $caPassword -file $caCertificatePem
if ($LASTEXITCODE -ne 0) { throw 'Could not export the local CA certificate chain.' }

$trustedRoot = Get-ChildItem Cert:\CurrentUser\Root |
    Where-Object Thumbprint -eq $caCertificate.Thumbprint |
    Select-Object -First 1
if ($null -eq $trustedRoot) {
    $rootStore = [System.Security.Cryptography.X509Certificates.X509Store]::new(
        'Root',
        [System.Security.Cryptography.X509Certificates.StoreLocation]::CurrentUser
    )
    $rootStore.Open(
        [System.Security.Cryptography.X509Certificates.OpenFlags]::ReadWrite
    )
    try {
        $rootStore.Add($caCertificate)
    } finally {
        $rootStore.Close()
    }
}

if (Test-Path $serverStorePath) { Remove-Item -LiteralPath $serverStorePath -Force }
& $keytool -genkeypair -alias server -dname "CN=$lanIp" `
    -keyalg RSA -keysize 2048 -validity 365 -ext "SAN=IP:$lanIp" `
    -keystore $serverStorePath -storetype PKCS12 -storepass $caPassword -keypass $caPassword
if ($LASTEXITCODE -ne 0) { throw 'Could not create the HTTPS server key.' }
& $keytool -certreq -alias server -keystore $serverStorePath -storetype PKCS12 `
    -storepass $caPassword -file $serverCsrPath -ext "SAN=IP:$lanIp"
if ($LASTEXITCODE -ne 0) { throw 'Could not create the HTTPS server certificate request.' }
& $keytool -gencert -alias ca -keystore $caStorePath -storetype PKCS12 `
    -storepass $caPassword -infile $serverCsrPath -outfile $serverCertificatePath -rfc `
    -validity 365 -ext "SAN=IP:$lanIp" -ext 'EKU=serverAuth' `
    -ext 'KU=digitalSignature,keyEncipherment'
if ($LASTEXITCODE -ne 0) { throw 'Could not sign the HTTPS server certificate.' }
& $keytool -importcert -noprompt -alias ca -keystore $serverStorePath -storetype PKCS12 `
    -storepass $caPassword -file $caCertificateFile
if ($LASTEXITCODE -ne 0) { throw 'Could not add the CA chain to the HTTPS server keystore.' }
& $keytool -importcert -noprompt -alias server -keystore $serverStorePath -storetype PKCS12 `
    -storepass $caPassword -file $serverCertificatePath
if ($LASTEXITCODE -ne 0) { throw 'Could not install the signed HTTPS server certificate.' }
[System.IO.File]::WriteAllText(
    (Join-Path $certificateDirectory 'server-password.txt'),
    $caPassword
)

$env:LIVEKIT_NODE_IP = $lanIp
Set-Location (Join-Path $PSScriptRoot '..\..')
docker compose up -d --force-recreate livekit
if ($LASTEXITCODE -ne 0) {
    throw 'Could not start LiveKit. Check that Docker Desktop is running.'
}

Write-Host ''
Write-Host "PC LAN address: $lanIp"
Write-Host "Voice-test page: https://${lanIp}:5173"
Write-Host "LiveKit client URL: wss://${lanIp}:5173/livekit"
Write-Host "Android CA certificate: $caCertificateFile"
Write-Host ''
Write-Host 'For same-Wi-Fi access, add these Windows Firewall rules in an elevated PowerShell:'
Write-Host "New-NetFirewallRule -DisplayName 'Tappazo voice page (LAN)' -Direction Inbound -Action Allow -Protocol TCP -LocalPort 5173 -RemoteAddress LocalSubnet -Profile Any"
Write-Host "New-NetFirewallRule -DisplayName 'Tappazo LiveKit audio (LAN)' -Direction Inbound -Action Allow -Protocol UDP -LocalPort 7882 -RemoteAddress LocalSubnet -Profile Any"
