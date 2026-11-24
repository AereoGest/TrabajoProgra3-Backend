package com.example.aereopuerto.service;

import com.example.aereopuerto.dto.AeropuertoDTO;
import com.example.aereopuerto.model.Aeropuerto;
import com.example.aereopuerto.repository.AeropuertoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AeropuertoServiceTest {

    @Mock
    private AeropuertoRepository aeropuertoRepository;

    @InjectMocks
    private AeropuertoService aeropuertoService;

    private AeropuertoDTO dtoValido() {
        AeropuertoDTO dto = new AeropuertoDTO();
        dto.setNombre("Aeroparque Jorge Newbery");
        dto.setCodigoIata("aep");
        dto.setCiudad("Buenos Aires");
        dto.setPais("Argentina");
        return dto;
    }

    @Test
    @DisplayName("crearAeropuerto guarda aeropuerto con nombre válido de al menos 4 letras y sin caracteres especiales")
    void crearAeropuerto_nombreValido_guardaCorrectamente() {
        AeropuertoDTO dto = dtoValido();
        when(aeropuertoRepository.findByCodigoIata("AEP")).thenReturn(Optional.empty());
        when(aeropuertoRepository.save(any(Aeropuerto.class))).thenAnswer(inv -> {
            Aeropuerto a = inv.getArgument(0);
            a.setId(1);
            return a;
        });

        Aeropuerto creado = aeropuertoService.crearAeropuerto(dto);

        assertNotNull(creado);
        assertEquals("Aeroparque Jorge Newbery", creado.getNombre());
        assertEquals("AEP", creado.getCodigoIata());
    }

    @Test
    @DisplayName("crearAeropuerto rechaza nombres con solo números, caracteres especiales o menos de 4 letras")
    void crearAeropuerto_nombresInvalidos_lanzaExcepcion() {
        for (String nombreInvalido : new String[]{"12345", "Aero 123", "Ezeiza (EZE)", "Aero-Parque", "Abc", "A B C", "   "}) {
            AeropuertoDTO dto = dtoValido();
            dto.setNombre(nombreInvalido);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> aeropuertoService.crearAeropuerto(dto),
                    "Debería fallar para nombre: " + nombreInvalido);
            assertTrue(ex.getMessage().contains("4 letras"));
        }
        verify(aeropuertoRepository, never()).save(any());
    }

    @Test
    @DisplayName("EditarAeropuerto rechaza nombres con menos de 4 letras o con caracteres especiales/números")
    void editarAeropuerto_nombreInvalido_lanzaExcepcion() {
        Aeropuerto existente = Aeropuerto.builder().id(1).nombre("Ezeiza").codigoIata("EZE").ciudad("Buenos Aires").pais("Argentina").build();
        when(aeropuertoRepository.findById(1)).thenReturn(Optional.of(existente));

        AeropuertoDTO dto = dtoValido();
        dto.setNombre("1234");

        assertThrows(IllegalArgumentException.class, () -> aeropuertoService.EditarAeropuerto(1, dto));
        verify(aeropuertoRepository, never()).save(any());
    }
}
