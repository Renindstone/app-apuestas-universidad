package pe.edu.unu.apuestas.predicciones.dto;

/** Una selección con su probabilidad (3 decimales) y su cuota justa = 1 / probabilidad (2 decimales). linea en null si no lleva. */
public record MercadoDTO(String mercado, String seleccion, Double linea, double probabilidad, double cuotaJusta) {

    public static MercadoDTO de(String mercado, String seleccion, Double linea, double probabilidad) {
        // Una probabilidad de 0 daría una cuota infinita, que JSON no admite: se toma como mínimo 0.001.
        double cuotaJusta = 1 / Math.max(probabilidad, 0.001);
        return new MercadoDTO(mercado, seleccion, linea, Math.round(probabilidad * 1000) / 1000.0, Math.round(cuotaJusta * 100) / 100.0);
    }
}
