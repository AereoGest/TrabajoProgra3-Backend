package com.example.aereopuerto.Specifications;

import com.example.aereopuerto.auth.entity.Role;
import com.example.aereopuerto.auth.entity.User;
import com.example.aereopuerto.model.Persona;
import com.example.aereopuerto.model.enums.Identificador;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    @SuppressWarnings("unchecked")
    private static Join<User, Persona> obtenerJoinPersona(Root<User> root) {
        for (Join<User, ?> join : root.getJoins()) {
            if ("persona".equals(join.getAttribute().getName())) {
                return (Join<User, Persona>) join;
            }
        }
        return root.join("persona", JoinType.LEFT);
    }

    public static Specification<User> porRole(Role role) {
        return (root, query, cb) -> {
            if (role == null) return null;
            return cb.equal(root.get("role"), role);
        };
    }

    public static Specification<User> porRoles(List<Role> roles) {
        return (root, query, cb) -> {
            if (roles == null || roles.isEmpty()) return null;
            return root.get("role").in(roles);
        };
    }

    public static Specification<User> porIdentificador(Identificador identificador) {
        return (root, query, cb) -> {
            if (identificador == null) return null;
            Join<User, Persona> persona = obtenerJoinPersona(root);
            return cb.equal(persona.get("identificador"), identificador);
        };
    }

    public static Specification<User> porNumeroIdentificador(String numeroIdentificador) {
        return (root, query, cb) -> {
            if (numeroIdentificador == null || numeroIdentificador.trim().isEmpty()) return null;
            Join<User, Persona> persona = obtenerJoinPersona(root);
            String patron = "%" + numeroIdentificador.trim().toLowerCase() + "%";
            return cb.like(cb.lower(persona.get("numeroIdentificador")), patron);
        };
    }

    public static Specification<User> porTexto(String texto) {
        return (root, query, cb) -> {
            if (texto == null || texto.trim().isEmpty()) return null;
            String patron = "%" + texto.trim().toLowerCase() + "%";

            Join<User, Persona> persona = obtenerJoinPersona(root);

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.like(cb.lower(persona.get("nombre")), patron));
            predicates.add(cb.like(cb.lower(persona.get("apellido")), patron));
            predicates.add(cb.like(cb.lower(persona.get("numeroIdentificador")), patron));
            predicates.add(cb.like(cb.lower(root.get("email")), patron));
            predicates.add(cb.like(cb.lower(root.get("telefono")), patron));

            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}
