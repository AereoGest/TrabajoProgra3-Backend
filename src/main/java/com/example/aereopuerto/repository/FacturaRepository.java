package com.example.aereopuerto.repository;

import com.example.aereopuerto.model.Factura;
import com.example.aereopuerto.model.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FacturaRepository extends JpaRepository<Factura, Integer> {

    List<Factura> findByReservaPersona(Persona persona);

    /** Facturas emitidas en un rango [desde, hasta), de la más nueva a la más vieja. */
    List<Factura> findByFechaEmisionGreaterThanEqualAndFechaEmisionLessThanOrderByFechaEmisionDesc(
            LocalDateTime desde, LocalDateTime hasta);

    /**
     * Ventas agrupadas por día, calculadas en la base de datos.
     * Cada fila: [fecha (DATE), cantidad (COUNT), total (SUM)].
     */
    @Query(value = """
            SELECT DATE(f.fecha_emision)                AS fecha,
                   COUNT(*)                             AS cantidad,
                   COALESCE(SUM(f.total_pagado), 0)     AS total
            FROM factura f
            WHERE f.fecha_emision >= :desde
              AND f.fecha_emision <  :hasta
            GROUP BY DATE(f.fecha_emision)
            ORDER BY fecha
            """, nativeQuery = true)
    List<Object[]> ventasAgrupadasPorDia(@Param("desde") LocalDateTime desde,
                                         @Param("hasta") LocalDateTime hasta);
}