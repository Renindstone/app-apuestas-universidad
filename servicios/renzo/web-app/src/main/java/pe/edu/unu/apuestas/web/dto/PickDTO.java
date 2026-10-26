package pe.edu.unu.apuestas.web.dto;

/** La apuesta del día que eligió ms-apuestas. */
public record PickDTO(Long id, Long partidoId, String partido, String mercado, String seleccion, Double linea, String casa,
        double cuota, double probabilidad, double valorEsperado, double stakePct) implements Seleccion {

    // La cuota justa no viene en el JSON: es 1 / probabilidad.
    public double cuotaJusta() {
        return 1 / probabilidad;
    }
}
