package pe.edu.unu.apuestas.ingesta.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades ingesta.* del application.yml: de dónde se descarga y qué temporadas se cargan.
 */
@ConfigurationProperties("ingesta")
public record IngestaProperties(String urlBase, String temporadaActual, String temporadaAnterior, boolean usarMuestras) {
}
