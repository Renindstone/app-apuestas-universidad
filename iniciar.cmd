@echo off
rem Lanzador para Windows: doble clic, o "iniciar" en una consola dentro del repo. Guia: infra\DISTRIBUIDO.md
rem Ejemplos: iniciar            (menu)
rem           iniciar todo       (todo en esta maquina, sin preguntar)
rem           iniciar apagar     (apaga lo que corre en esta maquina)
chcp 65001 >nul
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo Falta Java 17 o superior. Instala Java 21 ^(por ejemplo Temurin^) y vuelve a intentar.
  pause
  exit /b 1
)
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 infra\Lanzador.java %*
if errorlevel 1 pause
