package pe.edu.unu.apuestas.apuestas.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import pe.edu.unu.apuestas.apuestas.client.CatalogoClient;
import pe.edu.unu.apuestas.apuestas.dto.ApuestaDTO;
import pe.edu.unu.apuestas.apuestas.dto.ApuestaRequest;
import pe.edu.unu.apuestas.apuestas.dto.EstadoApuesta;
import pe.edu.unu.apuestas.apuestas.dto.PartidoDTO;
import pe.edu.unu.apuestas.apuestas.dto.RendimientoDTO;
import pe.edu.unu.apuestas.apuestas.dto.RendimientoMercadoDTO;
import pe.edu.unu.apuestas.apuestas.entity.ApuestaUsuario;
import pe.edu.unu.apuestas.apuestas.entity.Resultado;
import pe.edu.unu.apuestas.apuestas.repository.ApuestaUsuarioRepository;

/**
 * Las apuestas que cada usuario registra y su rendimiento. Siempre se trabaja con el usuario del token (RN-14).
 */
@Service
public class MisApuestasService {

    private final ApuestaUsuarioRepository apuestas;
    private final CatalogoClient catalogo;

    public MisApuestasService(ApuestaUsuarioRepository apuestas, CatalogoClient catalogo) {
        this.apuestas = apuestas;
        this.catalogo = catalogo;
    }

    /**
     * POST /mis-apuestas: valida la selección (RN-09) y la casa, pide el partido a ms-catalogo y guarda su nombre
     * («Arsenal vs Chelsea») junto con la apuesta (RN-APU-05). 404 si el partido no existe, 503 si ms-catalogo no responde.
     */
    public ApuestaDTO registrar(Long usuarioId, ApuestaRequest datos) {
        Mercados.validar(datos.mercado(), datos.seleccion(), datos.linea());
        Mercados.validarCasa(datos.casa());
        PartidoDTO partido = Remoto.llamar("ms-catalogo", datos.partidoId(), () -> catalogo.partido(datos.partidoId()));
        return ApuestaDTO.de(apuestas.save(new ApuestaUsuario(usuarioId, partido.id(), partido.descripcion(), datos.mercado(),
                datos.seleccion(), datos.linea(), datos.casa(), datos.cuota(), datos.monto())));
    }

    // GET /mis-apuestas?estado=: las del usuario, de la más reciente a la más antigua. Sin estado, todas.
    public List<ApuestaDTO> listar(Long usuarioId, EstadoApuesta estado) {
        List<ApuestaUsuario> lista = estado == null ? apuestas.findByUsuarioIdOrderByFechaDescIdDesc(usuarioId)
                : estado == EstadoApuesta.PENDIENTE ? apuestas.findByUsuarioIdAndResultadoIsNullOrderByFechaDescIdDesc(usuarioId)
                : apuestas.findByUsuarioIdAndResultadoIsNotNullOrderByFechaDescIdDesc(usuarioId);
        return lista.stream().map(ApuestaDTO::de).toList();
    }

    /**
     * GET /mis-apuestas/rendimiento (RN-APU-07): cuenta todas las apuestas, pero el monto, el beneficio, el ROI y el
     * detalle por mercado solo salen de las liquidadas. ROI = beneficio / monto apostado.
     */
    public RendimientoDTO rendimiento(Long usuarioId) {
        List<ApuestaUsuario> todas = apuestas.findByUsuarioIdOrderByFechaDescIdDesc(usuarioId);
        List<ApuestaUsuario> liquidadas = todas.stream().filter(a -> a.getResultado() != null).toList();
        int ganadas = (int) liquidadas.stream().filter(a -> a.getResultado() == Resultado.GANADA).count();
        Map<String, List<ApuestaUsuario>> porMercado = liquidadas.stream()
                .collect(Collectors.groupingBy(ApuestaUsuario::getMercado, TreeMap::new, Collectors.toList()));
        return new RendimientoDTO(todas.size(), todas.size() - liquidadas.size(), ganadas, liquidadas.size() - ganadas,
                monto(liquidadas), beneficio(liquidadas), roi(liquidadas),
                porMercado.entrySet().stream()
                        .map(e -> new RendimientoMercadoDTO(e.getKey(), e.getValue().size(), beneficio(e.getValue()), roi(e.getValue())))
                        .toList());
    }

    private static BigDecimal monto(List<ApuestaUsuario> lista) {
        return lista.stream().map(ApuestaUsuario::getMonto).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    private static BigDecimal beneficio(List<ApuestaUsuario> lista) {
        return lista.stream().map(ApuestaUsuario::getGanancia).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }

    // Con 3 decimales. 0 si todavía no hay apuestas liquidadas.
    private static double roi(List<ApuestaUsuario> lista) {
        BigDecimal monto = monto(lista);
        return monto.signum() == 0 ? 0 : beneficio(lista).divide(monto, 3, RoundingMode.HALF_UP).doubleValue();
    }
}
