-- ==============================================================================
-- AEROGEST - SCRIPT DE CARGA DE DATOS DE PRUEBA (SEED DATA)
-- Base de datos: MySQL 8.0+
-- Compatibilidad: Hibernate / Spring Boot JPA Entities
-- Contraseñas encriptadas con Spring Security BCryptPasswordEncoder (cost 10):
--   admin123    -> $2a$10$3tGmt/PxYNFYQZlxNgJQ5OCfXytVsnrF91jNuDh5.5Pn.hL.CrxV2
--   empleado123 -> $2a$10$VFoiLZKVesjGj4INLmHN2.1WdBLou1DT.JHC0vgfL6z5Z6OBgPlVS
--   cliente123  -> $2a$10$0FLaZ1E1rELjtj486lwrFeJwI7kfaQFOZGe17GHlYz9nZsm.xfP8a
-- ==============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------------------------
-- 1. PERSONAS (Perfiles personales asociados a usuarios y empleados)
-- ------------------------------------------------------------------------------
INSERT INTO persona (persona_id, nombre, apellido, fecha_nacimiento, identificador, numero_identificador, sexo) VALUES
(1, 'Administrador', 'AeroGest', '1985-05-15', 'DNI', '30111222', 'MASCULINO'),
(2, 'Carlos', 'Piloto', '1988-08-20', 'DNI', '33444555', 'MASCULINO'),
(3, 'Juan', 'Pérez', '1995-03-10', 'DNI', '39123456', 'MASCULINO'),
(4, 'María', 'González', '1992-11-25', 'DNI', '36789101', 'FEMENINO'),
(5, 'Lucía', 'Martínez', '1996-07-14', 'DNI', '40555666', 'FEMENINO'),
(6, 'Esteban', 'Copiloto', '1990-09-05', 'DNI', '35666777', 'MASCULINO');

-- ------------------------------------------------------------------------------
-- 2. USUARIOS (Cuentas del README con roles RBAC y contraseñas BCrypt)
-- ------------------------------------------------------------------------------
INSERT INTO users (id, persona_id, email, telefono, password, perfil_completo, is_verified, two_factor_enabled, role) VALUES
(1, 1, 'admin@gmail.com', '+542235001122', '$2a$10$3tGmt/PxYNFYQZlxNgJQ5OCfXytVsnrF91jNuDh5.5Pn.hL.CrxV2', 1, 1, 0, 'ROLE_ADMIN'),
(2, 2, 'empleado@gmail.com', '+542235003344', '$2a$10$VFoiLZKVesjGj4INLmHN2.1WdBLou1DT.JHC0vgfL6z5Z6OBgPlVS', 1, 1, 0, 'ROLE_EMPLEADO'),
(3, 3, 'cliente@gmail.com', '+542235005566', '$2a$10$0FLaZ1E1rELjtj486lwrFeJwI7kfaQFOZGe17GHlYz9nZsm.xfP8a', 1, 1, 0, 'ROLE_USUARIO'),
(4, 4, 'maria.gonzalez@gmail.com', '+541144332211', '$2a$10$0FLaZ1E1rELjtj486lwrFeJwI7kfaQFOZGe17GHlYz9nZsm.xfP8a', 1, 1, 0, 'ROLE_USUARIO');

-- ------------------------------------------------------------------------------
-- 3. AEROPUERTOS
-- ------------------------------------------------------------------------------
INSERT INTO aeropuerto (aeropuerto_id, nombre, codigo_iata, ciudad, pais) VALUES
(1, 'Aeroparque Jorge Newbery', 'AEP', 'Buenos Aires', 'Argentina'),
(2, 'Aeropuerto Internacional Ministro Pistarini (Ezeiza)', 'EZE', 'Buenos Aires', 'Argentina'),
(3, 'Aeropuerto Internacional Ástor Piazzolla', 'MDQ', 'Mar del Plata', 'Argentina'),
(4, 'Aeropuerto Internacional Ingeniero Aeronáutico Ambrosio Taravella', 'COR', 'Córdoba', 'Argentina'),
(5, 'Aeropuerto Internacional Teniente Luis Candelaria', 'BRC', 'San Carlos de Bariloche', 'Argentina'),
(6, 'Aeropuerto Internacional Gobernador Francisco Gabrielli (El Plumerillo)', 'MDZ', 'Mendoza', 'Argentina'),
(7, 'Aeropuerto Internacional John F. Kennedy', 'JFK', 'Nueva York', 'Estados Unidos');

-- ------------------------------------------------------------------------------
-- 4. AVIONES (Flota activa)
-- ------------------------------------------------------------------------------
INSERT INTO avion (avion_id, identificador, modelo, capacidad_pasajeros, capacidad_bodega, estado) VALUES
(1, 'LV-ARG-01', 'Boeing 737-800', 160, 44000.00, 'DISPONIBLE'),
(2, 'LV-ARG-02', 'Airbus A320neo', 150, 42000.00, 'ACTIVO'),
(3, 'LV-ARG-03', 'Embraer E190', 96, 28000.00, 'DISPONIBLE'),
(4, 'LV-ARG-04', 'Boeing 787-9 Dreamliner', 280, 85000.00, 'DISPONIBLE');

-- ------------------------------------------------------------------------------
-- 5. ASIENTOS (Distribución por clase para el avión 1 y 2)
-- ------------------------------------------------------------------------------
INSERT INTO asiento (id, codigo, precio_extra, avion_id, ocupado) VALUES
-- Avión 1 (Boeing 737-800)
(1, '1A', 45000.00, 1, 0), -- Primera Clase
(2, '1B', 45000.00, 1, 0), -- Primera Clase
(3, '1C', 45000.00, 1, 0), -- Primera Clase
(4, '1D', 45000.00, 1, 0), -- Primera Clase
(5, '2A', 25000.00, 1, 0), -- Business
(6, '2B', 25000.00, 1, 0), -- Business
(7, '2C', 25000.00, 1, 1), -- Business (Ocupado)
(8, '2D', 25000.00, 1, 0), -- Business
(9, '3A', 0.00, 1, 0),     -- Económica
(10, '3B', 0.00, 1, 0),    -- Económica
(11, '3C', 0.00, 1, 1),    -- Económica (Ocupado)
(12, '3D', 0.00, 1, 0),    -- Económica
(13, '4A', 0.00, 1, 0),    -- Económica
(14, '4B', 0.00, 1, 0),    -- Económica
-- Avión 2 (Airbus A320neo)
(15, '1A', 40000.00, 2, 0),
(16, '1B', 40000.00, 2, 0),
(17, '2A', 20000.00, 2, 0),
(18, '3A', 0.00, 2, 0);

-- ------------------------------------------------------------------------------
-- 6. EQUIPAJE
-- ------------------------------------------------------------------------------
INSERT INTO equipaje (equipaje_id, peso, tipo, precio) VALUES
(1, 10.00, 'MANO', 0.00),
(2, 12.00, 'CABINA', 15000.00),
(3, 23.00, 'BODEGA', 28000.00);

-- ------------------------------------------------------------------------------
-- 7. ASISTENCIA AL VIAJERO
-- ------------------------------------------------------------------------------
INSERT INTO asistencia_al_viajero (asistencia_id, nombre_plan, descripcion, precio) VALUES
(1, 'Sin Asistencia', 'Cobertura no contratada', 0.00),
(2, 'Plan Nacional Básico', 'Cobertura médica y extravío de equipaje hasta $2.000.000', 12500.00),
(3, 'Plan Premium Total', 'Cobertura médica integral, cancelación y demora de vuelo', 25000.00);

-- ------------------------------------------------------------------------------
-- 8. VUELOS (Rutas nacionales e internacionales)
-- ------------------------------------------------------------------------------
INSERT INTO vuelo (vuelo_id, aeropuerto_origen_id, aeropuerto_destino_id, avion_id, fecha_salida, fecha_llegada, hora_salida, hora_llegada, estado, precio_vuelo, escala) VALUES
(1, 1, 3, 1, '2026-11-20', '2026-11-20', '08:00:00', '08:55:00', 'PROGRAMADO', 85000.00, 0),  -- AEP -> MDQ
(2, 3, 1, 1, '2026-11-20', '2026-11-20', '19:30:00', '20:25:00', 'PROGRAMADO', 85000.00, 0),  -- MDQ -> AEP
(3, 1, 5, 2, '2026-11-22', '2026-11-22', '09:15:00', '11:35:00', 'PROGRAMADO', 145000.00, 0), -- AEP -> BRC
(4, 1, 4, 3, '2026-11-23', '2026-11-23', '14:00:00', '15:20:00', 'BOARDING', 92000.00, 0),   -- AEP -> COR
(5, 4, 6, 3, '2026-11-24', '2026-11-24', '16:00:00', '17:15:00', 'PROGRAMADO', 78000.00, 0),  -- COR -> MDZ
(6, 2, 7, 4, '2026-11-25', '2026-11-26', '22:30:00', '07:15:00', 'ACTIVO', 680000.00, 0);    -- EZE -> JFK

-- ------------------------------------------------------------------------------
-- 9. ASIGNACIONES DE EMPLEADOS A VUELOS
-- ------------------------------------------------------------------------------
INSERT INTO asignacion (asignacion_id, vuelo_id, persona_id, rol_empleado) VALUES
(1, 1, 2, 'PILOTO'),
(2, 1, 6, 'COPILOTO'),
(3, 1, 5, 'AZAFATA'),
(4, 2, 2, 'PILOTO'),
(5, 3, 2, 'PILOTO');

-- ------------------------------------------------------------------------------
-- 10. CARRITOS Y CARRITO ITEMS
-- ------------------------------------------------------------------------------
INSERT INTO carrito (carrito_id, persona_id) VALUES
(1, 3), -- Carrito de Juan Pérez (cliente@gmail.com)
(2, 4); -- Carrito de María González

INSERT INTO carrito_item (carrito_item_id, carrito_id, vuelo_id, cantidad, clase_vuelo) VALUES
(1, 1, 3, 1, 'TURISTA'),
(2, 2, 5, 2, 'ECONOMICA');

-- ------------------------------------------------------------------------------
-- 11. FAVORITOS
-- ------------------------------------------------------------------------------
INSERT INTO favorito (favorito_id, persona_id, vuelo_id, fecha_agregado) VALUES
(1, 3, 1, '2026-11-05 14:30:00'),
(2, 3, 3, '2026-11-06 18:20:00'),
(3, 4, 6, '2026-11-08 10:15:00');

-- ------------------------------------------------------------------------------
-- 12. RESERVAS
-- ------------------------------------------------------------------------------
INSERT INTO reserva (reserva_id, codigo_reserva, mercado_pago_id, persona_id, valor, cantidad_pasajes, estado, fecha_reserva, fecha_check_in) VALUES
(1, 'RES-AEP-MDQ-1001', 98765432101, 3, 110000.00, 1, 'CONFIRMADO', '2026-11-08 12:00:00', '2026-11-09 10:00:00'),
(2, 'RES-AEP-BRC-1002', 98765432102, 3, 173000.00, 1, 'ACTIVO', '2026-11-09 16:45:00', NULL),
(3, 'RES-COR-MDZ-1003', NULL, 4, 93000.00, 1, 'PROCESANDO', '2026-11-10 02:30:00', NULL);

-- ------------------------------------------------------------------------------
-- 13. PASAJES
-- ------------------------------------------------------------------------------
INSERT INTO pasaje (pasaje_id, codigo_pasaje, clase, vuelo_id, asistencia_id, reserva_id, asiento_id, nombre, apellido, tipo_documento, documento, fecha_nacimiento, email) VALUES
(1, 'TKT-1001-A', 'BUSINESS', 1, 2, 1, 7, 'Juan', 'Pérez', 'DNI', '39123456', '1995-03-10', 'cliente@gmail.com'),
(2, 'TKT-1002-A', 'ECONOMICA', 3, 3, 2, 11, 'Juan', 'Pérez', 'DNI', '39123456', '1995-03-10', 'cliente@gmail.com'),
(3, 'TKT-1003-A', 'TURISTA', 5, 1, 3, NULL, 'María', 'González', 'DNI', '36789101', '1992-11-25', 'maria.gonzalez@gmail.com');

-- ------------------------------------------------------------------------------
-- 14. PASAJE - EQUIPAJE (Tabla intermedia ManyToMany)
-- ------------------------------------------------------------------------------
INSERT INTO pasaje_equipaje (pasaje_id, equipaje_id) VALUES
(1, 1), -- Pasaje 1 lleva equipaje de mano
(1, 3), -- Pasaje 1 lleva equipaje en bodega
(2, 1), -- Pasaje 2 lleva equipaje de mano
(2, 2); -- Pasaje 2 lleva equipaje en cabina

-- ------------------------------------------------------------------------------
-- 15. FACTURAS
-- ------------------------------------------------------------------------------
INSERT INTO factura (factura_id, situacion_fiscal, reserva_id, cuil, fecha_emision, metodo_pago, cuotas, total_pagado) VALUES
(1, 'Consumidor Final', 1, '20-39123456-7', '2026-11-08 12:05:00', 'MERCADOPAGO', 1, 110000.00),
(2, 'Consumidor Final', 2, '20-39123456-7', '2026-11-09 16:50:00', 'TARJETA_CREDITO', 3, 173000.00);

SET FOREIGN_KEY_CHECKS = 1;
