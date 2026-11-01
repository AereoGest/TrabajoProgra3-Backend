package com.example.aereopuerto.auth.entity;

import com.example.aereopuerto.model.Persona;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User implements UserDetails {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String telefono;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private Boolean perfilCompleto;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(unique = true)
    private String verificationToken;

    @Column(unique = true)
    private String resetToken;

    private LocalDateTime resetTokenExpiry;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean twoFactorEnabled = false;

    private String twoFactorCode;

    private LocalDateTime twoFactorCodeExpiry;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public boolean isPerfilCompleto() {
        return role != Role.ROLE_INCOMPLETO;
    }
/*
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }


 */

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }


    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired()  { return true; }

    @Override
    public boolean isAccountNonLocked()   { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
