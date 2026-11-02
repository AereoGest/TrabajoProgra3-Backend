package com.example.aereopuerto.service;

import com.example.aereopuerto.Exceptions.FavoritoInvalidoException;
import com.example.aereopuerto.Exceptions.PersonaInvalidaException;
import com.example.aereopuerto.Exceptions.VueloInvalidoException;
import com.example.aereopuerto.auth.service.UsuarioAutenticadoService;
import com.example.aereopuerto.dto.FavoritoDTO;
import com.example.aereopuerto.model.Favorito;
import com.example.aereopuerto.model.Persona;
import com.example.aereopuerto.model.Vuelo;
import com.example.aereopuerto.repository.FavoritoRepository;
import com.example.aereopuerto.repository.PersonaRepository;
import com.example.aereopuerto.repository.VueloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;
    private final PersonaRepository personaRepository;
    private final VueloRepository vueloRepository;
    private final UsuarioAutenticadoService usuarioAutenticadoService;


    private FavoritoDTO addFavorito(Integer personaId, Integer vueloId) {
        if (favoritoRepository.findByPersonaIdAndVueloId(personaId, vueloId).isPresent()) {
            throw new FavoritoInvalidoException("El vuelo ya se encuentra en favoritos para este usuario.");
        }

        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new PersonaInvalidaException("Usuario no encontrado con id: " + personaId));

        Vuelo vuelo = vueloRepository.findById(vueloId)
                .orElseThrow(() -> new VueloInvalidoException("Vuelo no encontrado con id: " + vueloId));

        Favorito favorito = Favorito.builder()
                .persona(persona)
                .vuelo(vuelo)
                .fechaAgregado(LocalDateTime.now())
                .build();

        return mapToDTO(favoritoRepository.save(favorito));
    }

    private void removeFavorito(Integer personaId, Integer favoritoId) {
        Favorito favorito = favoritoRepository.findById(favoritoId)
                .orElseThrow(() -> new FavoritoInvalidoException("Favorito no encontrado con id: " + favoritoId));
        if (!favorito.getPersona().getId().equals(personaId)) {
            throw new FavoritoInvalidoException("El favorito no pertenece a esta persona.");
        }

        favoritoRepository.delete(favorito);
    }

    private FavoritoDTO mapToDTO(Favorito favorito) {
        FavoritoDTO dto = new FavoritoDTO();
        dto.setId(favorito.getId());
        dto.setPersonaId(favorito.getPersona().getId());
        dto.setVueloId(favorito.getVuelo().getId());
        dto.setFechaAgregado(favorito.getFechaAgregado());
        return dto;
    }

    //@CacheEvict(value = "favoritos", key = "#result.personaId")
    public FavoritoDTO addFavoritoPorToken(String email, Integer vueloId) {
        return addFavorito(obtenerPersonaPorEmail(email).getId(), vueloId);
    }

    public List<FavoritoDTO> getFavoritosPorToken(String email) {
        Persona persona = obtenerPersonaPorEmail(email);

        return favoritoRepository.findByPersonaId(persona.getId())
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    public void removeFavoritoPorToken(String email, Integer favoritoId) {
        Persona persona = obtenerPersonaPorEmail(email);
        removeFavorito(persona.getId(), favoritoId);
    }

    private Persona obtenerPersonaPorEmail(String email) {
        return usuarioAutenticadoService.getPersonaPorEmail(email);
    }

}


