package com.example.aereopuerto.repository;

import com.example.aereopuerto.model.Pasaje;
import com.example.aereopuerto.model.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PasajeRepository extends JpaRepository<Pasaje, Integer> {
    long countByVueloId(Integer vueloId);

    boolean existsByAsientoId(Integer asientoId);
    List<Pasaje> findByReservaId(Integer reservaId);
    @Query("SELECT p.vuelo.id AS vueloId, COUNT(p) AS cnt FROM Pasaje p WHERE p.vuelo.id IN :ids GROUP BY p.vuelo.id")
    List<Object[]> countGroupedByVueloIdIn(@Param("ids") List<Integer> ids);

    @Query("""
    SELECT p.asiento.id FROM Pasaje p
    WHERE p.vuelo.id = :vueloId AND p.asiento IS NOT NULL
      AND p.reserva.estadoReserva <> :cancelada
""")
    List<Integer> findAsientosOcupadosPorVuelo(@Param("vueloId") Integer vueloId,
                                               @Param("cancelada") EstadoReserva cancelada);
}
