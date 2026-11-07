package com.example.aereopuerto.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Checkinresponsedto {

    private Integer reservaId;
    private LocalDateTime fechaCheckIn;
    private List<Pasajecheckindto> pasajes;
}
