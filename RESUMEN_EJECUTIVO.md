# 📑 Memoria Técnica de Desarrollo: ClimaTrack

**Proyecto:** ClimaTrack - Gestión Técnica Bajo Demanda  
**Autor:** Giseella Patricia Sanchez Rico  
**Tecnología:** Android Nativo (Kotlin) + SQLite  
**Arquitectura:** Offline-First con Patrón Repository  

---

## 1. Concepto y Modelo de Negocio
ClimaTrack nació como una solución a la gestión dispersa de mantenimientos de aires acondicionados. Implementamos un modelo **Bajo Demanda (InDrive Style)** donde el ciclo de vida del servicio es circular: el cliente solicita, el técnico "caza" la oferta en un radar y el sistema documenta el proceso con validez legal y física.

---

## 2. Desglose Funcional y Desarrollo Técnico

### 🔐 Módulo de Seguridad y Acceso
*   **Funcionalidad:** Login multi-rol (Admin, Técnico, Cliente) y registro de usuarios.
*   **Cómo se hizo:** Implementamos un objeto `SecurityUtils` que utiliza el algoritmo **SHA-256** para hashear las contraseñas. La validación no se hace por texto plano, sino comparando hashes dentro de la base de datos SQLite para proteger la integridad del usuario.

### 📍 Módulo Cliente (Experiencia Mapa)
*   **Funcionalidad:** Solicitar servicio mediante ubicación GPS real y descripción de falla.
*   **Cómo se hizo:** Utilizamos **OSMDroid** (mapas OpenStreetMap, sin costo ni API key) sobre un `MapView`, con la fuente de teselas **CARTO Voyager** (ver `utils/MapConfig.kt`). Para la ubicación, integramos **FusedLocationProviderClient**, que consulta el hardware del sensor GPS. La solicitud se guarda en una tabla transaccional llamada `solicitudes_servicio` con estado `PENDIENTE`.

### 📡 Módulo Técnico (Radar de Servicios)
*   **Funcionalidad:** Panel donde el técnico activa su disponibilidad y ve trabajos cercanos.
*   **Cómo se hizo:** Creamos un `MaterialSwitch` que activa la visibilidad de un `RecyclerView`. Este componente utiliza un `RadarAdapter` que consulta en tiempo real (polling cada 3 seg) la tabla de solicitudes buscando estados pendientes. Al aceptar, se ejecuta un `UPDATE` en SQL vinculando el `tecnico_id`.

### 🛠️ Módulo de Mantenimiento y Repuestos
*   **Funcionalidad:** Registro de diagnóstico, trabajo realizado y materiales usados.
*   **Cómo se hizo:** Diseñamos un formulario con `TextInputLayout` para validaciones en tiempo real (evita campos vacíos). Para los repuestos, creamos una tabla transaccional `detalle_repuestos` que vincula el catálogo de materiales con el ID único del mantenimiento actual.

### 📷 Módulo de Evidencias y Firma (Cierre Legal)
*   **Funcionalidad:** Captura de fotos y firma táctil del cliente.
*   **Cómo se hizo:** 
    *   **Cámara:** Usamos la API moderna `ActivityResultContracts.TakePicture()` junto con un **FileProvider** para generar rutas de archivos seguras en el almacenamiento privado.
    *   **Firma:** Desarrollamos un componente personalizado (`SignatureView`) basado en un `Canvas` de Android para capturar los trazos del dedo sobre la pantalla y verificar que el área no esté vacía antes de guardar.

---

## 3. Ingeniería de Datos (SQLite)
Diseñamos un esquema relacional de **11 tablas** que garantiza la trazabilidad total:
1.  **usuarios:** Roles y credenciales.
2.  **clientes / equipos:** Base de activos.
3.  **solicitudes:** El puente entre el mapa y el técnico.
4.  **ordenes / mantenimientos:** La ejecución técnica.
5.  **repuestos / detalle_repuestos:** Consumo de materiales.
6.  **evidencias / ubicaciones / aprobaciones:** Respaldo de hardware (Foto, GPS, Firma).

---

## 4. Estrategia "Offline-First"
El mayor reto técnico fue la estabilidad sin internet.
*   **Solución:** La aplicación no utiliza APIs REST externas. Toda la lógica de "mensajería" entre cliente y técnico ocurre dentro del mismo archivo de base de datos `.db` del dispositivo.
*   **Blindaje:** Usamos bloques `try-catch` en los sensores (GPS/Cámara) para que, si el hardware no responde o no hay señal de red para el mapa, la app asigne valores de "Fallback" y permita al técnico terminar su trabajo sin que la aplicación se cierre (crashee).

---

## 5. Conclusión del Prototipo
ClimaTrack no es solo una interfaz; es un sistema transaccional completo que cumple con los estándares de **Android 14 (API 34)** y demuestra el dominio de componentes complejos de la plataforma Android.

---
**Giseella Patricia Sanchez Rico**  
*Líder de Desarrollo*
