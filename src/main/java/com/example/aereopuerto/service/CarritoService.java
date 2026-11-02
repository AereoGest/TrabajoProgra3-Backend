package com.example.aereopuerto.service;

import com.example.aereopuerto.Exceptions.CarritoInvalidoException;
import com.example.aereopuerto.Exceptions.PersonaInvalidaException;
import com.example.aereopuerto.Exceptions.VueloInvalidoException;
import com.example.aereopuerto.dto.CarritoDTO;
import com.example.aereopuerto.dto.CarritoItemDTO;
import com.example.aereopuerto.model.Carrito;
import com.example.aereopuerto.model.CarritoItem;
import com.example.aereopuerto.model.Persona;
import com.example.aereopuerto.model.Vuelo;
import com.example.aereopuerto.model.enums.ClasesVuelo;
import com.example.aereopuerto.model.enums.estadoVuelo;
import com.example.aereopuerto.repository.CarritoItemRepository;
import com.example.aereopuerto.repository.CarritoRepository;
import com.example.aereopuerto.repository.PersonaRepository;
import com.example.aereopuerto.repository.VueloRepository;
import com.example.aereopuerto.auth.entity.User;
import com.example.aereopuerto.auth.service.UsuarioAutenticadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final CarritoItemRepository carritoItemRepository;
    private final PersonaRepository personaRepository;
    private final VueloRepository vueloRepository;
    private final UsuarioAutenticadoService usuarioAutenticadoService;

    public CarritoDTO getCarritoByPersonaId(Integer personaId) {
        Carrito carrito = createOrFindCarrito(personaId);
        return mapToDTO(carrito);
    }

    public CarritoItemDTO addItem(Integer personaId, Integer vueloId, Integer cantidad, ClasesVuelo clase) {
        Carrito carrito = createOrFindCarrito(personaId);

        Vuelo vuelo = vueloRepository.findById(vueloId)
                .orElseThrow(() -> new VueloInvalidoException("Vuelo no encontrado con id: " + vueloId));

        if (vuelo.getEstado() == estadoVuelo.CANCELADO) {
            throw new VueloInvalidoException("No se puede agregar un vuelo cancelado al carrito.");
        }

        if (cantidad <= 0) {
            throw new CarritoInvalidoException("La cantidad debe ser mayor a 0.");
        }

        var existing = carrito.getItems().stream()
                .filter(i -> i.getVuelo().getId().equals(vueloId) && i.getClaseVuelo() == clase)
                .findFirst();

        if (existing.isPresent()) {
            CarritoItem item = existing.get();
            item.setCantidad(item.getCantidad() + cantidad);
            return mapItemToDTO(carritoItemRepository.save(item));
        }

        CarritoItem item = CarritoItem.builder()
                .carrito(carrito)
                .vuelo(vuelo)
                .cantidad(cantidad)
                .claseVuelo(clase)
                .build();

        return mapItemToDTO(carritoItemRepository.save(item));
    }

    public CarritoItemDTO updateItemQuantity(Integer personaId, Integer itemId, Integer cantidad) {
        if (cantidad <= 0) {
            throw new CarritoInvalidoException("La cantidad debe ser mayor a 0.");
        }
        Carrito carrito = createOrFindCarrito(personaId);
        CarritoItem item = carritoItemRepository.findById(itemId)
                .orElseThrow(() -> new CarritoInvalidoException("Item del carrito no encontrado con id: " + itemId));
        if (!item.getCarrito().getPersona().getId().equals(personaId)) {
            throw new CarritoInvalidoException("El item no pertenece al usuario autenticado.");
        }
        item.setCantidad(cantidad);
        return mapItemToDTO(carritoItemRepository.save(item));
    }

    public void removeItem(Integer personaId, Integer itemId) {
        Carrito carrito = createOrFindCarrito(personaId);

        CarritoItem item = carritoItemRepository.findById(itemId)
                .orElseThrow(() -> new CarritoInvalidoException("Item del carrito no encontrado con id: " + itemId));

        if(!carrito.getId().equals(item.getCarrito().getId())) {
            throw new CarritoInvalidoException("El item " + item.getId() + " no se encuentra en el carrito.");
        }
        else if (!item.getCarrito().getPersona().getId().equals(personaId)) {
            throw new CarritoInvalidoException("El item no pertenece al usuario autenticado.");
        }
        else{
          carritoItemRepository.delete(item);    
        }
    }

    public void clearCarrito(Integer personaId, Integer carritoId) {

        Carrito carrito = carritoRepository.findById(carritoId)
                .orElseThrow(() -> new CarritoInvalidoException("Carrito no encontrado con id: " + carritoId));
       if (!carrito.getPersona().getId().equals(personaId)) {
            throw new CarritoInvalidoException("El carrito no pertenece al usuario autenticado.");
        }
        carrito.getItems().clear();
        carritoRepository.save(carrito);
    }

    private Carrito createOrFindCarrito (Integer personaId){
        return carritoRepository.findByPersonaId(personaId).orElseGet(() -> {
            Persona persona = personaRepository.findById(personaId)
                    .orElseThrow(() -> new PersonaInvalidaException("Persona no encontrado con id: " + personaId));
            Carrito newCarrito = Carrito.builder().persona(persona).build();
            return carritoRepository.save(newCarrito);
        });
        
    }

    public CarritoDTO getCarritoPorToken(String email) {
        return getCarritoByPersonaId(obtenerPersonaPorEmail(email).getId());
    }

    public CarritoItemDTO addItemPorToken(String email, Integer vueloId, Integer cantidad, ClasesVuelo clase) {
        User user = usuarioAutenticadoService.getUsuarioPorEmail(email);
        usuarioAutenticadoService.validarCuentaVerificada(user, "reservar vuelos");
        return addItem(usuarioAutenticadoService.getPersona(user).getId(), vueloId, cantidad, clase);
    }

    public CarritoItemDTO updateItemQuantityPorToken(String email, Integer itemId, Integer cantidad) {
        return updateItemQuantity(obtenerPersonaPorEmail(email).getId(), itemId, cantidad);
    }

    public void removeItemPorToken(String email, Integer itemId) {
        removeItem(obtenerPersonaPorEmail(email).getId(), itemId);
    }

    public void clearCarritoPorToken(String email, Integer carritoId) {
        clearCarrito(obtenerPersonaPorEmail(email).getId(), carritoId);
    }

    private Persona obtenerPersonaPorEmail(String email) {
        return usuarioAutenticadoService.getPersonaPorEmail(email);
    }

    private CarritoDTO mapToDTO(Carrito carrito) {
        CarritoDTO dto = new CarritoDTO();
        dto.setId(carrito.getId());
        dto.setPersonaId(carrito.getPersona().getId());
        dto.setItems(carrito.getItems().stream().map(this::mapItemToDTO).toList());
        return dto;
    }

    private CarritoItemDTO mapItemToDTO(CarritoItem item) {
        CarritoItemDTO dto = new CarritoItemDTO();
        dto.setId(item.getId());
        dto.setCarritoId(item.getCarrito().getId());
        dto.setVueloId(item.getVuelo().getId());
        dto.setCantidad(item.getCantidad());
        dto.setClaseVuelo(item.getClaseVuelo());
        return dto;
    }
}
