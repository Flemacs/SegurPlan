@echo off
REM Restaura la base SegurPlan desde un respaldo .sql (Windows).
REM Uso:  set DB_PASSWORD=clave  &&  scripts\restore.bat backups\segurplan_AAAAMMDD_HHMMSS.sql
setlocal
if "%~1"=="" ( echo Indique el archivo .sql a restaurar & exit /b 1 )
if "%PG_BIN%"=="" set "PG_BIN=C:\Program Files\PostgreSQL\16\bin"
if "%DB_HOST%"=="" set DB_HOST=localhost
if "%DB_PORT%"=="" set DB_PORT=5432
if "%DB_NAME%"=="" set DB_NAME=SegurPlan
if "%DB_USER%"=="" set DB_USER=postgres
if "%DB_PASSWORD%"=="" ( echo Defina DB_PASSWORD & exit /b 1 )

set /p R=Se reemplazaran los datos de %DB_NAME%. Escriba SI para continuar: 
if /i not "%R%"=="SI" ( echo Cancelado. & exit /b 1 )

set PGPASSWORD=%DB_PASSWORD%
"%PG_BIN%\psql.exe" -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -v ON_ERROR_STOP=1 -q -f "%~1"
if errorlevel 1 ( echo ERROR al restaurar & exit /b 1 )

echo Restauracion completada.
endlocal
