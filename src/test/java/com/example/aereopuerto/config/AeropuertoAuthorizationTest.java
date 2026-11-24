package com.example.aereopuerto.config;

import com.example.aereopuerto.auth.service.JwtService;
import com.example.aereopuerto.controller.AeropuertoController;
import com.example.aereopuerto.service.AeropuertoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Los empleados pueden consultar aeropuertos, pero solo el administrador puede crearlos, modificarlos o eliminarlos. */
@WebMvcTest(controllers = AeropuertoController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AeropuertoAuthorizationTest {

    private static final String AEROPUERTO_JSON = """
            {"nombre":"Aeropuerto Internacional Ezeiza","codigoIata":"EZE","ciudad":"Buenos Aires","pais":"Argentina"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AeropuertoService aeropuertoService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @Test
    @WithMockUser(roles = "EMPLEADO")
    @DisplayName("EMPLEADO puede listar aeropuertos")
    void empleadoPuedeListar() throws Exception {
        when(aeropuertoService.obtenerTodosLosAeropuertos()).thenReturn(List.of());

        mockMvc.perform(get("/api/aeropuertos"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLEADO")
    @DisplayName("EMPLEADO no puede crear aeropuertos (403)")
    void empleadoNoPuedeCrear() throws Exception {
        mockMvc.perform(post("/api/aeropuertos").contentType(MediaType.APPLICATION_JSON).content(AEROPUERTO_JSON))
                .andExpect(status().isForbidden());
        verify(aeropuertoService, never()).crearAeropuerto(any());
    }

    @Test
    @WithMockUser(roles = "EMPLEADO")
    @DisplayName("EMPLEADO no puede modificar aeropuertos (403)")
    void empleadoNoPuedeModificar() throws Exception {
        mockMvc.perform(put("/api/aeropuertos/1").contentType(MediaType.APPLICATION_JSON).content(AEROPUERTO_JSON))
                .andExpect(status().isForbidden());
        verify(aeropuertoService, never()).EditarAeropuerto(anyInt(), any());
    }

    @Test
    @WithMockUser(roles = "EMPLEADO")
    @DisplayName("EMPLEADO no puede eliminar aeropuertos (403)")
    void empleadoNoPuedeEliminar() throws Exception {
        mockMvc.perform(delete("/api/aeropuertos/1"))
                .andExpect(status().isForbidden());
        verify(aeropuertoService, never()).eliminarAeropuerto(anyInt());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN puede crear, modificar y eliminar aeropuertos")
    void adminPuedeGestionar() throws Exception {
        mockMvc.perform(post("/api/aeropuertos").contentType(MediaType.APPLICATION_JSON).content(AEROPUERTO_JSON))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/api/aeropuertos/1").contentType(MediaType.APPLICATION_JSON).content(AEROPUERTO_JSON))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/aeropuertos/1"))
                .andExpect(status().isNoContent());

        verify(aeropuertoService).crearAeropuerto(any());
        verify(aeropuertoService).EditarAeropuerto(anyInt(), any());
        verify(aeropuertoService).eliminarAeropuerto(1);
    }
}
