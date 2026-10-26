package pe.edu.unu.apuestas.ingesta.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.stereotype.Component;

/**
 * Descarga un CSV de football-data como texto, una sola vez y sin reintentos (RN-ING-01).
 */
@Component
public class DescargadorCsv {

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    // Lanza IOException si la fuente no responde o responde algo distinto de 200.
    public String descargar(String url) throws IOException, InterruptedException {
        HttpRequest pedido = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(60)).GET().build();
        HttpResponse<byte[]> respuesta = http.send(pedido, HttpResponse.BodyHandlers.ofByteArray());
        if (respuesta.statusCode() != 200) {
            throw new IOException("football-data respondió " + respuesta.statusCode() + ".");
        }
        // Los archivos son UTF-8, pero el servidor no lo declara.
        return new String(respuesta.body(), StandardCharsets.UTF_8);
    }
}
