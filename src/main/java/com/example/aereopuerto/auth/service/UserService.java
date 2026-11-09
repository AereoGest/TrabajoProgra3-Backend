package com.example.aereopuerto.auth.service;

import com.example.aereopuerto.auth.dto.ActualizarClienteAdminRequest;
import com.example.aereopuerto.auth.dto.CrearClienteAdminRequest;
import com.example.aereopuerto.auth.dto.UsuarioResponse;
import com.example.aereopuerto.auth.entity.Role;
import com.example.aereopuerto.auth.entity.User;
import com.example.aereopuerto.auth.repository.UserRepository;
import com.example.aereopuerto.model.Favorito;
import com.example.aereopuerto.model.Persona;
import com.example.aereopuerto.repository.AsignacionRepository;
import com.example.aereopuerto.repository.CarritoRepository;
import com.example.aereopuerto.repository.FavoritoRepository;
import com.example.aereopuerto.repository.PersonaRepository;
import com.example.aereopuerto.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PersonaRepository personaRepository;
    private final ReservaRepository reservaRepository;
    private final AsignacionRepository asignacionRepository;
    private final CarritoRepository carritoRepository;
    private final FavoritoRepository favoritoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public void cambiarRol(Integer userId, Role nuevoRol) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (nuevoRol == Role.ROLE_INCOMPLETO) {
            throw new IllegalArgumentException("No se puede asignar el rol INCOMPLETO manualmente");
        }

        user.setRole(nuevoRol);
        userRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = "clientes", allEntries = true)
    public UsuarioResponse crearCliente(CrearClienteAdminRequest request) {
        if (request.getRole() == Role.ROLE_INCOMPLETO) {
            throw new IllegalArgumentException("No se puede crear un cliente con rol INCOMPLETO");
        }

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("El email ya se encuentra registrado");
        }

        String numeroDoc = request.getNumeroIdentificador().trim();
        if (personaRepository.findByNumeroIdentificador(numeroDoc).isPresent()) {
            throw new IllegalArgumentException("El número de documento ya se encuentra registrado");
        }

        Persona persona = Persona.builder()
                .nombre(request.getNombre().trim())
                .apellido(request.getApellido().trim())
                .identificador(request.getIdentificador())
                .numeroIdentificador(numeroDoc)
                .fechaNacimiento(request.getFechaNacimiento())
                .sexo(request.getSexo())
                .build();
        persona = personaRepository.save(persona);

        User user = User.builder()
                .persona(persona)
                .email(email)
                .telefono(request.getTelefono().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .perfilCompleto(true)
                .isVerified(true)
                .twoFactorEnabled(false)
                .build();
        user = userRepository.save(user);

        return authService.mapearUsuarioResponse(user);
    }

    @Transactional
    @CacheEvict(value = "clientes", allEntries = true)
    public UsuarioResponse actualizarCliente(Integer id, ActualizarClienteAdminRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (request.getRole() == Role.ROLE_INCOMPLETO) {
            throw new IllegalArgumentException("No se puede asignar el rol INCOMPLETO");
        }

        String newEmail = request.getEmail().trim().toLowerCase();
        if (!user.getEmail().equalsIgnoreCase(newEmail) && userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("El email ya está en uso por otro usuario");
        }

        Persona persona = user.getPersona();
        if (persona == null) {
            persona = new Persona();
        }

        String newDoc = request.getNumeroIdentificador().trim();
        Integer personaId = persona.getId();
        personaRepository.findByNumeroIdentificador(newDoc)
                .ifPresent(existing -> {
                    if (personaId == null || !existing.getId().equals(personaId)) {
                        throw new IllegalArgumentException("El número de documento ya está en uso por otro usuario");
                    }
                });

        persona.setNombre(request.getNombre().trim());
        persona.setApellido(request.getApellido().trim());
        persona.setIdentificador(request.getIdentificador());
        persona.setNumeroIdentificador(newDoc);
        persona.setFechaNacimiento(request.getFechaNacimiento());
        persona.setSexo(request.getSexo());
        persona = personaRepository.save(persona);

        user.setPersona(persona);
        user.setEmail(newEmail);
        user.setTelefono(request.getTelefono().trim());
        user.setRole(request.getRole());
        user.setPerfilCompleto(true);

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            if (request.getPassword().trim().length() < 6) {
                throw new IllegalArgumentException("La nueva contraseña debe tener al menos 6 caracteres");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        }

        user = userRepository.save(user);
        return authService.mapearUsuarioResponse(user);
    }

    @Transactional
    @CacheEvict(value = "clientes", allEntries = true)
    public void eliminarCliente(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        String currentEmail = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;
        if (currentEmail != null && user.getEmail().equalsIgnoreCase(currentEmail)) {
            throw new IllegalArgumentException("No podés eliminar tu propia cuenta de administrador en uso");
        }

        Persona persona = user.getPersona();
        if (persona != null && persona.getId() != null) {
            Integer personaId = persona.getId();
            if (reservaRepository.existsByPersonaId(personaId)) {
                throw new IllegalStateException("No se puede eliminar el usuario porque tiene reservas registradas en el sistema.");
            }
            if (asignacionRepository.existsByEmpleadoId(personaId)) {
                throw new IllegalStateException("No se puede eliminar el usuario porque tiene asignaciones de vuelo registradas.");
            }

            carritoRepository.findByPersonaId(personaId).ifPresent(carritoRepository::delete);

            List<Favorito> favoritos = favoritoRepository.findByPersonaId(personaId);
            if (!favoritos.isEmpty()) {
                favoritoRepository.deleteAll(favoritos);
            }
        }

        userRepository.delete(user);
        if (persona != null && persona.getId() != null) {
            personaRepository.delete(persona);
        }
    }
}
