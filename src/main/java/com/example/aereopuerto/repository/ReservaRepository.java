package com.example.aereopuerto.repository;

import com.example.aereopuerto.model.Persona;
import com.example.aereopuerto.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Integer>, JpaSpecificationExecutor<Reserva> {
    List<Reserva> findByPersona(Persona persona);
    boolean existsByCodigoReserva (String codigoReserva);
}