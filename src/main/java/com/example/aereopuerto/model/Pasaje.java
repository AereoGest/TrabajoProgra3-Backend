package com.example.aereopuerto.model;


import com.example.aereopuerto.model.enums.ClasesVuelo;
import com.example.aereopuerto.model.enums.Identificador;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import javax.print.attribute.standard.MediaSize;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "pasaje")
public class Pasaje implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "pasaje_id")
    private Integer id;

    @Column(name = "codigo_pasaje")
    private String codigoPasaje;

    @Enumerated(EnumType.STRING)
    @Column(name = "clase")
    private ClasesVuelo clasesVuelo;

    @ManyToOne
    @JoinColumn(name = "vuelo_id", nullable = false)
    private Vuelo vuelo;

    @ManyToMany
    @JoinTable(
            name = "pasaje_equipaje",
            joinColumns = @JoinColumn(name = "pasaje_id"),
            inverseJoinColumns = @JoinColumn(name = "equipaje_id")
    )
    private List<Equipaje> equipajes;

    @ManyToOne
    @JoinColumn(name= "asistencia_id")
    private AsistenciaAlViajero asistenciaAlViajero;

    @ManyToOne
    @JoinColumn(name = "reserva_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Reserva reserva;

    @ManyToOne
    @JoinColumn(name = "asiento_id")
    private Asiento asiento;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", length = 20)
    private Identificador tipoDocumento;

    @Column(nullable = false, length = 20)
    private String documento;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(length = 100)
    private String email;

}
