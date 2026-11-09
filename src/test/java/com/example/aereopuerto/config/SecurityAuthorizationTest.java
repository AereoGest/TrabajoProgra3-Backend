package com.example.aereopuerto.config;

import com.example.aereopuerto.auth.controller.AuthController;
import com.example.aereopuerto.auth.controller.UserController;
import com.example.aereopuerto.auth.dto.PerfilResponse;
import com.example.aereopuerto.auth.dto.UsuarioResponse;
import com.example.aereopuerto.auth.entity.Role;
import com.example.aereopuerto.auth.service.AuthService;
import com.example.aereopuerto.auth.service.JwtService;
import com.example.aereopuerto.auth.service.UserService;
import com.example.aereopuerto.controller.ReservaController;
import com.example.aereopuerto.controller.VueloController;
import com.example.aereopuerto.service.CheckinService;
import com.example.aereopuerto.service.ReservaService;
import com.example.aereopuerto.service.VueloService;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {VueloController.class, AuthController.class, UserController.class, ReservaController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VueloService vueloService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ReservaService reservaService;

    @MockitoBean
    private CheckinService checkinService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @Test
    @DisplayName("GET /api/vuelos sin autenticación devuelve 401")
    void obtenerTodosVuelos_SinAutenticacion_Retorna401() throws Exception {
        mockMvc.perform(get("/api/vuelos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    @DisplayName("GET /api/vuelos con rol USUARIO devuelve 403 Forbidden")
    void obtenerTodosVuelos_ConRolUsuario_Retorna403() throws Exception {
        mockMvc.perform(get("/api/vuelos"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLEADO")
    @DisplayName("GET /api/vuelos con rol EMPLEADO devuelve 403 Forbidden")
    void obtenerTodosVuelos_ConRolEmpleado_Retorna403() throws Exception {
        mockMvc.perform(get("/api/vuelos"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/vuelos con rol ADMIN devuelve 200 OK")
    void obtenerTodosVuelos_ConRolAdmin_Retorna200() throws Exception {
        when(vueloService.obtenerTodosLosVuelos()).thenReturn(List.of());

        mockMvc.perform(get("/api/vuelos"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/vuelos/disponibles es público y devuelve 200 OK")
    void obtenerVuelosDisponibles_Publico_Retorna200() throws Exception {
        when(vueloService.obtenerVuelosCliente()).thenReturn(List.of());

        mockMvc.perform(get("/api/vuelos/disponibles"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    @DisplayName("GET /api/auth/usuarios con rol USUARIO devuelve 403 Forbidden")
    void obtenerUsuarios_ConRolUsuario_Retorna403() throws Exception {
        mockMvc.perform(get("/api/auth/usuarios"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/auth/usuarios con rol ADMIN devuelve 200 OK")
    void obtenerUsuarios_ConRolAdmin_Retorna200() throws Exception {
        when(authService.obtenerUsuarios()).thenReturn(List.of(
                UsuarioResponse.builder().id(1).email("admin@aerogest.com").role(Role.ROLE_ADMIN).build()
        ));

        mockMvc.perform(get("/api/auth/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("ROLE_ADMIN"));
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    @DisplayName("PATCH /api/admin/users/haceradmin/{id} con rol USUARIO devuelve 403 Forbidden")
    void hacerAdmin_ConRolUsuario_Retorna403() throws Exception {
        mockMvc.perform(patch("/api/admin/users/haceradmin/2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/admin/users/haceradmin/{id} con rol ADMIN devuelve 200 OK")
    void hacerAdmin_ConRolAdmin_Retorna200() throws Exception {
        mockMvc.perform(patch("/api/admin/users/haceradmin/2"))
                .andExpect(status().isOk());

        verify(userService).cambiarRol(2, Role.ROLE_ADMIN);
    }

    @Test
    @WithMockUser(username = "admin@aerogest.com", roles = "ADMIN")
    @DisplayName("GET /api/auth/perfil devuelve el rol del usuario autenticado")
    void obtenerPerfil_Autenticado_RetornaRol() throws Exception {
        when(authService.obtenerPerfil()).thenReturn(
                PerfilResponse.builder()
                        .email("admin@aerogest.com")
                        .role("ROLE_ADMIN")
                        .isVerified(true)
                        .build()
        );

        mockMvc.perform(get("/api/auth/perfil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    @WithMockUser(roles = "EMPLEADO")
    @DisplayName("GET /api/reservas con rol EMPLEADO devuelve 403 Forbidden")
    void obtenerReservas_ConRolEmpleado_Retorna403() throws Exception {
        mockMvc.perform(get("/api/reservas"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/reservas con rol ADMIN devuelve 200 OK")
    void obtenerReservas_ConRolAdmin_Retorna200() throws Exception {
        when(reservaService.obtenerTodasLasReservas()).thenReturn(List.of());

        mockMvc.perform(get("/api/reservas"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    @DisplayName("POST /api/reservas con rol USUARIO devuelve 403 Forbidden")
    void crearReservaAdmin_ConRolUsuario_Retorna403() throws Exception {
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLEADO")
    @DisplayName("PUT /api/reservas/1 con rol EMPLEADO devuelve 403 Forbidden")
    void actualizarReserva_ConRolEmpleado_Retorna403() throws Exception {
        mockMvc.perform(put("/api/reservas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/reservas/1 con rol ADMIN ejecuta baja lógica y devuelve 204 No Content")
    void eliminarReserva_ConRolAdmin_Retorna204() throws Exception {
        mockMvc.perform(delete("/api/reservas/1"))
                .andExpect(status().isNoContent());

        verify(reservaService).cancelarReserva(1);
    }
}
