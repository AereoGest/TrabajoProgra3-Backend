package com.example.aereopuerto.Specifications;

import com.example.aereopuerto.model.Asignacion;
import com.example.aereopuerto.model.enums.RolEmpleado;
import com.example.aereopuerto.model.enums.estadoVuelo;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AsignacionSpecification {

    public static Specification<Asignacion> porEmpleadoId(Integer empleadoId) {
        return (root, query, cb) -> {
            if (empleadoId == null) return null;
            return cb.equal(root.get("empleado").get("id"), empleadoId);
        };
    }

    public static Specification<Asignacion> porVueloId(Integer vueloId) {
        return (root, query, cb) -> {
            if (vueloId == null) return null;
            return cb.equal(root.get("vuelo").get("id"), vueloId);
        };
    }

    public static Specification<Asignacion> porRolEmpleado(RolEmpleado rol) {
        return (root, query, cb) -> {
            if (rol == null) return null;
            return cb.equal(root.get("rolEmpleado"), rol);
        };
    }

    public static Specification<Asignacion> porEstadoVuelo(estadoVuelo estado) {
        return (root, query, cb) -> {
            if (estado == null) return null;
            return cb.equal(root.get("vuelo").get("estado"), estado);
        };
    }

    public static Specification<Asignacion> porFechaSalidaDesde(LocalDate fechaDesde) {
        return (root, query, cb) -> {
            if (fechaDesde == null) return null;
            return cb.greaterThanOrEqualTo(root.get("vuelo").get("fechaSalida"), fechaDesde);
        };
    }

    public static Specification<Asignacion> porFechaSalidaHasta(LocalDate fechaHasta) {
        return (root, query, cb) -> {
            if (fechaHasta == null) return null;
            return cb.lessThanOrEqualTo(root.get("vuelo").get("fechaSalida"), fechaHasta);
        };
    }

    public static Specification<Asignacion> porTexto(String texto) {
        return (root, query, cb) -> {
            if (texto == null || texto.trim().isEmpty()) return null;
            String patron = "%" + texto.trim().toLowerCase() + "%";

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.like(cb.lower(root.get("empleado").get("nombre")), patron));
            predicates.add(cb.like(cb.lower(root.get("empleado").get("apellido")), patron));
            predicates.add(cb.like(cb.lower(root.get("empleado").get("numeroIdentificador")), patron));
            predicates.add(cb.like(cb.lower(root.get("vuelo").get("aeropuertoOrigen").get("codigoIata")), patron));
            predicates.add(cb.like(cb.lower(root.get("vuelo").get("aeropuertoOrigen").get("ciudad")), patron));
            predicates.add(cb.like(cb.lower(root.get("vuelo").get("aeropuertoDestino").get("codigoIata")), patron));
            predicates.add(cb.like(cb.lower(root.get("vuelo").get("aeropuertoDestino").get("ciudad")), patron));
            predicates.add(cb.like(cb.lower(root.get("vuelo").get("avion").get("identificador")), patron));

            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}
