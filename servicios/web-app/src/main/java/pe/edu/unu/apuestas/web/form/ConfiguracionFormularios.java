package pe.edu.unu.apuestas.web.form;

import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Ajustes comunes de todos los formularios.
 */
@ControllerAdvice
public class ConfiguracionFormularios {

    // Acepta montos y cuotas escritos con coma ("2,20") o con punto ("2.20"). Un campo vacío queda en null.
    @InitBinder
    public void decimales(WebDataBinder binder) {
        binder.registerCustomEditor(BigDecimal.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String texto) {
                String limpio = texto == null ? "" : texto.trim().replace(',', '.');
                setValue(limpio.isEmpty() ? null : new BigDecimal(limpio));
            }
        });
    }
}
