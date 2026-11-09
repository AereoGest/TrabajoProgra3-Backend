package com.example.aereopuerto.repository;

import com.example.aereopuerto.model.Asignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AsignacionRepository extends JpaRepository<Asignacion, Integer>, JpaSpecificationExecutor<Asignacion> {
    List<Asignacion> findByEmpleadoIdOrderByVueloFechaSalidaAscVueloHoraSalidaAsc(Integer personaId);
    List<Asignacion> findByEmpleadoId(Integer personaId);
    boolean existsByEmpleadoId(Integer personaId);
    List<Asignacion> findByVueloId(Integer vueloId);
    boolean existsByVueloIdAndEmpleadoId(Integer vueloId, Integer personaId);
    boolean existsByVueloIdAndEmpleadoIdAndIdNot(Integer vueloId, Integer personaId, Integer id);
}
