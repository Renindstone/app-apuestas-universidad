package pe.edu.unu.apuestas.catalogo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unu.apuestas.catalogo.dto.LigaDTO;
import pe.edu.unu.apuestas.catalogo.service.CatalogoService;

/**
 * GET /api/catalogo/ligas (público).
 */
@RestController
public class LigaController {

    private final CatalogoService catalogo;

    public LigaController(CatalogoService catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping("/ligas")
    public List<LigaDTO> ligas() {
        return catalogo.ligas();
    }
}
