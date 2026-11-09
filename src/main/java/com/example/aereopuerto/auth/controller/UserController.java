package com.example.aereopuerto.auth.controller;

import com.example.aereopuerto.auth.dto.ActualizarClienteAdminRequest;
import com.example.aereopuerto.auth.dto.CrearClienteAdminRequest;
import com.example.aereopuerto.auth.dto.UsuarioResponse;
import com.example.aereopuerto.auth.entity.Role;
import com.example.aereopuerto.auth.service.UserService;
import com.example.aereopuerto.model.enums.Identificador;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Usuarios Admin", description = "Gestión integral de clientes y usuarios (solo ADMIN)")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Filtrar usuarios",
            description = "Filtra usuarios multicriterio por texto, rol, tipo de documento y número. Solo ADMIN."
    )
    @ApiResponse(responseCode = "200", description = "Lista filtrada de usuarios obtenida exitosamente")
    @GetMapping("/filtrar")
    public ResponseEntity<List<UsuarioResponse>> buscarUsuarios(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Identificador identificador,
            @RequestParam(required = false) String numeroIdentificador,
            @RequestParam(required = false, defaultValue = "NOMBRE_ASC") String orden) {

        return ResponseEntity.ok(userService.buscarUsuariosConFiltros(
                texto, role, identificador, numeroIdentificador, orden));
    }

    @Operation(
            summary = "Filtrar lista de empleados",
            description = "Filtra exclusivamente la lista de empleados (ROLE_EMPLEADO) por texto, documento y orden. Solo ADMIN."
    )
    @ApiResponse(responseCode = "200", description = "Lista filtrada de empleados obtenida exitosamente")
    @GetMapping("/empleados")
    public ResponseEntity<List<UsuarioResponse>> buscarEmpleados(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Identificador identificador,
            @RequestParam(required = false) String numeroIdentificador,
            @RequestParam(required = false, defaultValue = "NOMBRE_ASC") String orden) {

        return ResponseEntity.ok(userService.buscarEmpleadosConFiltros(
                texto, identificador, numeroIdentificador, orden));
    }

    @Operation(
            summary = "Crear cliente / usuario",
            description = "Da de alta un nuevo cliente con su perfil completo y rol asignado. Solo ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cliente creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o documento/email duplicado"),
            @ApiResponse(responseCode = "403", description = "No autorizado")
    })
    @PostMapping
    public ResponseEntity<UsuarioResponse> crearCliente(@Valid @RequestBody CrearClienteAdminRequest request) {
        UsuarioResponse nuevo = userService.crearCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @Operation(
            summary = "Actualizar cliente / usuario",
            description = "Modifica los datos personales y de cuenta de un cliente existente. Solo ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o conflicto de datos"),
            @ApiResponse(responseCode = "403", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> actualizarCliente(
            @PathVariable Integer id,
            @Valid @RequestBody ActualizarClienteAdminRequest request) {
        return ResponseEntity.ok(userService.actualizarCliente(id, request));
    }

    @Operation(
            summary = "Eliminar cliente / usuario",
            description = "Elimina un usuario y su persona si no posee reservas o asignaciones asociadas. Solo ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cliente eliminado exitosamente"),
            @ApiResponse(responseCode = "400", description = "No se puede eliminar por integridad referencial"),
            @ApiResponse(responseCode = "403", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCliente(@PathVariable Integer id) {
        userService.eliminarCliente(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Convertir usuario en empleado",
            description = "Cambia el rol del usuario a EMPLEADO. Solo accesible por ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rol actualizado correctamente"),
            @ApiResponse(responseCode = "403", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PatchMapping("/emplear/{userId}")
    public ResponseEntity<String> hacerEmpleado(@PathVariable Integer userId) {
        userService.cambiarRol(userId, Role.ROLE_EMPLEADO);
        return ResponseEntity.ok("El usuario ahora es empleado");
    }

    @Operation(
            summary = "Convertir usuario en administrador",
            description = "Cambia el rol del usuario a ADMIN. Solo accesible por ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rol actualizado correctamente"),
            @ApiResponse(responseCode = "403", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PatchMapping("/haceradmin/{userId}")
    public ResponseEntity<String> hacerAdmin(@PathVariable Integer userId) {
        userService.cambiarRol(userId, Role.ROLE_ADMIN);
        return ResponseEntity.ok("El usuario ahora es administrador");
    }

    @Operation(
            summary = "Convertir usuario en usuario regular",
            description = "Cambia el rol del usuario a USUARIO. Solo accesible por ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rol actualizado correctamente"),
            @ApiResponse(responseCode = "403", description = "No autorizado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PatchMapping("/hacerusuario/{userId}")
    public ResponseEntity<String> hacerUsuario(@PathVariable Integer userId) {
        userService.cambiarRol(userId, Role.ROLE_USUARIO);
        return ResponseEntity.ok("El usuario ahora es usuario regular");
    }
}
