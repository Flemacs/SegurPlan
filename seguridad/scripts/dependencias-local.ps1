$mvn = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $mvn) { Write-Warning "Usar Maven integrado de NetBeans o instalar Maven"; exit 1 }
New-Item -ItemType Directory -Force seguridad/evidencias | Out-Null
mvn -B dependency:tree "-DoutputFile=seguridad/evidencias/dependency-tree.txt"
Write-Host "El arbol de dependencias NO identifica CVE. Consultar Snyk/Dependabot para CVE reales."
