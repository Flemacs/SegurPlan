param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string]$Output = "seguridad/evidencias/smoke-local.csv"
)

$uri = [Uri]$BaseUrl

if ($uri.Host -notin @("localhost", "127.0.0.1", "::1")) {
    throw "Solo se permiten pruebas en localhost"
}

$tests = @(
    @{ method = "GET"; path = "/v3/api-docs" },
    @{ method = "GET"; path = "/swagger-ui/index.html" },
    @{ method = "GET"; path = "/api/csrf" },
    @{ method = "GET"; path = "/api/admin/asesores" },
    @{ method = "GET"; path = "/api/admin/catalogos/aseguradoras" }
)

$results = foreach ($t in $tests) {

    $url = $BaseUrl.TrimEnd('/') + $t.path

    try {
        $response = Invoke-WebRequest `
            -Uri $url `
            -Method $t.method `
            -UseBasicParsing `
            -TimeoutSec 10 `
            -ErrorAction Stop

        $status = [int]$response.StatusCode
        $nota = "Respuesta HTTP recibida"

    } catch {

        $exception = $_.Exception

        if ($null -ne $exception.Response) {
            $status = [int]$exception.Response.StatusCode
            $nota = "Respuesta HTTP de error recibida"
        } else {
            $status = "ERROR"
            $nota = $exception.Message
        }
    }

    [pscustomobject]@{
        fecha  = Get-Date -Format "o"
        metodo = $t.method
        ruta   = $t.path
        http   = $status
        nota   = $nota
    }
}

$directory = Split-Path -Parent $Output

if ($directory -and -not (Test-Path $directory)) {
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
}

$results | Export-Csv `
    -NoTypeInformation `
    -Encoding UTF8 `
    -Path $Output

$results | Format-Table -AutoSize

Write-Host ""
Write-Host "Resultados guardados en: $Output"
