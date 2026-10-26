package pe.edu.unu.apuestas.gateway.seguridad;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Valida el JWT que firmó ms-usuarios (HS256 con JWT_SECRET): firma, vencimiento y que traiga sub y rol.
 */
@Component
public class ValidadorJwt {

    private final SecretKey clave;

    // Arma la clave al arrancar. Si JWT_SECRET es corta, el gateway no arranca: así el error se ve enseguida.
    public ValidadorJwt(@Value("${jwt.secreto}") String secreto) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres (HS256).");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    // Devuelve los datos del token (sub = id del usuario, rol y usuario) o lanza TokenInvalidoException.
    public Claims validar(String token) {
        Claims datos;
        try {
            datos = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw new TokenInvalidoException("El token venció. Vuelve a iniciar sesión.");
        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenInvalidoException("El token no es válido.");
        }
        if (datos.getSubject() == null || datos.get("rol", String.class) == null) {
            throw new TokenInvalidoException("El token no trae el usuario o el rol.");
        }
        return datos;
    }
}
