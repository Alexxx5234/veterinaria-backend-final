package com.veterinaria.veterinaria.controller;
// ==========================================================
// IMPORTACIONES GENERALES
// ==========================================================

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.Operation;
// ==========================================================
// MODELOS
// ==========================================================

import com.veterinaria.veterinaria.model.Cita;
import com.veterinaria.veterinaria.model.Mascota;
import com.veterinaria.veterinaria.model.Servicio;
import com.veterinaria.veterinaria.model.Usuario;
import com.veterinaria.veterinaria.model.Veterinario;
// ==========================================================
// REPOSITORIOS
// ==========================================================

import com.veterinaria.veterinaria.repository.UsuarioRepository;
// ==========================================================
// SERVICIOS
// ==========================================================

import com.veterinaria.veterinaria.service.CitaService;
import com.veterinaria.veterinaria.service.MascotaService;
import com.veterinaria.veterinaria.service.ServicioService;
import com.veterinaria.veterinaria.service.VeterinarioService;
// ==========================================================
// EXCEPCIONES Y RESPUESTAS
// ==========================================================

import com.veterinaria.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.veterinaria.exception.MensajeRespuesta;
// ==========================================================
// CONTROLADOR DE CITAS
// ==========================================================
//
// ADMIN:
// Puede gestionar todas las citas.
//
// USER:
// Puede gestionar las citas de sus propias mascotas.
//
// VETERINARIO:
// Puede consultar y atender únicamente las citas
// asignadas a su registro profesional.
//
// NUEVO: puede cancelar CONFIRMADAS y reactivar CANCELADAS.
//
// ==========================================================

@RestController
@RequestMapping("/api/citas")
public class CitaController {
    // ======================================================
    // DEPENDENCIAS
    // ======================================================
    private final CitaService citaService;
    private final MascotaService mascotaService;
    private final VeterinarioService veterinarioService;
    private final ServicioService servicioService;
    private final UsuarioRepository usuarioRepository;
    // ======================================================
    // CONSTRUCTOR ORIGINAL
    // ======================================================
    public CitaController(
            CitaService citaService,
            MascotaService mascotaService,
            VeterinarioService veterinarioService,
            ServicioService servicioService,
            UsuarioRepository usuarioRepository
    ) {
        this.citaService = citaService;
        this.mascotaService = mascotaService;
        this.veterinarioService = veterinarioService;
        this.servicioService = servicioService;
        this.usuarioRepository = usuarioRepository;
    }

    // ======================================================
    // LISTAR TODAS LAS CITAS
    // ======================================================
    //
    // GET /api/citas
    //
    // ORIGINAL:
    // Únicamente ADMIN puede consultar todas las citas.
    //
    // ======================================================
    @Operation(
        summary = "Listar todas las citas",
        description = "Obtiene todas las citas registradas. Disponible únicamente para administradores."
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Cita> listarTodos() {
        return citaService.listarTodos();
    }

    // ======================================================
    // LISTAR MIS CITAS COMO CLIENTE
    // ======================================================
    //
    // GET /api/citas/mis-citas
    //
    // ORIGINAL:
    //
    // USER consulta las citas de sus propias mascotas.
    //
    // ADMIN conserva el permiso original.
    //
    // ======================================================
    @Operation(
        summary = "Listar mis citas",
        description = "Obtiene únicamente las citas pertenecientes al usuario autenticado."
    )
    @GetMapping("/mis-citas")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public List<Cita> listarMisCitas(
            Authentication authentication
    ) {
        Usuario usuario = obtenerUsuarioAutenticado(
                authentication
        );
        return citaService.listarPorUsuario(
                usuario.getId()
        );
    }

    // ======================================================
    // NUEVO: LISTAR MIS CITAS COMO VETERINARIO
    // ======================================================
    //
    // GET /api/citas/mis-citas-veterinario
    //
    // SOLO VETERINARIO.
    //
    // 1. Identifica la cuenta autenticada.
    // 2. Busca su registro profesional vinculado.
    // 3. Obtiene solamente las citas asignadas.
    //
    // No recibe un veterinarioId desde Angular.
    //
    // Esto impide que el veterinario consulte citas
    // ajenas cambiando un ID en la petición.
    //
    // @Transactional mantiene disponible la relación
    // de Usuario durante la consulta.
    //
    // ======================================================
    @Operation(
        summary = "Consultar mis citas como veterinario",
        description = "Obtiene únicamente las citas asignadas al veterinario autenticado."
    )
    @GetMapping("/mis-citas-veterinario")
    @PreAuthorize("hasRole('VETERINARIO')")
    @Transactional(readOnly = true)
    public List<Cita> listarCitasVeterinario(
            Authentication authentication
    ) {
        // Identificar al veterinario autenticado.
        Veterinario veterinario =
                obtenerVeterinarioAutenticado(authentication);
        // Filtrar las citas por el ID del veterinario.
        return citaService.listarTodos()
                .stream()
                .filter(cita ->
                        cita.getVeterinario() != null
                        &&
                        veterinario.getId().equals(
                                cita.getVeterinario().getId()
                        )
                )
                .toList();
    }

    // ======================================================
    // CONSULTAR CITA POR ID
    // ======================================================
    //
    // GET /api/citas/{id}
    //
    // USER:
    // Solo puede consultar sus propias citas.
    //
    // ADMIN:
    // Puede consultar cualquier cita.
    //
    // VETERINARIO:
    // NUEVO: puede consultar una cita asignada a él.
    //
    // ======================================================
    @Operation(
        summary = "Consultar cita por ID",
        description = "Consulta una cita comprobando los permisos del usuario autenticado."
    )
    @GetMapping("/{id}")
    @PreAuthorize(
        "hasAnyRole('USER','ADMIN','VETERINARIO')"
    )
    @Transactional(readOnly = true)
    public ResponseEntity<Cita> buscarPorId(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Cita cita = citaService.buscarPorId(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Cita no encontrada con id: " + id
                    )
                );
        // Comprueba el acceso según el rol.
        validarAccesoCita(
                cita,
                authentication
        );
        return ResponseEntity.ok(cita);
    }

    // ======================================================
    // REGISTRAR NUEVA CITA
    // ======================================================
    //
    // POST /api/citas
    //
    // ORIGINAL:
    //
    // USER:
    // Solamente puede crear citas para sus mascotas.
    //
    // ADMIN:
    // Puede crear citas para cualquier mascota.
    //
    // Se validan mascota, veterinario y servicio.
    //
    // VETERINARIO:
    // No puede utilizar este endpoint.
    //
    // ======================================================
    @Operation(
        summary = "Registrar nueva cita",
        description = "Permite registrar una cita seleccionando una mascota, un veterinario y un servicio."
    )
    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Cita crear(
            @RequestBody Cita cita,
            Authentication authentication
    ) {
        // ==================================================
        // 1. VALIDAR MASCOTA
        // ==================================================
        if (
                cita.getMascota() == null
                ||
                cita.getMascota().getId() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe seleccionar una mascota"
            );
        }
        Long mascotaId = cita.getMascota().getId();
        Mascota mascota;
        // ==================================================
        // 2. VALIDAR PROPIETARIO DE LA MASCOTA
        // ==================================================
        if (esAdmin(authentication)) {
            // ADMIN puede utilizar cualquier mascota.
            mascota = mascotaService.buscarPorId(mascotaId)
                    .orElseThrow(() ->
                        new ResourceNotFoundException(
                            "Mascota no encontrada con id: "
                            + mascotaId
                        )
                    );
        } else {
            // USER solo puede utilizar sus mascotas.
            Usuario usuario = obtenerUsuarioAutenticado(
                    authentication
            );
            mascota = mascotaService.buscarPorIdYUsuario(
                    mascotaId,
                    usuario.getId()
            )
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "La mascota seleccionada no pertenece al usuario autenticado"
                )
            );
        }
        // ==================================================
        // 3. VALIDAR VETERINARIO
        // ==================================================
        if (
                cita.getVeterinario() == null
                ||
                cita.getVeterinario().getId() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe seleccionar un veterinario"
            );
        }
        Long veterinarioId =
                cita.getVeterinario().getId();
        Veterinario veterinario =
                veterinarioService.buscarPorId(veterinarioId)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Veterinario no encontrado con id: "
                        + veterinarioId
                    )
                );
        // ==================================================
        // 4. VALIDAR SERVICIO
        // ==================================================
        if (
                cita.getServicio() == null
                ||
                cita.getServicio().getId() == null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe seleccionar un servicio"
            );
        }
        Long servicioId = cita.getServicio().getId();
        Servicio servicio =
                servicioService.buscarPorId(servicioId)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Servicio no encontrado con id: "
                        + servicioId
                    )
                );
        // ==================================================
        // 5. ASIGNAR OBJETOS COMPLETOS
        // ==================================================
        cita.setMascota(mascota);
        cita.setVeterinario(veterinario);
        cita.setServicio(servicio);
        // ==================================================
        // 6. GENERAR ID AUTOMÁTICAMENTE
        // ==================================================
        cita.setId(null);
        // ==================================================
        // 7. ASIGNAR ESTADO INICIAL
        // ==================================================
        //
        // Se conserva la lógica original.
        //
        // Si no se envía estado, queda PENDIENTE.
        //
        // ==================================================
        if (
                cita.getEstado() == null
                ||
                cita.getEstado().isBlank()
        ) {
            cita.setEstado("PENDIENTE");
        }
        // ==================================================
        // 8. GUARDAR CITA
        // ==================================================
        return citaService.guardar(cita);
    }

    // ======================================================
    // ACTUALIZAR CITA
    // ======================================================
    //
    // PUT /api/citas/{id}
    //
    // ORIGINAL:
    //
    // USER:
    // Puede modificar únicamente sus propias citas.
    //
    // ADMIN:
    // Puede modificar cualquier cita.
    //
    // VETERINARIO:
    // No puede usar esta ruta para modificar libremente.
    //
    // ======================================================
    @Operation(
        summary = "Actualizar cita",
        description = "El usuario puede modificar únicamente sus propias citas. El administrador puede modificar cualquier cita."
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<Cita> actualizar(
            @PathVariable Long id,
            @RequestBody Cita datosCita,
            Authentication authentication
    ) {
        // ==================================================
        // 1. BUSCAR CITA EXISTENTE
        // ==================================================
        Cita citaExistente = citaService.buscarPorId(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Cita no encontrada con id: " + id
                    )
                );
        // ==================================================
        // 2. VALIDAR ACCESO A LA CITA
        // ==================================================
        validarAccesoCita(
                citaExistente,
                authentication
        );
        // ==================================================
        // 3. ACTUALIZAR FECHA
        // ==================================================
        citaExistente.setFechaHora(
                datosCita.getFechaHora()
        );
        // ==================================================
        // 4. ACTUALIZAR MOTIVO
        // ==================================================
        citaExistente.setMotivo(
                datosCita.getMotivo()
        );
        // ==================================================
        // 5. VALIDAR NUEVA MASCOTA
        // ==================================================
        if (
                datosCita.getMascota() != null
                &&
                datosCita.getMascota().getId() != null
        ) {
            Long mascotaId =
                    datosCita.getMascota().getId();
            Mascota mascota;
            if (esAdmin(authentication)) {
                mascota = mascotaService.buscarPorId(mascotaId)
                        .orElseThrow(() ->
                            new ResourceNotFoundException(
                                "Mascota no encontrada con id: "
                                + mascotaId
                            )
                        );
            } else {
                Usuario usuario = obtenerUsuarioAutenticado(
                        authentication
                );
                mascota = mascotaService.buscarPorIdYUsuario(
                        mascotaId,
                        usuario.getId()
                )
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No puedes utilizar una mascota de otro usuario"
                    )
                );
            }
            citaExistente.setMascota(mascota);
        }
        // ==================================================
        // 6. ACTUALIZAR VETERINARIO
        // ==================================================
        if (
                datosCita.getVeterinario() != null
                &&
                datosCita.getVeterinario().getId() != null
        ) {
            Long veterinarioId =
                    datosCita.getVeterinario().getId();
            Veterinario veterinario =
                    veterinarioService.buscarPorId(veterinarioId)
                    .orElseThrow(() ->
                        new ResourceNotFoundException(
                            "Veterinario no encontrado con id: "
                            + veterinarioId
                        )
                    );
            citaExistente.setVeterinario(veterinario);
        }
        // ==================================================
        // 7. ACTUALIZAR SERVICIO
        // ==================================================
        if (
                datosCita.getServicio() != null
                &&
                datosCita.getServicio().getId() != null
        ) {
            Long servicioId =
                    datosCita.getServicio().getId();
            Servicio servicio =
                    servicioService.buscarPorId(servicioId)
                    .orElseThrow(() ->
                        new ResourceNotFoundException(
                            "Servicio no encontrado con id: "
                            + servicioId
                        )
                    );
            citaExistente.setServicio(servicio);
        }
        // ==================================================
        // 8. ACTUALIZAR ESTADO
        // ==================================================
        //
        // ORIGINAL:
        //
        // Solo ADMIN puede modificar libremente
        // el estado de una cita desde esta ruta.
        //
        // ==================================================
        if (
                esAdmin(authentication)
                &&
                datosCita.getEstado() != null
                &&
                !datosCita.getEstado().isBlank()
        ) {
            citaExistente.setEstado(
                    datosCita.getEstado()
            );
        }
        // ==================================================
        // 9. GUARDAR CAMBIOS
        // ==================================================
        Cita citaActualizada = citaService.guardar(
                citaExistente
        );
        return ResponseEntity.ok(citaActualizada);
    }

    // ======================================================
    // CANCELAR CITA
    // ======================================================
    //
    // PUT /api/citas/{id}/cancelar
    //
    // ORIGINAL:
    //
    // USER:
    // Solo puede cancelar sus propias citas.
    //
    // ADMIN:
    // Puede cancelar cualquier cita.
    //
    // VETERINARIO:
    // Puede cancelar solamente sus citas CONFIRMADAS.
    // Las citas PENDIENTES se rechazan con /{id}/rechazar.
    //
    // La cita permanece en la base de datos.
    //
    // ======================================================
    @Operation(
        summary = "Cancelar cita",
        description = "Permite cancelar una cita sin eliminarla del sistema. El veterinario solo puede cancelar sus citas confirmadas."
    )
    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('USER','ADMIN','VETERINARIO')")
    @Transactional
    public ResponseEntity<Cita> cancelar(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Cita cita = citaService.buscarPorId(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Cita no encontrada con id: " + id
                    )
                );
        validarAccesoCita(
                cita,
                authentication
        );
        // Solo el veterinario asignado puede cancelar su cita
        // CONFIRMADA. Los permisos anteriores de USER y ADMIN
        // se mantienen sin modificación.
        if (esVeterinario(authentication)
                && !"CONFIRMADA".equals(cita.getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Como veterinario solo puedes cancelar citas CONFIRMADAS"
            );
        }
        cita.setEstado("CANCELADA");
        return ResponseEntity.ok(
                citaService.guardar(cita)
        );
    }

    // ======================================================
    // NUEVO: REACTIVAR CITA CANCELADA
    // ======================================================
    //
    // PUT /api/citas/{id}/reactivar?estado=PENDIENTE
    // PUT /api/citas/{id}/reactivar?estado=CONFIRMADA
    //
    // ADMIN: puede reactivar cualquier cita cancelada.
    // VETERINARIO: solo puede reactivar sus citas asignadas.
    // USER: no tiene acceso a este endpoint.
    //
    // No se pueden reactivar citas con fecha vencida.
    // Conserva mascota, servicio, veterinario e ID originales.
    // ======================================================
    @Operation(
        summary = "Reactivar cita cancelada",
        description = "Permite al ADMIN o al veterinario asignado reactivar una cita CANCELADA a PENDIENTE o CONFIRMADA."
    )
    @PutMapping("/{id}/reactivar")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO')")
    @Transactional
    public ResponseEntity<Cita> reactivarCita(
            @PathVariable Long id,
            @RequestParam("estado") String estadoDestino,
            Authentication authentication
    ) {
        // 1. Verificar el estado al que se quiere reactivar.
        String nuevoEstado = estadoDestino.trim().toUpperCase(Locale.ROOT);
        if (!"PENDIENTE".equals(nuevoEstado)
                && !"CONFIRMADA".equals(nuevoEstado)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo puedes reactivar la cita a PENDIENTE o CONFIRMADA"
            );
        }

        // 2. Obtener la cita e identificar al usuario autenticado.
        Cita cita = citaService.buscarPorId(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Cita no encontrada con id: " + id
                    )
                );
        // Para VETERINARIO, este método valida que sea el asignado.
        validarAccesoCita(cita, authentication);

        // 3. Solo se puede reactivar una cita CANCELADA.
        if (!"CANCELADA".equals(cita.getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Solo se pueden reactivar citas en estado CANCELADA"
            );
        }

        // 4. Comprobar que la fecha de la cita no haya vencido.
        if (cita.getFechaHora() == null
                || cita.getFechaHora().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No puedes reactivar una cita con fecha pasada o sin fecha"
            );
        }

        // 5. Reactivar sin crear otro registro.
        cita.setEstado(nuevoEstado);
        return ResponseEntity.ok(citaService.guardar(cita));
    }

    // ======================================================
    // NUEVO: ACEPTAR CITA COMO VETERINARIO
    // ======================================================
    //
    // PUT /api/citas/{id}/aceptar
    //
    // SOLO VETERINARIO.
    //
    // PENDIENTE -> CONFIRMADA
    //
    // No puede aceptar citas de otros veterinarios.
    //
    // ======================================================
    @Operation(
        summary = "Aceptar cita asignada",
        description = "Permite al veterinario aceptar una cita pendiente que tenga asignada."
    )
    @PutMapping("/{id}/aceptar")
    @PreAuthorize("hasRole('VETERINARIO')")
    @Transactional
    public ResponseEntity<Cita> aceptarCita(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return cambiarEstadoComoVeterinario(
                id,
                authentication,
                "PENDIENTE",
                "CONFIRMADA"
        );
    }

    // ======================================================
    // NUEVO: RECHAZAR CITA COMO VETERINARIO
    // ======================================================
    //
    // PUT /api/citas/{id}/rechazar
    //
    // SOLO VETERINARIO.
    //
    // PENDIENTE -> CANCELADA
    //
    // Utilizamos CANCELADA para mantener compatibilidad
    // con los estados que ya maneja el sistema.
    //
    // ======================================================
    @Operation(
        summary = "Rechazar cita asignada",
        description = "Permite al veterinario rechazar una cita pendiente que tenga asignada."
    )
    @PutMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('VETERINARIO')")
    @Transactional
    public ResponseEntity<Cita> rechazarCita(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return cambiarEstadoComoVeterinario(
                id,
                authentication,
                "PENDIENTE",
                "CANCELADA"
        );
    }

    // ======================================================
    // NUEVO: COMPLETAR CITA COMO VETERINARIO
    // ======================================================
    //
    // PUT /api/citas/{id}/completar
    //
    // SOLO VETERINARIO.
    //
    // CONFIRMADA -> COMPLETADA
    //
    // Por ahora solamente cambia el estado.
    //
    // Más adelante incorporaremos el registro clínico
    // de la atención, medicamentos y tratamientos.
    //
    // ======================================================
    @Operation(
        summary = "Completar cita asignada",
        description = "Permite al veterinario marcar como completada una cita previamente confirmada."
    )
    @PutMapping("/{id}/completar")
    @PreAuthorize("hasRole('VETERINARIO')")
    @Transactional
    public ResponseEntity<Cita> completarCita(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return cambiarEstadoComoVeterinario(
                id,
                authentication,
                "CONFIRMADA",
                "COMPLETADA"
        );
    }

    // ======================================================
    // ELIMINAR CITA
    // ======================================================
    //
    // DELETE /api/citas/{id}
    //
    // ORIGINAL:
    //
    // Solo ADMIN puede eliminar una cita.
    //
    // ======================================================
    @Operation(
        summary = "Eliminar cita",
        description = "Elimina definitivamente una cita. Disponible únicamente para administradores."
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> eliminar(
            @PathVariable Long id
    ) {
        citaService.buscarPorId(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Cita no encontrada con id: " + id
                    )
                );
        citaService.eliminar(id);
        return ResponseEntity.ok(
                new MensajeRespuesta(
                        200,
                        "Cita eliminada correctamente",
                        "El registro fue eliminado del sistema"
                )
        );
    }

    // ======================================================
    // MÉTODO AUXILIAR: OBTENER USUARIO AUTENTICADO
    // ======================================================
    //
    // ORIGINAL:
    //
    // El JWT identifica al usuario mediante su correo.
    //
    // ======================================================
    private Usuario obtenerUsuarioAutenticado(
            Authentication authentication
    ) {
        String correo = authentication.getName();
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Usuario autenticado no encontrado"
                    )
                );
    }

    // ======================================================
    // MÉTODO AUXILIAR: COMPROBAR SI ES ADMIN
    // ======================================================
    //
    // ORIGINAL.
    //
    // ======================================================
    private boolean esAdmin(
            Authentication authentication
    ) {
        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                    authority.getAuthority().equals(
                            "ROLE_ADMIN"
                    )
                );
    }

    // ======================================================
    // NUEVO: COMPROBAR SI ES VETERINARIO
    // ======================================================
    private boolean esVeterinario(
            Authentication authentication
    ) {
        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                    authority.getAuthority().equals(
                            "ROLE_VETERINARIO"
                    )
                );
    }

    // ======================================================
    // NUEVO: IDENTIFICAR VETERINARIO AUTENTICADO
    // ======================================================
    //
    // 1. Obtiene el usuario del JWT.
    // 2. Busca el veterinario vinculado a esa cuenta.
    //
    // No acepta un ID enviado por el frontend.
    //
    // ======================================================
    private Veterinario obtenerVeterinarioAutenticado(
            Authentication authentication
    ) {
        Usuario usuario = obtenerUsuarioAutenticado(
                authentication
        );
        return veterinarioService.listarTodos()
                .stream()
                .filter(veterinario ->
                        veterinario.getUsuario() != null
                        &&
                        usuario.getId().equals(
                                veterinario.getUsuario().getId()
                        )
                )
                .findFirst()
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "La cuenta no tiene un veterinario vinculado"
                    )
                );
    }

    // ======================================================
    // NUEVO: VALIDAR VETERINARIO ASIGNADO A UNA CITA
    // ======================================================
    //
    // Comprueba que la cita pertenezca al profesional
    // vinculado con la cuenta autenticada.
    //
    // ======================================================
    private void validarCitaAsignadaVeterinario(
            Cita cita,
            Authentication authentication
    ) {
        Veterinario veterinario =
                obtenerVeterinarioAutenticado(authentication);
        if (
                cita.getVeterinario() == null
                ||
                !veterinario.getId().equals(
                        cita.getVeterinario().getId()
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Esta cita no está asignada al veterinario autenticado"
            );
        }
    }

    // ======================================================
    // NUEVO: CAMBIAR ESTADO COMO VETERINARIO
    // ======================================================
    //
    // Centraliza las validaciones para:
    //
    // - Aceptar una cita.
    // - Rechazar una cita.
    // - Completar una cita.
    //
    // ======================================================
    private ResponseEntity<Cita> cambiarEstadoComoVeterinario(
            Long id,
            Authentication authentication,
            String estadoEsperado,
            String nuevoEstado
    ) {
        // 1. Buscar cita.
        Cita cita = citaService.buscarPorId(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Cita no encontrada con id: " + id
                    )
                );
        // 2. Comprobar que pertenezca al veterinario.
        validarCitaAsignadaVeterinario(
                cita,
                authentication
        );
        // 3. Comprobar que esté en el estado esperado.
        if (
                !estadoEsperado.equals(cita.getEstado())
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede realizar esta acción porque la cita no está en estado "
                    + estadoEsperado
            );
        }
        // 4. Cambiar estado.
        cita.setEstado(nuevoEstado);
        // 5. Guardar.
        Cita actualizada = citaService.guardar(cita);
        return ResponseEntity.ok(actualizada);
    }

    // ======================================================
    // VALIDAR ACCESO A UNA CITA
    // ======================================================
    //
    // ADMIN:
    // Puede consultar cualquier cita.
    //
    // USER:
    // Solo puede acceder a las citas de sus mascotas.
    //
    // VETERINARIO:
    // NUEVO: puede consultar las citas que tenga asignadas.
    //
    // ======================================================
    private void validarAccesoCita(
            Cita cita,
            Authentication authentication
    ) {
        // ADMIN puede acceder a todas.
        if (esAdmin(authentication)) {
            return;
        }
        // VETERINARIO solo puede acceder a las asignadas.
        if (esVeterinario(authentication)) {
            validarCitaAsignadaVeterinario(
                    cita,
                    authentication
            );
            return;
        }
        // USER debe ser propietario de la mascota.
        Usuario usuario = obtenerUsuarioAutenticado(
                authentication
        );
        if (
                cita.getMascota() == null
                ||
                cita.getMascota().getUsuario() == null
                ||
                !cita.getMascota()
                        .getUsuario()
                        .getId()
                        .equals(usuario.getId())
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tienes permiso para acceder a esta cita"
            );
        }
    }
}
