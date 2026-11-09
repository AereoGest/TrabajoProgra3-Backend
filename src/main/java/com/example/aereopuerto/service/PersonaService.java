package com.example.aereopuerto.service;

import com.example.aereopuerto.Exceptions.PersonaInvalidaException;
import com.example.aereopuerto.Specifications.PersonaSpecification;
import com.example.aereopuerto.auth.repository.UserRepository;
import com.example.aereopuerto.model.Persona;
import com.example.aereopuerto.model.enums.Identificador;
import com.example.aereopuerto.model.enums.Sexo;
import com.example.aereopuerto.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonaService {

    @Autowired
    private final PersonaRepository personaRepository;
    private final UserRepository userRepository;

    @Cacheable(value = "clientes", key = "#id")
    public Persona obtenerClientePorId(Integer id) {
        Persona persona = personaRepository.findById(id)
                .orElseThrow(() -> new PersonaInvalidaException("Persona no encontrado. ID: " + id));
        enriquecerEmail(persona);
        return persona;
    }

    @Cacheable(value = "clientes", key = "'todos'")
    public List<Persona> obtenerTodosLosClientes() {
        List<Persona> personas = personaRepository.findAll();
        personas.forEach(this::enriquecerEmail);
        return personas;
    }

    private void enriquecerEmail(Persona persona) {
        if (persona != null && persona.getId() != null && userRepository != null) {
            userRepository.findByPersonaId(persona.getId())
                    .ifPresent(u -> persona.setEmail(u.getEmail()));
        }
    }

    @CachePut(value = "clientes", key = "#result.id")
    @CacheEvict(value = "clientes", key = "'todos'")
    public Persona crearCliente(Persona persona) {
        return personaRepository.save(persona);
    }

    @CachePut(value = "clientes", key = "#result.id")
    @CacheEvict(value = "clientes", key = "'todos'")
    public Persona actualizarCliente(Integer id, Persona persona) {

        Persona c = personaRepository.findById(id)
                .orElseThrow(() -> new PersonaInvalidaException("Persona no encontrado. ID: " + id));

        c.setNombre(persona.getNombre());
        c.setApellido(persona.getApellido());
//        c.setMail(cliente.getMail());
        c.setIdentificador(persona.getIdentificador());
  //      c.setTelefono(cliente.getTelefono());
        c.setNumeroIdentificador(persona.getNumeroIdentificador());
  //      c.setFecha_nacimiento(cliente.getFecha_nacimiento());
        c.setSexo(persona.getSexo());

        return personaRepository.save(c);
    }

    @Caching(evict = {
            @CacheEvict(value = "clientes", key = "#id"),
            @CacheEvict(value = "clientes", key = "'todos'")
    })
    public void eliminarCliente(Integer id) {
        personaRepository.deleteById(id);
    }

    @CacheEvict(value = "clientes", key = "'todos'")
    public void invalidarListaDeClientes() {
    }

    public List<Persona> buscarPersonasConFiltros(
            String nombre,
            String apellido,
            Identificador identificador,
            String numeroIdentificador,
            Sexo sexo,
            Date fechaNacimiento) {

        Specification<Persona> spec = Specification
                .where(PersonaSpecification.porNombre(nombre))
                .and(PersonaSpecification.porApellido(apellido))
                .and(PersonaSpecification.porIdentificador(identificador))
                .and(PersonaSpecification.porNumeroIdentificador(numeroIdentificador))
                .and(PersonaSpecification.porSexo(sexo))
                .and(PersonaSpecification.porFechaNacimiento(fechaNacimiento));

        List<Persona> personas = personaRepository.findAll(spec);
        personas.forEach(this::enriquecerEmail);
        return personas;
    }

}
