package pe.edu.unu.apuestas.ingesta.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import pe.edu.unu.apuestas.ingesta.config.IngestaProperties;

/**
 * Descarga un CSV de football-data como texto, una sola vez y sin reintentos (RN-ING-01).
 * Con ingesta.usar-muestras=true lo lee de src/main/resources/muestras/ y no usa la red.
 */
@Component
public class DescargadorCsv {

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final boolean usarMuestras;

    public DescargadorCsv(IngestaProperties propiedades) {
        this.usarMuestras = propiedades.usarMuestras();
    }

    // Lanza IOException si la fuente no responde o responde algo distinto de 200.
    public String descargar(String url) throws IOException, InterruptedException {
        byte[] bytes;
        if (usarMuestras) {
            // https://football-data.co.uk/mmz4281/2627/E0.csv → muestras/mmz4281/2627/E0.csv
            bytes = new ClassPathResource("muestras" + URI.create(url).getPath()).getContentAsByteArray();
        } else {
            HttpRequest pedido = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(60)).GET().build();
            HttpResponse<byte[]> respuesta = http.send(pedido, HttpResponse.BodyHandlers.ofByteArray());
            if (respuesta.statusCode() != 200) {
                throw new IOException("football-data respondió " + respuesta.statusCode() + ".");
            }
            bytes = respuesta.body();
        }
        // Los archivos son UTF-8, pero el servidor no lo declara.
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
