package com.example.aereopuerto.Specifications;

import com.example.aereopuerto.model.Vuelo;
import com.example.aereopuerto.model.enums.estadoVuelo;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class VueloSpecification {

    public static Specification<Vuelo> porFechaSalidaDesde(LocalDateTime fechaSalida) {
        return (root, query, criteriaBuilder) -> {
            if (fechaSalida == null)
                return null;
            return criteriaBuilder.equal(root.get("fechaSalida"), fechaSalida);
        };
    }

    public static Specification<Vuelo> porFechaLlegadaHasta(LocalDateTime fechaLlegada) {
        return (root, query, criteriaBuilder) -> {
            if (fechaLlegada == null)
                return null;
            return criteriaBuilder.lessThanOrEqualTo(root.get("fechaLlegada"), fechaLlegada);
        };
    }

    public static Specification<Vuelo> precioMenorOIgualA(Double precioMaximo) {
        return (root, query, criteriaBuilder) -> {
            if (precioMaximo == null)
                return null;
            return criteriaBuilder.lessThanOrEqualTo(root.get("precioVuelo"), precioMaximo);
        };
    }

    public static Specification<Vuelo> porEscala(Boolean tieneEscala) {
        return (root, query, criteriaBuilder) -> {
            if (tieneEscala == null)
                return null;
            return criteriaBuilder.equal(root.get("escala"), tieneEscala);
        };
    }

    public static Specification<Vuelo> porEstado(estadoVuelo estado) {
        return (root, query, criteriaBuilder) -> {
            if (estado == null)
                return null;
            return criteriaBuilder.equal(root.get("estado"), estado);
        };
    }

    public static Specification<Vuelo> noCancelados() {
        return (root, query, cb) -> cb.notEqual(root.get("estado"), estadoVuelo.CANCELADO);
    }

    public static Specification<Vuelo> porDestinoGeneral(String destino) {
        return (root, query, criteriaBuilder) -> {
            if (destino == null || destino.trim().isEmpty())
                return null;

            String pattern = "%" + destino.toLowerCase() + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("aeropuertoDestino").get("ciudad")), pattern),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("aeropuertoDestino").get("pais")), pattern),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("aeropuertoDestino").get("codigoIata")), pattern));
        };
    }

    public static Specification<Vuelo> porOrigenGeneral(String origen) {
        return (root, query, criteriaBuilder) -> {
            if (origen == null || origen.trim().isEmpty())
                return null;

            String pattern = "%" + origen.toLowerCase() + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("aeropuertoOrigen").get("ciudad")), pattern),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("aeropuertoOrigen").get("pais")), pattern),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("aeropuertoOrigen").get("codigoIata")), pattern));
        };
    }

}