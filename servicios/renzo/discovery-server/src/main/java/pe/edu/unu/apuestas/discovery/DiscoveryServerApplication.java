package pe.edu.unu.apuestas.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Servidor Eureka: cada microservicio se registra aquí con su spring.application.name, y el gateway
 * y los clientes Feign lo encuentran por ese nombre (lb://ms-catalogo), sin IPs fijas.
 * Panel con los servicios registrados: http://localhost:8761
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
