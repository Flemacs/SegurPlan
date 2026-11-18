param([string]$Database = 'SegurPlan', [string]$User = 'postgres', [string]$Output = '.\segurplan-lab.backup')
$pgdump = Get-Command pg_dump -ErrorAction SilentlyContinue
if (-not $pgdump) { throw 'Instalar PostgreSQL y agregar pg_dump al PATH' }
& pg_dump -h 127.0.0.1 -p 5432 -U $User -Fc -f $Output $Database
if ($LASTEXITCODE -ne 0) { throw 'Error en respaldo; comprobar conexion y permisos' }
Write-Output 'Respaldo creado; conservar fuera del repositorio.'
