package pe.edu.unu.apuestas.usuarios.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import pe.edu.unu.apuestas.usuarios.entity.Usuario;

/**
 * Firma los JWT (HS256 con JWT_SECRET). El api-gateway los valida con la misma clave.
 */
@Service
public class JwtService {

    private final SecretKey clave;
    private final Duration duracion;

    // Si JWT_SECRET tiene menos de 32 caracteres, el servicio no arranca: HS256 no la aceptaría.
    public JwtService(@Value("${jwt.secreto}") String secreto, @Value("${jwt.horas}") long horas) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres (HS256).");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.duracion = Duration.ofHours(horas);
    }

    // Token con sub = id del usuario, y los claims usuario y rol. Vence en 8 horas (RN-USU-05).
    public String generar(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("usuario", usuario.getNombreUsuario())
                .claim("rol", usuario.getRol().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(duracion)))
                .signWith(clave)
                .compact();
    }

    // Segundos de vida del token, para el campo expiraEn de la respuesta (8 h = 28800).
    public long segundosDeVida() {
        return duracion.toSeconds();
    }
}
