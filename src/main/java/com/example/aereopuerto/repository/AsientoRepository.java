package com.example.aereopuerto.repository;

import com.example.aereopuerto.model.Asiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AsientoRepository extends JpaRepository<Asiento, Integer> {
    List<Asiento> findByAvionId(Integer avionId);

    List<Asiento> findByAvionIdAndVueloIsNull(Integer avionId);

    List<Asiento> findByVueloId(Integer vueloId);

    @Query("SELECT a FROM Asiento a WHERE a.avion.id = :avionId AND a.vuelo IS NULL AND (a.ocupado IS NULL OR a.ocupado = false)")
    List<Asiento> findAvailableByAvionId(@Param("avionId") Integer avionId);

    @Query("SELECT a FROM Asiento a WHERE a.vuelo.id = :vueloId AND (a.ocupado IS NULL OR a.ocupado = false)")
    List<Asiento> findAvailableByVueloId(@Param("vueloId") Integer vueloId);
}
