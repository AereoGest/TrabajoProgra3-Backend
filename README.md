# ✈️ AeroGest

> **Plataforma Integral de Gestión y Comercialización de Operaciones Aéreas**  
> **Universidad Tecnológica Nacional (UTN) · Facultad Regional Mar del Plata (FRMDP)**  
> **Tecnicatura Universitaria en Programación**  
> *Articulación de cátedras: Programación IV & Gestión de Desarrollo de Software* (Ciclo 2026)

---

[![Java](https://img.shields.io/badge/Java-21-orange.svg?style=flat&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen.svg?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-22-red.svg?style=flat&logo=angular)](https://angular.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-blue.svg?style=flat&logo=typescript)](https://www.typescriptlang.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-informational.svg?style=flat&logo=mysql)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-Cache-critical.svg?style=flat&logo=redis)](https://redis.io/)
[![Mercado Pago](https://img.shields.io/badge/Payments-Mercado%20Pago-blue.svg?style=flat&logo=mercadopago)](https://www.mercadopago.com.ar/developers)
[![JWT](https://img.shields.io/badge/Security-JWT%20%2B%202FA-black.svg?style=flat&logo=jsonwebtokens)](https://jwt.io/)

---

## 🌐 Acceso a la Plataforma y Repositorios

El sistema se encuentra completamente desplegado y operativo en ambiente de producción:

* 🌍 **Sitio Web en Producción:** **[https://aereogest.duckdns.org](https://aereogest.duckdns.org)**
* ⚙️ **Repositorio Backend:** **[GitHub - TrabajoProgra3-Backend](https://github.com/AereoGest/TrabajoProgra3-Backend)**
* 🎨 **Repositorio Frontend:** **[GitHub - Front-End-AeroGest](https://github.com/AereoGest/Front-End-AeroGest)**

> **Acceso Inmediato:** No requiere instalación de software local. Es accesible desde cualquier navegador moderno de escritorio o dispositivo móvil (diseño responsive).

---

## 👥 Equipo de Desarrollo y Docencia

| Integrante | Rol en el Proyecto |
|---|---|
| **Juan Manuel Fernandez** | Desarrollador Full-Stack |
| **Valentina Navarro** | Desarrolladora Full-Stack |
| **Franco Rodriguez** | Desarrollador Full-Stack |
| **Celeste Souto** | Desarrolladora Full-Stack |

* **Docente titular:** Prof. Rodrigo Soto (Programación IV)  
* **Cátedra de articulación:** Gestión de Desarrollo de Software  
* **Fecha de defensa:** Noviembre de 2026

---

## 🔑 Cuentas de Acceso de Prueba

El sistema cuenta con usuarios precargados en la base de datos para verificar cada uno de los roles y niveles de autorización:

| Rol | Correo Electrónico | Contraseña | Permisos y Alcance |
|---|---|---|---|
| 🛡️ **Administrador** | `admin@gmail.com` | `admin123` | Control total del sistema: vuelos, aviones, aeropuertos, reservas, pasajes, asignaciones, usuarios y métricas de ventas. |
| 🧑‍✈️ **Empleado** | `empleado@gmail.com` | `empleado123` | Consulta operativa de vuelos asignados (tripulación / personal de tierra) y check-in de pasajeros. |
| 🧳 **Cliente (Pasajero)** | `cliente@gmail.com` | `cliente123` | Búsqueda, reserva, selección de asientos, contratación de equipaje/asistencia, pago online y check-in. |

---

## 📖 Descripción General y Propuesta de Valor

### El Problema
Las aerolíneas medianas y pequeñas que comercializan pasajes mediante canales propios se enfrentan frecuentemente a plataformas fragmentadas: flujos de compra lentos y engorrosos, pérdida de reservas en el checkout, carga manual y errónea de datos de pasajeros y vulnerabilidades en la validación transaccional de pagos. Asimismo, el personal de tierra y tripulación suele carecer de herramientas integradas para consultar sus cronogramas de vuelos y realizar check-in operativo.

### La Solución: AeroGest
**AeroGest** centraliza el ciclo de vida completo de la comercialización y administración de vuelos comerciales:
1. **Canal de venta ágil y guiado:** Reserva de vuelos, mapa interactivo de asientos en tiempo real diferenciado por clase (Primera Clase, Clase Ejecutiva, Clase Económica), selección de equipaje y asistencia médica al viajero.
2. **Pasarela de pago segura:** Integración directa con **Mercado Pago** que verifica transacciones en el backend evitando inconsistencias o fraudes de montos.
3. **Seguridad y Confianza:** Autenticación robusta basada en **JWT**, verificación de correo electrónico obligatoria previa a la compra, y **Autenticación en Dos Factores (2FA)** vía código de un solo uso (OTP) enviado por correo electrónico.
4. **Experiencia de Usuario Personalizada:** Soporte bilingüe completo (**Español / Inglés**) con sincronización de mensajes de error del backend, selector de tonalidad (**Dark Mode / Light Mode**) y panel de configuración de usuario.
5. **Panel de Gestión Operativa Integral:** Herramientas administrativas avanzadas con baja lógica, asignación de tripulación a vuelos, control de reservas y visualización de reportes comerciales.

---

## 🎯 Módulos del Sistema por Perfil

```
                               ┌─────────────────────────────────┐
                               │       AeroGest Web Portal       │
                               │   https://aereogest.duckdns.org │
                               └────────────────┬────────────────┘
                                                │
                 ┌──────────────────────────────┼──────────────────────────────┐
                 │                              │                              │
                 ▼                              ▼                              ▼
     ┌───────────────────────┐      ┌───────────────────────┐      ┌───────────────────────┐
     │  🧳 Módulo Pasajeros  │      │  🧑‍✈️ Módulo Empleados │      │ 🛡️ Módulo Administrador│
     ├───────────────────────┤      ├───────────────────────┤      ├───────────────────────┤
     │ • Búsqueda & Filtros  │      │ • Vuelos asignados    │      │ • ABM Vuelos & Aviones│
     │ • Mapa de Asientos    │      │ • Check-in operativo  │      │ • ABM Aeropuertos     │
     │ • Carrito & Checkout  │      │ • Mi Perfil           │      │ • Gestión de Reservas │
     │ • Mercado Pago        │      │ • Configuración       │      │ • Asignación Personal │
     │ • Mis Reservas        │      └───────────────────────┘      │ • Gestión de Usuarios │
     │ • Check-in Online     │                                     │ • Reportes de Ventas  │
     │ • Favoritos           │                                     └───────────────────────┘
     │ • Settings (2FA/i18n) │
     └───────────────────────┘
```

### 1. 🧳 Módulo Pasajeros (Clientes)
* **Búsqueda y filtrado de vuelos:** Búsqueda dinámica por origen, destino, rango de fechas, ciudades y códigos de aeropuerto IATA.
* **Selección interactiva de asientos:** Mapa visual del fuselaje del avión segmentado por clases con tarifación en tiempo real y bloqueo de asientos ocupados.
* **Servicios adicionales:** Contratación personalizada de equipaje (mano, cabina, bodega) y planes de asistencia al viajero.
* **Carrito y Checkout guiado:** Resumen de compra, carga de datos de pasajeros y facturación, con integración fluida a Mercado Pago.
* **Autogestión de Reservas:** Consulta de estado, cancelación automática con reembolso habilitado si se realiza con más de 24 horas de antelación al vuelo.
* **Check-in Online:** Confirmación de pasajeros y generación de tarjeta de embarque digital.
* **Favoritos:** Marcado rápido de vuelos de interés.
* **Panel de Configuración:** Gestión de perfil, idioma (Español / English), modo visual (Oscuro / Claro) y activación de 2FA.

### 2. 🧑‍✈️ Módulo Empleados (Tripulación y Personal de Tierra)
* **Cronograma de asignaciones:** Consulta de vuelos asignados con información de aeronave, horarios de salida/arribo y aeropuertos involucrados.
* **Check-in Operativo:** Habilitación de check-in para pasajeros en mostradores de aeropuerto.

### 3. 🛡️ Módulo Administradores
* **Gestión de Vuelos:** Creación, edición, reprogramación y baja lógica de vuelos comerciales.
* **Gestión de Flota:** ABM de aeronaves, distribución de filas y capacidades por clase.
* **Gestión de Aeropuertos:** Administración del catálogo de destinos nacionales e internacionales con coordenadas y códigos IATA.
* **Gestión de Reservas y Pasajes:** Supervisión de todas las reservas de la aerolínea, modificación de estados y reasignación de pasajes.
* **Asignación Operativa:** Asignación de tripulantes de cabina y pilotos a cada vuelo programado.
* **Administración de Usuarios:** Gestión centralizada de roles (Cliente, Empleado, Administrador) y auditoría de cuentas.
* **Ventas y Finanzas:** Visualización del historial de transacciones, recaudación diaria y balance operativo.

---

## 🏗️ Arquitectura y Tecnologías

El sistema sigue una arquitectura desacoplada basada en el **Modelo C4**:

### Nivel C2: Diagrama de Contenedores

```
┌────────────────────────────────────────────────────────────────────────┐
│                              CLIENTE                                   │
│            Navegador Web (Desktop / Mobile Responsive)                 │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │ HTTPS
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        FRONTEND (SPA Container)                        │
│   • Framework: Angular 22 (Standalone Components + Signals)            │
│   • Internacionalización: Servicio custom i18n (ES / EN)               │
│   • Estilos: CSS3 modular + Dark/Light theme                           │
│   • Seguridad: Guards funcionales + Interceptor HTTP JWT               │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │ JSON / HTTPS (REST API)
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                         BACKEND (API Container)                        │
│   • Framework: Java 21 + Spring Boot 3                                 │
│   • Seguridad: Spring Security + Stateless JWT + 2FA OTP               │
│   • Lógica de Negocio: 15+ Servicios especializados                   │
│   • Persistencia: Spring Data JPA + Hibernate                          │
└──────────────┬───────────────────┬──────────────────┬──────────────────┘
               │                   │                  │
               ▼                   ▼                  ▼
    ┌──────────────────────┐┌──────────────┐┌──────────────────────┐
    │  BASE DE DATOS (SQL) ││ CACHE MEMORY ││ SERVICIOS EXTERNOS   │
    │  MySQL 8.0           ││ Redis        ││ • Mercado Pago API   │
    │  Persistencia ACID   ││ Consultas    ││ • Gmail SMTP (2FA,   │
    │  Relacional          ││ rápidas      ││   verificación y     │
    └──────────────────────┘└──────────────┘│   notificaciones)    │
                                            └──────────────────────┘
```

### Tecnologías Principales
* **Frontend:** Angular 22, TypeScript 5, RxJS, Signals, HTML5, CSS3.
* **Backend:** Java 21, Spring Boot 3, Spring Data JPA, Spring Security, Lombok.
* **Seguridad y Criptografía:** JWT (JSON Web Tokens), BCrypt Password Encoder, 2FA OTP.
* **Almacenamiento:** MySQL 8 (Datos transaccionales), Redis (Caché de alta velocidad).
* **Integraciones:** Mercado Pago SDK (Checkout Pro y API REST), JavaMailSender / Gmail API (SMTP TLS 587).
* **Infraestructura:** DuckDNS, Nginx Reverse Proxy, Docker, Linux VM.

---

## 📁 Estructura del Código Fuente

### 🎨 Frontend (`Front-End-AeroGest`)
```
src/
├── app/
│   ├── components/                # Componentes comunes (Navbar, Footer, Modales)
│   ├── guards/                    # authGuard, roleGuard, adminGuard
│   ├── interceptors/              # authInterceptor (adjunta JWT, traduce errores)
│   ├── models/                    # Interfaces TypeScript (Vuelo, Reserva, Usuario, etc.)
│   ├── pages/                     # Vistas principales (Rutas)
│   │   ├── admin-panel/           # Panel de control de administración
│   │   ├── aeropuertos-admin/      # ABM de aeropuertos
│   │   ├── aviones-admin/          # ABM de aviones y asientos
│   │   ├── asignacion-admin/       # Asignación de empleados a vuelos
│   │   ├── reservas-admin/         # Gestión global de reservas
│   │   ├── vuelos-admin/           # ABM de vuelos
│   │   ├── usuarios-admin/         # Gestión de roles de usuario
│   │   ├── ventas-admin/           # Reportes de facturación y ventas
│   │   ├── asientos/               # Selección interactiva de asientos
│   │   ├── carrito/                # Carrito de compras
│   │   ├── checkout/               # Carga de pasajeros y cobro
│   │   ├── checkin/                # Check-in online
│   │   ├── compras/                # Confirmación y voucher de compra
│   │   ├── empleados/              # Panel de asignaciones operativas
│   │   ├── favoritos/              # Listado de vuelos favoritos
│   │   ├── login/ & register/      # Autenticación y 2FA
│   │   ├── mis-reservas/           # Historial y cancelación de reservas
│   │   ├── perfil/                 # Datos personales
│   │   ├── settings/               # Idioma, tema visual y seguridad 2FA
│   │   └── vuelos/                 # Catálogo y buscador de vuelos
│   ├── pipes/                     # translate.pipe.ts (i18n dinámico)
│   └── services/                  # Servicios de comunicación con API REST
│       ├── auth.service.ts
│       ├── backend-messages.ts    # Mapeo y traducción de errores del backend
│       ├── i18n.service.ts        # Diccionarios ES/EN y señales reactivas
│       ├── theme.service.ts       # Control de modo oscuro y claro
│       └── [vuelos, reservas, mercadopago, etc.].service.ts
└── environments/                  # Variables de entorno (local / producción)
```

### ⚙️ Backend (`TrabajoProgra3-Backend`)
```
src/main/java/
└── com/aerolinea/
    ├── auth/                      # AuthService, AuthController, DTOs de login/registro
    ├── config/                    # SecurityConfig, CorsConfig, JwtConfig, RedisConfig
    ├── controller/                # Controladores REST (@RestController)
    │   ├── AdminUserController.java
    │   ├── AeropuertoController.java
    │   ├── AsignacionController.java
    │   ├── AsientoController.java
    │   ├── AvionController.java
    │   ├── CarritoController.java
    │   ├── CompraController.java
    │   ├── FacturaController.java
    │   ├── FavoritoController.java
    │   ├── MercadoPagoController.java
    │   ├── PasajeController.java
    │   ├── PersonaController.java
    │   ├── ReservaController.java
    │   └── VueloController.java
    ├── dto/                       # Data Transfer Objects para requests y responses
    ├── exceptions/                # GlobalExceptions (@ControllerAdvice) y ApiError
    ├── model/                     # Entidades JPA (@Entity)
    │   ├── Asiento.java, Avion.java, Vuelo.java, Reserva.java, Pasaje.java,
    │   ├── Usuario.java, Persona.java, Factura.java, Asignacion.java, etc.
    ├── repository/                # Interfaces Spring Data JPA
    ├── service/                   # Interfaces e implementaciones de negocio
    └── AerolineaApplication.java  # Clase principal Spring Boot
```

---

## 🔌 Catálogo de Endpoints de la API REST

### 1. Autenticación y Cuentas (`/api/auth`)
| Método | Endpoint | Descripción | Acceso |
|---|---|---|---|
| `POST` | `/api/auth/register` | Registro de nuevos clientes | Público |
| `POST` | `/api/auth/login` | Inicio de sesión con emisión de JWT | Público |
| `POST` | `/api/auth/verify-2fa` | Verificación de código OTP 2FA | Público |
| `POST` | `/api/auth/toggle-2fa` | Activar o desactivar 2FA en cuenta | Autenticado |
| `GET` | `/api/auth/verify` | Validación de cuenta por enlace de email | Público |
| `POST` | `/api/auth/forgot-password` | Envío de código para restablecer clave | Público |
| `POST` | `/api/auth/reset-password` | Restablecimiento de contraseña con token | Público |
| `GET` | `/api/auth/perfil` | Obtener datos del usuario autenticado | Autenticado |
| `PUT` | `/api/auth/perfil` | Actualizar datos del usuario | Autenticado |
| `POST` | `/api/auth/logout` | Cierre de sesión y revocación | Autenticado |

### 2. Vuelos y Disponibilidad (`/api/vuelos`)
| Método | Endpoint | Descripción | Acceso |
|---|---|---|---|
| `GET` | `/api/vuelos/disponibles` | Listado de vuelos activos con cupo | Público |
| `GET` | `/api/vuelos/disponibles/filtrar` | Filtro por origen, destino y fecha | Público |
| `GET` | `/api/vuelos/{id}` | Detalle completo de un vuelo | Público |
| `GET` | `/api/vuelos` | Listado general (activos e inactivos) | `ADMIN` |
| `POST` | `/api/vuelos` | Alta de nuevo vuelo comercial | `ADMIN` |
| `PUT` | `/api/vuelos/{id}` | Modificación de vuelo | `ADMIN` |
| `DELETE` | `/api/vuelos/{id}` | Baja lógica de vuelo | `ADMIN` |

### 3. Asientos y Aeronaves (`/api/asientos`, `/api/aviones`)
| Método | Endpoint | Descripción | Acceso |
|---|---|---|---|
| `GET` | `/api/asientos/avion/{avionId}` | Asientos y distribución por clase | Público |
| `GET` | `/api/aviones` | Listado de aeronaves de la flota | `ADMIN` |
| `POST` | `/api/aviones` | Alta de nueva aeronave | `ADMIN` |
| `PUT` | `/api/aviones/{id}` | Modificación de aeronave | `ADMIN` |
| `DELETE` | `/api/aviones/{id}` | Baja de aeronave | `ADMIN` |

### 4. Carrito de Compras (`/api/carrito`)
| Método | Endpoint | Descripción | Acceso |
|---|---|---|---|
| `GET` | `/api/carrito` | Obtener carrito del usuario activo | Autenticado |
| `POST` | `/api/carrito/items` | Agregar vuelo con asientos al carrito | Autenticado |
| `DELETE` | `/api/carrito/items/{itemId}` | Quitar ítem del carrito | Autenticado |
| `DELETE` | `/api/carrito/{carritoId}/clear`| Vaciar contenido del carrito | Autenticado |

### 5. Reservas y Check-in (`/api/reservas`)
| Método | Endpoint | Descripción | Acceso |
|---|---|---|---|
| `POST` | `/api/reservas` | Creación de reserva desde carrito | Autenticado |
| `GET` | `/api/reservas/mis-reservas` | Historial de reservas del usuario | Autenticado |
| `GET` | `/api/reservas/{id}` | Detalle de reserva con pasajes | Autenticado |
| `PUT` | `/api/reservas/{id}/cancelar` | Cancelación con regla de reembolso (24h) | Autenticado |
| `PATCH` | `/api/reservas/{id}/checkin` | Confirmación de check-in online | Autenticado |
| `GET` | `/api/reservas/filtrar` | Búsqueda global de reservas | `ADMIN` |

### 6. Pagos y Facturación (`/api/mercadopago`, `/api/facturas`)
| Método | Endpoint | Descripción | Acceso |
|---|---|---|---|
| `POST` | `/api/mercadopago/crear-preferencia`| Generación de preferencia de pago | Autenticado |
| `POST` | `/api/compras/confirmar` | Confirmación de pago y emisión de pasajes | Autenticado |
| `GET` | `/api/facturas/usuario` | Consulta de facturas del usuario | Autenticado |
| `GET` | `/api/facturas/{id}/pdf` | Descarga de comprobante de compra en PDF | Autenticado |

### 7. Administración Operativa y RRHH (`/api/admin/users`, `/api/asignacion`, `/api/aeropuertos`)
| Método | Endpoint | Descripción | Acceso |
|---|---|---|---|
| `GET` | `/api/admin/users` | Listado y filtro de usuarios registrados | `ADMIN` |
| `PUT` | `/api/admin/users/{id}/rol` | Cambio de rol (Cliente / Empleado / Admin) | `ADMIN` |
| `GET` | `/api/asignacion` | Listado de asignaciones de empleados | `ADMIN` |
| `GET` | `/api/asignacion/mis-vuelos` | Vuelos asignados al empleado autenticado | `EMPLEADO` / `ADMIN` |
| `POST` | `/api/asignacion` | Asignar empleado a un vuelo | `ADMIN` |
| `DELETE` | `/api/asignacion/{id}` | Desasignar empleado de un vuelo | `ADMIN` |
| `GET` | `/api/aeropuertos` | Catálogo de aeropuertos | Público |
| `POST` | `/api/aeropuertos` | Alta de nuevo aeropuerto | `ADMIN` |
| `PUT` | `/api/aeropuertos/{id}` | Modificación de aeropuerto | `ADMIN` |
| `DELETE` | `/api/aeropuertos/{id}` | Eliminación de aeropuerto | `ADMIN` |

---

## 📊 Matriz de Requisitos Funcionales y Estado de Implementación

El proyecto se estructuró siguiendo la metodología de priorización **MoSCoW**. De los **60 requisitos funcionales** contemplados en el alcance de la cursada, **57 se encuentran 100% implementados e integrados en producción**:

| Categoría MoSCoW | Total Requisitos | Implementados | Parciales | Sin Implementar | Postergados (TFI) |
|---|:---:|:---:|:---:|:---:|:---:|
| 🔴 **Must Have** (Críticos) | 19 | 19 (100%) | 0 | 0 | 0 |
| 🟡 **Should Have** (Importantes) | 31 | 28 (90.3%) | 2 | 1 | 0 |
| 🟢 **Could Have** (Deseables) | 10 | 10 (100%) | 0 | 0 | 0 |
| ⚪ **Won't Have** (Futuros para TFI) | 12 | 0 | 0 | 0 | 12 |
| **TOTAL** | **72** | **57** | **2** | **1** | **12** |

* Los 19 requerimientos **Must Have** (Catálogo de vuelos, reservas, carrito, checkout, pagos con Mercado Pago, datos de pasajeros, facturación, ABM de vuelos, etc.) están **100% operativos**.
* Requerimientos innovadores adicionales implementados: **RF69** (Internacionalización Español/Inglés), **RF70** (Alternancia Modo Oscuro/Claro), **RF71** (Filtro avanzado de reservas) y **RF72** (Check-in para terceros).
* Los requisitos de fidelización (programa de millas, alertas de precio por correo y galería colaborativa de fotos) quedaron formalmente postergados para la siguiente etapa de Trabajo Final Integrador (TFI).

---

## 🔒 Mecanismos de Seguridad y Resiliencia

1. **Tokens JWT:** Generación de tokens criptográficos firmados con HMAC-SHA256, con tiempo de expiración controlado y validados en cada petición mediante `JwtAuthenticationFilter`.
2. **Control de Acceso Basado en Roles (RBAC):** Restricciones granulares en el backend mediante `@PreAuthorize("hasRole('ADMIN')")` y en el frontend con `roleGuard` y `adminGuard`.
3. **Autenticación en Dos Factores (2FA):** Posibilidad de activación voluntaria desde la pantalla de configuración; solicita un código OTP de 6 dígitos enviado por correo electrónico en cada intento de inicio de sesión.
4. **Verificación de Email:** Bloqueo de confirmación de reservas hasta que el usuario confirme su casilla a través del enlace criptográfico enviado en el registro.
5. **Manejo Centralizado de Excepciones:** Controlador global `@ControllerAdvice` que unifica los formatos de error (`ApiError`) y traduce los códigos de excepción hacia el frontend (`backend-messages.ts`).

---

## 💻 Guía de Instalación y Ejecución Local

### Prerrequisitos de Entorno
* **Java Development Kit (JDK):** Versión 21 o superior
* **Node.js:** Versión 20.x o superior & **npm** 10.x+
* **Motor de Base de Datos:** MySQL 8.0+
* **Servicio de Caché:** Redis 7.0+ (opcional para desarrollo local)
* **Gestor de paquetes:** Maven 3.9+ (o utilizar el wrapper `./mvnw` incluido)

---

### 1. Puesta en Marcha del Backend (Spring Boot)

1. Clonar el repositorio del backend:
   ```bash
   git clone https://github.com/AereoGest/TrabajoProgra3-Backend.git
   cd TrabajoProgra3-Backend
   ```

2. Configurar la base de datos en `src/main/resources/application.yml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/aerogest_db?createDatabaseIfNotExist=true
       username: root
       password: tu_password
     mail:
       host: smtp.gmail.com
       port: 587
       username: tu_correo@gmail.com
       password: tu_app_password
   ```

3. Compilar y ejecutar la aplicación:
   ```bash
   # En Windows
   mvnw.cmd spring-boot:run

   # En Linux / macOS
   ./mvnw spring-boot:run
   ```
   *La API iniciará por defecto en `http://localhost:8080`.*

4. Ejecutar pruebas unitarias del backend:
   ```bash
   mvn test
   ```

---

### 2. Puesta en Marcha del Frontend (Angular)

1. Clonar el repositorio del frontend:
   ```bash
   git clone https://github.com/AereoGest/Front-End-AeroGest.git
   cd Front-End-AeroGest
   ```

2. Instalar las dependencias del proyecto:
   ```bash
   npm install
   ```

3. Iniciar el servidor de desarrollo con proxy a la API:
   ```bash
   npm start
   ```
   *El frontend quedará disponible en `http://localhost:4200` y redirigirá las llamadas `/api/*` al backend en el puerto 8080 gracias a `proxy.conf.json`.*

4. Ejecutar la suite de pruebas unitarias:
   ```bash
   npm test -- --watch=false
   ```

5. Compilar para producción:
   ```bash
   npm run build
   ```

---

## 🚀 Hoja de Ruta Futura (TFI)

Para la siguiente etapa de desarrollo en el Trabajo Final Integrador (TFI), se planifican las siguientes extensiones funcionales:
- [ ] **Programa de Millas y Puntos (AeroPoints):** Acreditación automática por vuelo completado y canje por pasajes/upgrades de asiento.
- [ ] **Alertas de Precio en Tiempo Real:** Notificaciones automáticas por correo electrónico ante descensos de tarifas en rutas favoritas.
- [ ] **Galería Comunitaria de Destinos:** Espacio para que los pasajeros suban fotos de sus destinos luego de completar un vuelo.
- [ ] **Asistente Virtual Inteligente (Chatbot):** Soporte automatizado para preguntas frecuentes y estado de reservas.

---

<div align="center">

✈️ **AeroGest** · *Universidad Tecnológica Nacional (UTN FRMDP)* · 2026  
*Desarrollado con dedicación por el equipo de Programación IV y Gestión de Desarrollo de Software*

</div>
