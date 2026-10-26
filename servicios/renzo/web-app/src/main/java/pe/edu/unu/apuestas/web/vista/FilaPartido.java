package pe.edu.unu.apuestas.web.vista;

import java.util.List;
import java.util.stream.Collectors;

import pe.edu.unu.apuestas.web.dto.PartidoDTO;

/**
 * Un renglón de la lista de Hoy: el partido, las probabilidades 1X2 del modelo y las cuotas 1X2 de bet365.
 * modelo1x2 o cuotas1x2 quedan en null si su servicio no respondió.
 */
public record FilaPartido(PartidoDTO partido, List<Double> modelo1x2, List<CeldaCuota> cuotas1x2) {

    public boolean sinModelo() {
        return modelo1x2 == null;
    }

    public boolean sinCuotas() {
        return cuotas1x2 == null;
    }

    // "57 / 23 / 20": probabilidades del modelo en porcentaje entero.
    public String modeloTexto() {
        return modelo1x2 == null ? "— / — / —"
                : modelo1x2.stream().map(p -> p == null ? "—" : String.valueOf(Math.round(p * 100))).collect(Collectors.joining(" / "));
    }
}
