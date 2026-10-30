package com.example.aereopuerto.repository;

import com.example.aereopuerto.model.Equipaje;
import com.example.aereopuerto.model.enums.TipoEquipaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipajeRepository extends JpaRepository<Equipaje, Integer> {
    boolean existsByTipo(TipoEquipaje tipo);
    boolean existsByTipoAndIdNot(TipoEquipaje tipo, Integer id);
}
