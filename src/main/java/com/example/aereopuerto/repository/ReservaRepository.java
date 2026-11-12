package com.example.aereopuerto.repository;

import com.example.aereopuerto.model.Persona;
import com.example.aereopuerto.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;


@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Integer>, JpaSpecificationExecutor<Reserva> {
    List<Reserva> findByPersona(Persona persona);
    boolean existsByPersona(Persona persona);
    boolean existsByPersonaId(Integer personaId);
    boolean existsByCodigoReserva (String codigoReserva);
    Optional<Reserva> findByCodigoReservaIgnoreCase(String codigoReserva);

    /** Reservas que no están canceladas y tienen pasajes en el vuelo indicado. */
    @Query("""
            SELECT DISTINCT p.reserva FROM Pasaje p
            WHERE p.vuelo.id = :vueloId
            AND p.reserva.estadoReserva <> com.example.aereopuerto.model.enums.EstadoReserva.CANCELADA
            """)
    List<Reserva> findNoCanceladasPorVuelo(@Param("vueloId") Integer vueloId);
}