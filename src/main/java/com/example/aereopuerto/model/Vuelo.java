package com.example.aereopuerto.model;

import com.example.aereopuerto.model.enums.estadoVuelo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.cglib.core.Local;

import java.io.Serializable;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "vuelo")

public class Vuelo implements Serializable {

    /**
     * SerialVersionUID es para objetos serializables
     * Esto es para que redis pueda identificar el objeto y los cambios en el objeto
     * en java
     * que si bien java lo hace auto, puede fallar, asiq se define
     */
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vuelo_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "aeropuerto_origen_id", nullable = false)
    private Aeropuerto aeropuertoOrigen;

    @ManyToOne
    @JoinColumn(name = "aeropuerto_destino_id", nullable = false)
    private Aeropuerto aeropuertoDestino;

    @ManyToOne
    @JoinColumn(name = "avion_id", nullable = false)
    private Avion avion;

    @Column(nullable = false, name = "fecha_salida")
    private LocalDate fechaSalida;

    @Column(nullable = false, name = "fecha_llegada")
    private LocalDate fechaLlegada;

    @Column(nullable = false, name = "hora_salida")
    private LocalTime horaSalida;

    @Column(nullable = false, name = "hora_llegada")
    private LocalTime horaLlegada;

    @Column(nullable = false, name = "estado")
    @Enumerated(EnumType.STRING)
    private estadoVuelo estado;

    @Column(nullable = false, name = "precio_vuelo")
    private Double precioVuelo;

    private Boolean escala;

    @Column(name = "asientos_primera")
    private Integer asientosPrimera;

    @Column(name = "asientos_ejecutiva")
    private Integer asientosEjecutiva;

    @Column(name = "asientos_economica")
    private Integer asientosEconomica;

    /**
     * Capacidad efectiva del vuelo: suma de asientos por categoría cuando está configurada,
     * o capacidad total del avión como fallback para vuelos previos a esta configuración.
     */
    @JsonIgnore
    public int capacidadTotalEfectiva() {
        if (asientosPrimera != null && asientosEjecutiva != null && asientosEconomica != null) {
            return asientosPrimera + asientosEjecutiva + asientosEconomica;
        }
        return avion != null && avion.getCapacidadPasajeros() != null ? avion.getCapacidadPasajeros() : 0;
    }
}