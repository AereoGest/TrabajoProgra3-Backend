package com.example.aereopuerto.auth.repository;

import com.example.aereopuerto.auth.entity.Role;
import com.example.aereopuerto.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByVerificationToken(String token);
    Optional<User> findByResetToken(String token);
    Optional<User> findByPersonaId(Integer personaId);
    List<User> findByRoleIn(List<Role> roles);
}
