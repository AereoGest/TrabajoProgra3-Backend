package com.example.aereopuerto.model;

import com.example.aereopuerto.model.enums.EstadoReserva;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "reserva")

public class Reserva implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reserva_id")
    private Integer id;

    @Column(name = "codigo_reserva", unique = true, length = 20)
    private String codigoReserva;

    @Column(name = "mercado_pago_id")
    private Long mercadoPagoId;

    @ManyToOne
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(nullable = false, name= "cantidad_pasajes")
    private Integer cantidadPasajes;

    @Column(nullable = false, name = "estado", length = 20)
    @Enumerated(EnumType.STRING)
    private EstadoReserva estadoReserva;

    @Column(nullable = false, name="fecha_reserva")
    private LocalDateTime fechaReserva;

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Pasaje> pasajes = new ArrayList<>();

    public void agregarPasaje(Pasaje pasaje) {
        pasajes.add(pasaje);
        pasaje.setReserva(this);
        this.cantidadPasajes = pasajes.size();
    }


}