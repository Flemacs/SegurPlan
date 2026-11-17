@echo off
REM Respaldo manual de la base SegurPlan con pg_dump (Windows).
REM Uso:  set DB_PASSWORD=clave  &&  scripts\backup.bat
REM Si pg_dump no esta en el PATH, ajuste PG_BIN.
setlocal
if "%PG_BIN%"=="" set "PG_BIN=C:\Program Files\PostgreSQL\16\bin"
if "%DB_HOST%"=="" set DB_HOST=localhost
if "%DB_PORT%"=="" set DB_PORT=5432
if "%DB_NAME%"=="" set DB_NAME=SegurPlan
if "%DB_USER%"=="" set DB_USER=postgres
if "%DB_PASSWORD%"=="" ( echo Defina DB_PASSWORD & exit /b 1 )

set PGPASSWORD=%DB_PASSWORD%
if not exist backups mkdir backups

for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmmss"') do set TS=%%i
set ARCHIVO=backups\segurplan_%TS%.sql

"%PG_BIN%\pg_dump.exe" -h %DB_HOST% -p %DB_PORT% -U %DB_USER% --format=plain --clean --if-exists --no-owner -f "%ARCHIVO%" %DB_NAME%
if errorlevel 1 ( echo ERROR al crear el respaldo & exit /b 1 )

echo Respaldo creado: %ARCHIVO%
endlocal
