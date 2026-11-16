#!/bin/sh
# Lanzador para Mac y Linux: ./iniciar.sh dentro del repo. Guía: infra/DISTRIBUIDO.md
# Ejemplos: ./iniciar.sh            (menú)
#           ./iniciar.sh todo       (todo en esta máquina, sin preguntar)
#           ./iniciar.sh apagar     (apaga lo que corre en esta máquina)
cd "$(dirname "$0")" || exit 1
if ! command -v java >/dev/null 2>&1; then
  echo "Falta Java 17 o superior. Instala Java 21 (por ejemplo Temurin) y vuelve a intentar."
  exit 1
fi
exec java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 infra/Lanzador.java "$@"
