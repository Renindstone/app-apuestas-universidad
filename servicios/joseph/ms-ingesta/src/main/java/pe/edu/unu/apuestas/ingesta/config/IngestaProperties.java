package pe.edu.unu.apuestas.ingesta.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades ingesta.* del application.yml: de dónde se descarga (football-data y el calendario de fixturedownload)
 * y qué temporadas se cargan.
 */
@ConfigurationProperties("ingesta")
public record IngestaProperties(String urlBase, String temporadaActual, String temporadaAnterior, String urlCalendario) {

    // Sin ingesta.url-calendario se usa fixturedownload.com.
    public String urlCalendario() {
        return urlCalendario == null || urlCalendario.isBlank() ? "https://fixturedownload.com" : urlCalendario;
    }
}
