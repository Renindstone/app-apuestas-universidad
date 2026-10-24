package pe.edu.unu.apuestas.usuarios.controller;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Flujo completo por HTTP contra la base de datos H2 de pruebas: registro, duplicado, login, perfil y banca.
 */
@SpringBootTest
class FlujoUsuarioTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    private static String json(String... pares) {
        StringBuilder s = new StringBuilder("{");
        for (int i = 0; i < pares.length; i += 2) {
            s.append(i == 0 ? "" : ",").append('"').append(pares[i]).append("\":").append(pares[i + 1]);
        }
        return s.append('}').toString();
    }

    @Test
    void registroLoginPerfilYBanca() throws Exception {
        mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json("nombreUsuario", "\"lucia\"", "email", "\"Lucia@Correo.pe\"", "clave", "\"Secreta123\"", "claveConfirmacion", "\"Secreta123\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("APOSTADOR"));

        // CB-08 por HTTP: el mismo correo en minúsculas → 409.
        mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json("nombreUsuario", "\"lucia2\"", "email", "\"lucia@correo.pe\"", "clave", "\"Secreta123\"", "claveConfirmacion", "\"Secreta123\"")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("USUARIO_DUPLICADO"));

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("nombreUsuario", "\"lucia\"", "clave", "\"Secreta123\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(not(emptyString())))
                .andExpect(jsonPath("$.expiraEn").value(28800));

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("nombreUsuario", "\"lucia\"", "clave", "\"mala-clave\"")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detalle").value("Usuario o contraseña incorrectos"));

        // El admin inicial existe (se creó al arrancar con admin.clave), así que lucia es el usuario 2.
        mvc.perform(get("/me").header("X-Usuario-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("lucia@correo.pe"))
                .andExpect(jsonPath("$.banca").value(0));

        mvc.perform(put("/me/banca").header("X-Usuario-Id", "2").contentType(MediaType.APPLICATION_JSON).content(json("banca", "500")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.banca").value(500.00));

        mvc.perform(put("/me/banca").header("X-Usuario-Id", "2").contentType(MediaType.APPLICATION_JSON).content(json("banca", "-5")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDACION"));
    }

    @Test
    void elAdminInicialPuedeIniciarSesion() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("nombreUsuario", "\"admin\"", "clave", "\"admin-de-pruebas\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void sinCabeceraDeUsuarioResponde401() throws Exception {
        mvc.perform(get("/me")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("NO_AUTORIZADO"));
    }

    @Test
    void datosNoValidosResponden400ConMensaje() throws Exception {
        mvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(json("nombreUsuario", "\"x\"", "email", "\"no-es-correo\"", "clave", "\"123\"", "claveConfirmacion", "\"123\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDACION"));
    }
}
