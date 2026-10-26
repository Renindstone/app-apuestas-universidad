package pe.edu.unu.apuestas.cuotas.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unu.apuestas.cuotas.dto.CuotaDTO;
import pe.edu.unu.apuestas.cuotas.dto.CuotaManualRequest;
import pe.edu.unu.apuestas.cuotas.dto.ProbabilidadesMercadoDTO;
import pe.edu.unu.apuestas.cuotas.service.CuotaService;

/**
 * Rutas públicas (vía gateway: /api/cuotas + ruta). Las usan la web y ms-apuestas.
 */
@RestController
public class CuotaController {

    private final CuotaService cuotas;

    public CuotaController(CuotaService cuotas) {
        this.cuotas = cuotas;
    }

    // ?casa=bet365 es opcional: sin ella, las 3 casas.
    @GetMapping("/partidos/{partidoId}/cuotas")
    public List<CuotaDTO> cuotasDePartido(@PathVariable Long partidoId, @RequestParam(required = false) String casa) {
        return cuotas.cuotasDePartido(partidoId, casa);
    }

    // ?casa=pinnacle&mercado=1X2&linea= (la línea va vacía en 1X2 y AMBOS_MARCAN).
    @GetMapping("/partidos/{partidoId}/probabilidades-mercado")
    public ProbabilidadesMercadoDTO probabilidadesMercado(@PathVariable Long partidoId, @RequestParam String casa,
            @RequestParam String mercado, @RequestParam(required = false) BigDecimal linea) {
        return cuotas.probabilidadesMercado(partidoId, casa, mercado, linea);
    }

    // Cuota de Betano cargada a mano. El usuario es el del token: X-Usuario-Id lo pone el gateway (RN-14). Responde 201.
    @PostMapping("/cuotas/manual")
    @ResponseStatus(HttpStatus.CREATED)
    public CuotaDTO guardarManual(@RequestHeader("X-Usuario-Id") Long usuarioId, @Valid @RequestBody CuotaManualRequest datos) {
        return cuotas.guardarManual(datos, usuarioId);
    }
}
