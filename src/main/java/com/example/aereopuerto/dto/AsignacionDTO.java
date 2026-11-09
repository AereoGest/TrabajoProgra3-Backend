package com.example.aereopuerto.dto;

import com.example.aereopuerto.model.enums.RolEmpleado;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsignacionDTO {

    @NotNull(message = "El vuelo es obligatorio")
    private Integer vueloId;

    @NotNull(message = "El empleado es obligatorio")
    private Integer empleadoId;

    @NotNull(message = "El rol del empleado es obligatorio")
    private RolEmpleado rolEmpleado;

    @JsonProperty("vuelo")
    private void unpackVuelo(Map<String, Object> vuelo) {
        if (vuelo != null && vuelo.containsKey("id") && vuelo.get("id") != null) {
            try {
                this.vueloId = Integer.valueOf(vuelo.get("id").toString());
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @JsonProperty("empleado")
    private void unpackEmpleado(Map<String, Object> empleado) {
        if (empleado != null && empleado.containsKey("id") && empleado.get("id") != null) {
            try {
                this.empleadoId = Integer.valueOf(empleado.get("id").toString());
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
