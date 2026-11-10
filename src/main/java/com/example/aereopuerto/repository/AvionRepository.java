package com.example.aereopuerto.repository;

import com.example.aereopuerto.model.Avion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AvionRepository extends JpaRepository<Avion, Integer>, JpaSpecificationExecutor<Avion> {

    boolean existsByIdentificadorIgnoreCase(String identificador);

    boolean existsByIdentificadorIgnoreCaseAndIdNot(String identificador, Integer id);
}
