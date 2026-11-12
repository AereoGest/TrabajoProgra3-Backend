package com.example.aereopuerto.model;

import com.example.aereopuerto.model.enums.RolEmpleado;
import com.example.aereopuerto.model.enums.estadoVuelo;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "asignacion")

public class Asignacion implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="asignacion_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "vuelo_id", nullable = false)
    private Vuelo vuelo;

    @ManyToOne
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona empleado;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RolEmpleado rolEmpleado;


    @Transient
    @JsonProperty("estadoAsignacion")
    public String getEstadoAsignacion() {

        if (vuelo == null) {
            return "ASIGNADA";
        }

        // Si el vuelo fue cancelado
        if (vuelo.getEstado() == estadoVuelo.CANCELADO) {
            return "CANCELADA";
        }

        // Si el vuelo ya llegó a destino
        LocalDateTime llegada = LocalDateTime.of(
                vuelo.getFechaLlegada(),
                vuelo.getHoraLlegada()
        );

        if (!llegada.isAfter(LocalDateTime.now())) {
            return "FINALIZADA";
        }

        return "ASIGNADA";
    }


}