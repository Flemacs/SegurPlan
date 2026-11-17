param([int]$Port=8080)
if ($Port -lt 1 -or $Port -gt 65535) { throw "Puerto invalido" }
New-Item -ItemType Directory -Force "seguridad/evidencias" | Out-Null
if (Get-Command nmap -ErrorAction SilentlyContinue) { nmap -sV -p $Port 127.0.0.1 | Out-File -Encoding utf8 seguridad/evidencias/nmap-local.txt }
else { Write-Warning "Nmap no instalado; sin evidencia" }
if (Get-Command nikto -ErrorAction SilentlyContinue) { nikto -host "http://127.0.0.1:$Port" | Out-File -Encoding utf8 seguridad/evidencias/nikto-local.txt }
else { Write-Warning "Nikto no instalado; sin evidencia" }
