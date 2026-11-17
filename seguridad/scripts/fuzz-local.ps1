param([string]$BaseUrl = 'http://127.0.0.1:8080')
if ($BaseUrl -notmatch '^http://(localhost|127\.0\.0\.1)(:\d+)?$') { throw 'Solo localhost autorizado' }
$paths = @('/api/csrf','/api/NO_EXISTE','/api/admin/NO_EXISTE','/api/usuarios/login')
foreach ($path in $paths) {
 try { $r=Invoke-WebRequest -Uri ($BaseUrl+$path) -Method GET -TimeoutSec 5 -SkipHttpErrorCheck; Write-Output "$path $($r.StatusCode)" }
 catch { Write-Output "$path ERROR: $($_.Exception.Message)" }
}
