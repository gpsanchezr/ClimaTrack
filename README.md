# ClimaTrack ❄️
### Sistema de Gestión de Mantenimiento Bajo Demanda (Estilo InDrive)

ClimaTrack es una plataforma móvil transaccional diseñada para revolucionar la industria del mantenimiento de equipos de climatización (HVAC). Inspirada en modelos de negocio bajo demanda, la aplicación conecta en tiempo real las necesidades de los clientes con la disponibilidad técnica y la supervisión administrativa, garantizando trazabilidad, seguridad y eficiencia operativa.

---

## ⚡ Últimas Actualizaciones de Arquitectura (Novedades)

### 1. 🗺️ Migración de Mapas a CARTO Voyager y Gestión Avanzada de Caché
- **Soporte Authenticated CDN:** Actualización de la configuración centralizada en `MapConfig.kt` para integrar las teselas rasterizadas oficiales de **CARTO Voyager** mediante una API Key autenticada (`.png?key=...`).
- **Resolución de Bloqueos 403:** Soluciona de forma definitiva el error `403 Access blocked` común en redes institucionales compartidas (ej. SENA) y elimina las marcas de agua de previsualización (`API KEY REQUIRED`).
- **Invalidación Forzada de Caché (`osmdroid_cache_v6`):** Implementación de una rutina en `MapConfig.inicializar()` que realiza un borrado profundo e incondicional de directorios de almacenamiento de teselas viejas en el dispositivo, forzando la descarga de mapas limpios de alta definición.
- **Distribución en Subdominios (`a`, `b`, `c`, `d`):** Optimización del rendimiento mediante descarga paralela en los cuatro servidores CDN de CARTO.

### 2. ⚡ Nuevo Modelo de Oferta Pública Controlada con Concurrencia en SQLite
- **Modelo de Tres Fases:**
  1. **Creación:** El Cliente registra la falla, marca y su **dirección exacta** (`direccion_exacta`). La solicitud entra a la base de datos local con estado `'PENDIENTE_ADMIN'` y sin técnico asignado.
  2. **Publicación Administrada:** El Administrador revisa el servicio y ejecuta un `UPDATE` que cambia el estado a `'PUBLICADA'`.
  3. **Competencia en Tiempo Real:** Las solicitudes en `'PUBLICADA'` se despliegan en el radar de los Técnicos.
- **Bloqueo Atómico de Concurrencia (Anti-Race Condition):** La aceptación del trabajo ejecuta una sentencia condicional atómica en SQLite:
  ```sql
  UPDATE solicitudes_servicio 
  SET estado = 'ASIGNADA', tecnico_id = ? 
  WHERE id = ? AND estado = 'PUBLICADA'
  ```
  - **Result > 0:** El técnico gana la orden de trabajo.
  - **Result == 0:** Si otro técnico fue milisegundos más rápido, la base de datos rechaza la actualización y el sistema le notifica inmediatamente mediante un Toast (*"¡Demasiado tarde! Otro técnico tomó esta solicitud"*), recargando el radar sin duplicar asignaciones.

### 3. 🎨 Carga Dinámica de Interfaz y Branding
- **Mapeo Dinámico de Recurso (`resources.getIdentifier()`):** Implementación de un cargador inteligente en `ClientConfirmationActivity.kt` que limpia la marca del equipo (minúsculas, remoción de espacios/símbolos como "LG", "Samsung", "Mabe") y obtiene el recurso drawable correspondiente en tiempo de ejecución.
- **Fidelidad Visual:** Elimina tintes planos (`imageTintList = null`) al encontrar logotipos oficiales para mostrar los colores reales del fabricante, incorporando un mecanismo *fallback* hacia el vector genérico (`ic_hvac_unit`) en caso de marcas personalizadas.

---

## 👤 Módulos y Roles del Sistema

El sistema implementa un Control de Acceso basado en Roles (RBAC) con tres perfiles diferenciados:

1. **📱 Módulo Cliente (María):**
   - **Solicitud Geolocalizada y Dirección Exacta:** Petición de servicio capturando coordenadas GPS e indicando la dirección detallada de la residencia.
   - **Confirmación de Servicio:** Interfaz en Material Design 3 con tarjetas resumidas y campo de dirección de alta precisión.
   - **Historial de Servicios y Gestión de Activos:** Trazabilidad de órdenes pasadas y hoja de vida de sus equipos.

2. **🔧 Módulo Técnico (Alex):**
   - **Radar Competitivo:** Visualización en tiempo real de servicios en estado `'PUBLICADA'` listos para ser tomados.
   - **Ejecución Técnica:** Registro de diagnósticos, repuestos utilizados, evidencias fotográficas y geolocalización de cierre.
   - **Firma Táctil:** Captura de firma digital del cliente al finalizar el mantenimiento.

3. **👑 Módulo Administrador (Giseella):**
   - **Control y Publicación:** Supervisión de solicitudes en `'PENDIENTE_ADMIN'` y habilitación para el radar general.
   - **Asignación Programada:** Capacidad de agendar fecha/hora específica a técnicos individuales con validación anti-cruces de horario.
   - **Gestión CRUD:** Control sobre usuarios, clientes, equipos e inventario de repuestos.

---

## 🏗️ Arquitectura y Tecnologías

Desarrollada bajo una filosofía **Offline-First**, garantizando que todo el flujo operativo funcione sin conexión continua mediante persistencia local.

- **Lenguaje:** Android Nativo con **Kotlin**.
- **Persistencia:** **SQLite** (`SQLiteOpenHelper`) con una arquitectura relacional de 11 tablas transaccionales y migraciones incrementales no destructivas (`onUpgrade`).
- **UI/UX:** **Material Design 3** y **ViewBinding**.
- **Mapas:** Integración con **OSMDroid** sobre motor **CARTO Voyager**.
- **Hardware y Sensores:**
  - **GPS:** **FusedLocationProviderClient** para coordenadas y Geocoder asíncrono para dirección aproximada.
  - **Cámara:** Evidencias fotográficas gestionadas de forma segura con **FileProvider**.
- **Seguridad:** Encriptación Hashing de contraseñas mediante **SHA-256**.

---

## ✨ Características Clave

- **Firma Digital (SignatureView):** Componente personalizado para la captura de trazos táctiles legales.
- **Seguridad de Archivos:** Gestión moderna de permisos y almacenamiento privado para evidencias.
- **Polling Reactivo:** Sistema de escucha automática para sincronización entre cliente, administrador y técnico.

---

## 🚀 Estado del Proyecto

El proyecto se encuentra **100% finalizado, probado y validado**, listo para demostraciones en vivo, entornos institucionales (SENA) y evaluaciones técnicas.

---
**Desarrollado por:** Giseella Patricia Sanchez Rico  
*Especialista en Desarrollo de Software*
