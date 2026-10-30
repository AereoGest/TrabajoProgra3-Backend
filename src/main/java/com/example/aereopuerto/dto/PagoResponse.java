package com.example.aereopuerto.dto;

import com.example.aereopuerto.model.enums.EstadoReserva;

public record PagoResponse(String status, String statusDetail, String codigoReserva, EstadoReserva estadoReserva) {}
