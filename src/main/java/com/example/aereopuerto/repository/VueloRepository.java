package com.example.aereopuerto.repository;

import com.example.aereopuerto.model.Vuelo;
import com.example.aereopuerto.model.enums.estadoVuelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VueloRepository extends JpaRepository<Vuelo, Integer>, JpaSpecificationExecutor<Vuelo> {

    @Query("""
    SELECT COUNT(v) > 0 FROM Vuelo v
    WHERE v.avion.id = :avionId
    AND v.id <> :excludeId
    AND v.estado <> com.example.aereopuerto.model.enums.estadoVuelo.CANCELADO
    AND (
        FUNCTION('TIMESTAMP', v.fechaSalida, v.horaSalida) < FUNCTION('TIMESTAMP', :fechaLlegada, :horaLlegada)
        AND
        FUNCTION('TIMESTAMP', v.fechaLlegada, v.horaLlegada) > FUNCTION('TIMESTAMP', :fechaSalida, :horaSalida)
    )
""")
    boolean existeConflictoHorario(
            @Param("avionId") Integer avionId,
            @Param("fechaSalida") LocalDate fechaSalida,
            @Param("horaSalida") LocalTime horaSalida,
            @Param("fechaLlegada") LocalDate fechaLlegada,
            @Param("horaLlegada") LocalTime horaLlegada,
            @Param("excludeId") Integer excludeId
    );

    List<Vuelo> findByEstadoNot(estadoVuelo estado);

    @Query("SELECT v FROM Vuelo v WHERE v.avion.id = :avionId AND v.estado IN :estados")
    List<Vuelo> findByAvionIdAndEstadoIn(@Param("avionId") Integer avionId, @Param("estados") List<estadoVuelo> estados);

    @Query(value = "SELECT * FROM vuelo WHERE vuelo_id = :id FOR UPDATE", nativeQuery = true)
    Optional<Vuelo> findByIdForUpdate(@Param("id") Integer id);
}
