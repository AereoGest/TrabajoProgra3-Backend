package com.example.aereopuerto.auth.service;

import com.example.aereopuerto.Exceptions.PersonaInvalidaException;
import com.example.aereopuerto.auth.entity.User;
import com.example.aereopuerto.auth.repository.UserRepository;
import com.example.aereopuerto.model.Persona;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioAutenticadoService {

    private final UserRepository userRepository;

    public User getUsuarioPorEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new PersonaInvalidaException("Usuario no encontrado para el token provisto."));
    }

    public Persona getPersonaPorEmail(String email) {
        return getPersona(getUsuarioPorEmail(email));
    }

    public Persona getPersona(User user) {
        Persona persona = user.getPersona();
        if (persona == null) {
            throw new PersonaInvalidaException("El usuario no tiene una persona asociada.");
        }
        return persona;
    }

    public void validarCuentaVerificada(User user, String accion) {
        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            throw new IllegalArgumentException(
                    "Debes verificar tu cuenta desde tu correo electrónico para poder " + accion + ".");
        }
    }
}
