package pe.edu.unu.apuestas.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Único punto de entrada al sistema: enruta cada /api/... a su microservicio, valida el JWT,
 * aplica las reglas por rol y bloquea las rutas internas.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
