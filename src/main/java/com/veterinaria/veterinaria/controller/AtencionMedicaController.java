
package com.veterinaria.veterinaria.controller;

// ==========================================================
// IMPORTACIONES GENERALES
// ==========================================================

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// ==========================================================
// MODELOS Y SERVICIOS
// ==========================================================

import com.veterinaria.veterinaria.model.AtencionMedica;
import com.veterinaria.veterinaria.service.AtencionMedicaService;

// ==========================================================
// CONTROLADOR DE ATENCIONES MEDICAS
// ==========================================================
//
// ADMIN:
// Puede consultar las atenciones e historiales.
//
// VETERINARIO:
// Puede registrar atenciones de sus citas CONFIRMADAS.
// Puede consultar sus atenciones e historiales autorizados.
//
// USER:
// Puede consultar atenciones e historiales de sus mascotas.
//
// SEGURIDAD:
// La identidad se obtiene del JWT.
// No se permite seleccionar otro veterinario desde el JSON.
//
// MEDICAMENTOS:
// Se integraran despues con el catalogo administrado
// exclusivamente por ADMIN.
//
// ==========================================================

@RestController
@RequestMapping("/api/atenciones-medicas")
public class AtencionMedicaController {

    // ======================================================
    // DEPENDENCIA
    // ======================================================

    private final AtencionMedicaService atencionMedicaService;

    // ======================================================
    // CONSTRUCTOR
    // ======================================================

    public AtencionMedicaController(
            AtencionMedicaService atencionMedicaService
    ) {
        this.atencionMedicaService = atencionMedicaService;
    }

    // ======================================================
    // 1. REGISTRAR ATENCION MEDICA
    // ======================================================
    //
    // POST /api/atenciones-medicas
    //
    // SOLO VETERINARIO.
    //
    // Reglas verificadas por el servicio:
    //
    // - La cita debe existir.
    // - Debe estar CONFIRMADA.
    // - Debe pertenecer al veterinario autenticado.
    // - No debe tener otra atencion registrada.
    //
    // Al registrar la atencion, el servicio cambia
    // automaticamente la cita a COMPLETADA.
    //
    // ======================================================

    @Operation(
        summary = "Registrar atencion medica",
        description = "Permite al veterinario registrar el diagnostico, "
                + "tratamiento y observaciones de una cita confirmada."
    )
    @PostMapping
    @PreAuthorize("hasRole('VETERINARIO')")
    public ResponseEntity<AtencionMedicaRespuesta> registrar(
            @Valid @RequestBody RegistroAtencionRequest datos,
            Authentication authentication
    ) {

        // Obtener el correo real del usuario autenticado.
        String correo = obtenerCorreoAutenticado(authentication);

        // El servicio realiza las validaciones de permisos.
        AtencionMedica atencion =
                atencionMedicaService.registrarAtencion(
                        datos.getCitaId(),
                        datos.getDiagnostico(),
                        datos.getTratamiento(),
                        datos.getObservaciones(),
                        correo
                );

        // Devolver 201 Created con los datos registrados.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AtencionMedicaRespuesta.desde(atencion));
    }

    // ======================================================
    // 2. CONSULTAR ATENCION POR CITA
    // ======================================================
    //
    // GET /api/atenciones-medicas/cita/{citaId}
    //
    // ADMIN: cualquier cita.
    // USER: citas de sus mascotas.
    // VETERINARIO: citas asignadas a el.
    //
    // ======================================================

    @Operation(
        summary = "Consultar atencion por cita",
        description = "Obtiene la atencion medica de una cita "
                + "comprobando los permisos del usuario."
    )
    @GetMapping("/cita/{citaId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','VETERINARIO')")
    public ResponseEntity<AtencionMedicaRespuesta> buscarPorCita(
            @PathVariable Long citaId,
            Authentication authentication
    ) {

        String correo = obtenerCorreoAutenticado(authentication);

        AtencionMedica atencion =
                atencionMedicaService.buscarPorCita(
                        citaId,
                        correo
                );

        return ResponseEntity.ok(
                AtencionMedicaRespuesta.desde(atencion)
        );
    }

    // ======================================================
    // 3. HISTORIAL CLINICO DE UNA MASCOTA
    // ======================================================
    //
    // GET /api/atenciones-medicas/mascota/{mascotaId}/historial
    //
    // ADMIN: puede consultar todos los historiales.
    // USER: solo las mascotas de su propiedad.
    // VETERINARIO: mascotas con las que tiene citas.
    //
    // Las atenciones se ordenan de la mas reciente
    // a la mas antigua mediante el repositorio.
    //
    // ======================================================

    @Operation(
        summary = "Consultar historial clinico",
        description = "Obtiene las atenciones medicas de una mascota "
                + "ordenadas desde la mas reciente."
    )
    @GetMapping("/mascota/{mascotaId}/historial")
    @PreAuthorize("hasAnyRole('ADMIN','USER','VETERINARIO')")
    public ResponseEntity<List<AtencionMedicaRespuesta>> historialMascota(
            @PathVariable Long mascotaId,
            Authentication authentication
    ) {

        String correo = obtenerCorreoAutenticado(authentication);

        List<AtencionMedica> historial =
                atencionMedicaService.historialDeMascota(
                        mascotaId,
                        correo
                );

        // Convertir las entidades en respuestas seguras.
        List<AtencionMedicaRespuesta> respuesta =
                historial.stream()
                        .map(AtencionMedicaRespuesta::desde)
                        .toList();

        return ResponseEntity.ok(respuesta);
    }

    // ======================================================
    // 4. CONSULTAR MIS ATENCIONES COMO VETERINARIO
    // ======================================================
    //
    // GET /api/atenciones-medicas/mis-atenciones
    //
    // SOLO VETERINARIO.
    //
    // Devuelve las atenciones registradas en las citas
    // correspondientes al veterinario autenticado.
    //
    // ======================================================

    @Operation(
        summary = "Consultar mis atenciones medicas",
        description = "Obtiene las atenciones correspondientes "
                + "al veterinario autenticado."
    )
    @GetMapping("/mis-atenciones")
    @PreAuthorize("hasRole('VETERINARIO')")
    public ResponseEntity<List<AtencionMedicaRespuesta>> misAtenciones(
            Authentication authentication
    ) {

        String correo = obtenerCorreoAutenticado(authentication);

        List<AtencionMedica> atenciones =
                atencionMedicaService.misAtenciones(correo);

        List<AtencionMedicaRespuesta> respuesta =
                atenciones.stream()
                        .map(AtencionMedicaRespuesta::desde)
                        .toList();

        return ResponseEntity.ok(respuesta);
    }

    // ======================================================
    // NUEVO: CONSULTAR TODAS LAS ATENCIONES MEDICAS
    // ======================================================
    //
    // GET /api/atenciones-medicas
    //
    // SOLO ADMIN.
    //
    // Permite supervisar las atenciones registradas
    // por todos los veterinarios, sin modificarlas.
    //
    // ======================================================

    @Operation(
        summary = "Consultar todas las atenciones medicas",
        description = "Permite al administrador consultar todas las "
                + "atenciones medicas registradas."
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AtencionMedicaRespuesta>> listarTodas() {

        // Recuperar las atenciones desde el servicio.
        List<AtencionMedica> atenciones =
                atencionMedicaService.listarTodas();

        // Convertir las entidades a respuestas seguras.
        List<AtencionMedicaRespuesta> respuesta =
                atenciones.stream()
                        .map(AtencionMedicaRespuesta::desde)
                        .toList();

        return ResponseEntity.ok(respuesta);
    }

    // ======================================================
    // PASO 50: CONSULTAR ATENCIONES POR VETERINARIO
    // ======================================================
    //
    // GET /api/atenciones-medicas/veterinario/{veterinarioId}
    //
    // SOLO ADMIN.
    // Muestra las atenciones de un veterinario concreto.
    // La seguridad y la validacion del ID se refuerzan
    // desde AtencionMedicaService.
    //
    // ======================================================

    @Operation(
        summary = "Consultar atenciones por veterinario",
        description = "Permite al administrador consultar las atenciones "
                + "registradas por un veterinario mediante su ID."
    )
    @GetMapping("/veterinario/{veterinarioId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AtencionMedicaRespuesta>> listarPorVeterinario(
            @PathVariable Long veterinarioId
    ) {

        // El servicio valida el ID y consulta el repositorio.
        List<AtencionMedica> atenciones =
                atencionMedicaService.listarPorVeterinario(veterinarioId);

        // Utilizar el mismo DTO seguro de los demas endpoints.
        List<AtencionMedicaRespuesta> respuesta =
                atenciones.stream()
                        .map(AtencionMedicaRespuesta::desde)
                        .toList();

        return ResponseEntity.ok(respuesta);
    }

    // ======================================================
    // PASO 51: CONSULTAR ATENCIONES POR RANGO DE FECHAS
    // ======================================================
    //
    // GET /api/atenciones-medicas/fechas?desde=2026-10-01&hasta=2026-10-20
    //
    // SOLO ADMIN.
    //
    // Recibe fechas en formato yyyy-MM-dd.
    // El servicio valida el orden de las fechas e incluye
    // todas las atenciones del dia inicial y del dia final.
    // Los resultados se ordenan de mas reciente a mas antigua.
    //
    // ======================================================

    @Operation(
        summary = "Consultar atenciones medicas por rango de fechas",
        description = "Permite al administrador consultar las atenciones "
                + "registradas entre dos fechas, incluyendo ambos dias. "
                + "Utilice el formato yyyy-MM-dd."
    )
    @GetMapping("/fechas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AtencionMedicaRespuesta>> listarPorFechas(
            @RequestParam("desde")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam("hasta")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {

        // El servicio valida las fechas, los permisos
        // y recupera los registros desde el repositorio.
        List<AtencionMedica> atenciones =
                atencionMedicaService.listarPorFechas(desde, hasta);

        // Mantener el mismo DTO utilizado por las consultas anteriores.
        List<AtencionMedicaRespuesta> respuesta =
                atenciones.stream()
                        .map(AtencionMedicaRespuesta::desde)
                        .toList();

        return ResponseEntity.ok(respuesta);
    }

    // ======================================================
    // PASO 52: ESTADISTICAS GENERALES DE ATENCIONES MEDICAS
    // ======================================================
    //
    // GET /api/atenciones-medicas/estadisticas
    //
    // SOLO ADMIN.
    //
    // Muestra el total de atenciones, mascotas diferentes
    // atendidas y veterinarios con atenciones registradas.
    // Tambien incluye el numero de atenciones por veterinario.
    //
    // El Service calcula los datos sin modificar registros.
    // Se reutiliza el DTO definido en AtencionMedicaService.
    //
    // ======================================================

    @Operation(
        summary = "Consultar estadisticas de atenciones medicas",
        description = "Permite al administrador consultar el total de "
                + "atenciones, mascotas atendidas y veterinarios con "
                + "atenciones, incluyendo el resumen por veterinario."
    )
    @GetMapping("/estadisticas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AtencionMedicaService.EstadisticasAtenciones> obtenerEstadisticas() {

        // Obtener las estadisticas calculadas en el servicio.
        // El servicio tambien verifica que el rol sea ADMIN.
        AtencionMedicaService.EstadisticasAtenciones estadisticas =
                atencionMedicaService.obtenerEstadisticas();

        // Devolver los datos en formato JSON con 200 OK.
        return ResponseEntity.ok(estadisticas);
    }

    // ======================================================
    // PASO 49: HISTORIALES CLINICOS GENERALES PARA ADMIN
    // ======================================================
    //
    // GET /api/atenciones-medicas/historiales
    //
    // SOLO ADMIN.
    //
    // Reutiliza las atenciones existentes y las agrupa
    // por mascota, de la mas reciente a la mas antigua.
    // No modifica la informacion clinica ni la base de datos.
    // Solo incluye mascotas con atenciones registradas.
    //
    // ======================================================

    @Operation(
        summary = "Consultar historiales clinicos de todas las mascotas",
        description = "Permite al administrador consultar las atenciones "
                + "agrupadas por mascota y ordenadas de la mas reciente."
    )
    @GetMapping("/historiales")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<HistorialMascotaRespuesta>> listarHistoriales() {

        // La seguridad tambien se valida en listarTodas() del servicio.
        List<AtencionMedicaRespuesta> atenciones =
                atencionMedicaService.listarTodas()
                        .stream()
                        .map(AtencionMedicaRespuesta::desde)
                        .sorted(Comparator.comparing(
                                AtencionMedicaRespuesta::fechaAtencion,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        ))
                        .toList();

        // Crear un grupo por ID de mascota, conservando el orden.
        Map<Long, List<AtencionMedicaRespuesta>> agrupadas =
                atenciones.stream().collect(
                        Collectors.groupingBy(
                                AtencionMedicaRespuesta::mascotaId,
                                LinkedHashMap::new,
                                Collectors.toList()
                        )
                );

        // Cada mascota aparece una sola vez con todas sus atenciones.
        List<HistorialMascotaRespuesta> respuesta =
                agrupadas.entrySet()
                        .stream()
                        .map(grupo -> new HistorialMascotaRespuesta(
                                grupo.getKey(),
                                grupo.getValue().get(0).nombreMascota(),
                                grupo.getValue()
                        ))
                        .toList();

        return ResponseEntity.ok(respuesta);
    }

    // ======================================================
    // 5. OBTENER CORREO DEL USUARIO AUTENTICADO
    // ======================================================
    //
    // Spring Security identifica al usuario por su JWT.
    //
    // Nunca solicitamos el correo dentro del JSON,
    // porque alguien podria intentar suplantar
    // a otro usuario o veterinario.
    //
    // ======================================================

    private String obtenerCorreoAutenticado(
            Authentication authentication
    ) {

        if (
                authentication == null
                ||
                !authentication.isAuthenticated()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Debes iniciar sesion para continuar"
            );
        }

        return authentication.getName();
    }

    // ======================================================
    // 6. DTO: DATOS PARA REGISTRAR UNA ATENCION
    // ======================================================
    //
    // Este objeto representa el JSON de Swagger.
    //
    // No recibe:
    // - veterinarioId
    // - usuarioId
    // - estado de la cita
    // - medicamentos nuevos
    //
    // El veterinario se identifica por su JWT.
    // El estado lo modifica el servicio.
    //
    // ======================================================

    public static class RegistroAtencionRequest {

        // ID de la cita que sera atendida.
        @NotNull(message = "Debe seleccionar una cita")
        private Long citaId;

        // Diagnostico obligatorio.
        @NotBlank(message = "El diagnostico es obligatorio")
        @Size(
            max = 2000,
            message = "El diagnostico no puede superar 2000 caracteres"
        )
        private String diagnostico;

        // Tratamiento obligatorio.
        @NotBlank(message = "El tratamiento es obligatorio")
        @Size(
            max = 2000,
            message = "El tratamiento no puede superar 2000 caracteres"
        )
        private String tratamiento;

        // Observaciones opcionales.
        @Size(
            max = 2000,
            message = "Las observaciones no pueden superar 2000 caracteres"
        )
        private String observaciones;

        public RegistroAtencionRequest() {
        }

        public Long getCitaId() {
            return citaId;
        }

        public void setCitaId(Long citaId) {
            this.citaId = citaId;
        }

        public String getDiagnostico() {
            return diagnostico;
        }

        public void setDiagnostico(String diagnostico) {
            this.diagnostico = diagnostico;
        }

        public String getTratamiento() {
            return tratamiento;
        }

        public void setTratamiento(String tratamiento) {
            this.tratamiento = tratamiento;
        }

        public String getObservaciones() {
            return observaciones;
        }

        public void setObservaciones(String observaciones) {
            this.observaciones = observaciones;
        }
    }

    // ======================================================
    // PASO 49: RESPUESTA AGRUPADA POR MASCOTA
    // ======================================================
    //
    // Se utiliza solo para consultar los historiales generales.
    // Reutiliza el DTO de atenciones ya existente.
    //
    // ======================================================

    public record HistorialMascotaRespuesta(
            Long mascotaId,
            String nombreMascota,
            List<AtencionMedicaRespuesta> atenciones
    ) {
    }

    // ======================================================
    // 7. DTO: RESPUESTA DE ATENCION MEDICA
    // ======================================================
    //
    // Devuelve solamente los campos necesarios.
    //
    // Evita devolver entidades completas con relaciones
    // que puedan provocar ciclos JSON o exponer datos
    // innecesarios de usuarios y veterinarios.
    //
    // El ID de la cita permite relacionar esta respuesta
    // con la mascota y el veterinario correspondientes.
    //
    // ======================================================

    public record AtencionMedicaRespuesta(
            Long id,
            Long citaId,
            // PASO 48: Identificacion de la mascota y del veterinario.
            Long mascotaId,
            String nombreMascota,
            Long veterinarioId,
            String nombreVeterinario,
            String diagnostico,
            String tratamiento,
            String observaciones,
            String medicamentosUtilizados,
            String medicamentosRecetados,
            LocalDateTime fechaAtencion
    ) {

        // Convertir la entidad a una respuesta JSON.
        public static AtencionMedicaRespuesta desde(
                AtencionMedica atencion
        ) {

            return new AtencionMedicaRespuesta(
                    atencion.getId(),
                    atencion.getCita().getId(),
                    // PASO 48: Datos obtenidos de las relaciones existentes.
                    atencion.getCita().getMascota().getId(),
                    atencion.getCita().getMascota().getNombre(),
                    atencion.getCita().getVeterinario().getId(),
                    atencion.getCita().getVeterinario().getNombre(),
                    atencion.getDiagnostico(),
                    atencion.getTratamiento(),
                    atencion.getObservaciones(),
                    atencion.getMedicamentosUtilizados(),
                    atencion.getMedicamentosRecetados(),
                    atencion.getFechaAtencion()
            );
        }
    }
}
