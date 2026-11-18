package com.example.aereopuerto.model;

import com.example.aereopuerto.model.enums.ClasesVuelo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "asiento")
public class Asiento {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String codigo; // 1A, 1B, 7F, etc.

    private Double precioExtra;

    @ManyToOne
    @JoinColumn(name = "avion_id")
    private Avion avion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vuelo_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Vuelo vuelo;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", length = 20)
    private ClasesVuelo categoria;

    private Boolean ocupado;
}
