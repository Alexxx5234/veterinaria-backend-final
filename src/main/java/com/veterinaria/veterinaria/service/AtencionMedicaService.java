
package com.veterinaria.veterinaria.service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.veterinaria.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.veterinaria.model.AtencionMedica;
import com.veterinaria.veterinaria.model.Cita;
import com.veterinaria.veterinaria.model.Mascota;
import com.veterinaria.veterinaria.model.Rol;
import com.veterinaria.veterinaria.model.Usuario;
import com.veterinaria.veterinaria.model.Veterinario;
import com.veterinaria.veterinaria.repository.AtencionMedicaRepository;
import com.veterinaria.veterinaria.repository.UsuarioRepository;

// ==========================================================
// SERVICIO DE ATENCIONES MEDICAS
// ==========================================================
// El veterinario registra la atencion de una cita CONFIRMADA.
// ADMIN y USER pueden consultar solo la informacion permitida.
// Los medicamentos NO se crean aqui: su catalogo sera
// administrado exclusivamente por ADMIN en otro modulo.
// ==========================================================

@Service
public class AtencionMedicaService {

    // ======================================================
    // DEPENDENCIAS
    // ======================================================

    private final AtencionMedicaRepository atencionRepository;
    private final CitaService citaService;
    private final MascotaService mascotaService;
    private final VeterinarioService veterinarioService;
    private final UsuarioRepository usuarioRepository;

    // ======================================================
    // CONSTRUCTOR
    // ======================================================

    public AtencionMedicaService(
            AtencionMedicaRepository atencionRepository,
            CitaService citaService,
            MascotaService mascotaService,
            VeterinarioService veterinarioService,
            UsuarioRepository usuarioRepository
    ) {
        this.atencionRepository = atencionRepository;
        this.citaService = citaService;
        this.mascotaService = mascotaService;
        this.veterinarioService = veterinarioService;
        this.usuarioRepository = usuarioRepository;
    }

    // ======================================================
    // 1. REGISTRAR UNA ATENCION MEDICA
    // ======================================================
    //
    // Solo VETERINARIO, y solo en una cita asignada a el.
    //
    // Se exige que la cita este CONFIRMADA
    // y que no tenga una atencion medica registrada.
    //
    // Al guardar la atencion:
    // CONFIRMADA -> COMPLETADA
    //
    // Ambas operaciones forman parte de la misma transaccion.
    //
    // El correoAutenticado debe obtenerse del JWT
    // en el controlador, NUNCA del JSON del cliente.
    //
    // Por ahora no recibimos medicamentos.
    // Mas adelante se seleccionaran del catalogo
    // creado exclusivamente por ADMIN.
    //
    // ======================================================

    @PreAuthorize("hasRole('VETERINARIO')")
    @Transactional
    public AtencionMedica registrarAtencion(
            Long citaId,
            String diagnostico,
            String tratamiento,
            String observaciones,
            String correoAutenticado
    ) {

        // 1. Buscar al usuario autenticado.
        Usuario usuario = buscarUsuarioAutenticado(
                correoAutenticado
        );

        // 2. Obtener el veterinario vinculado a su cuenta.
        Veterinario veterinario = buscarVeterinarioVinculado(
                usuario
        );

        // 3. Buscar la cita.
        Cita cita = buscarCita(citaId);

        // ==================================================
        // VALIDAR QUE LA CITA PERTENEZCA AL VETERINARIO
        // ==================================================

        if (
                cita.getVeterinario() == null
                ||
                !veterinario.getId().equals(
                        cita.getVeterinario().getId()
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo puedes atender citas asignadas a tu cuenta veterinaria"
            );

        }

        // ==================================================
        // VALIDAR ESTADO DE LA CITA
        // ==================================================
        //
        // Solo se registra una atencion
        // cuando la cita esta CONFIRMADA.
        //
        // ==================================================

        if (!"CONFIRMADA".equals(cita.getEstado())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La cita debe estar CONFIRMADA para registrar la atencion"
            );

        }

        // ==================================================
        // EVITAR ATENCIONES DUPLICADAS
        // ==================================================
        //
        // Una misma cita solo puede tener
        // una atencion medica registrada.
        //
        // ==================================================

        if (atencionRepository.existsByCita_Id(citaId)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Esta cita ya tiene una atencion medica registrada"
            );

        }

        // ==================================================
        // VALIDAR LOS DATOS MEDICOS
        // ==================================================

        String diagnosticoValidado = textoObligatorio(
                diagnostico,
                "diagnostico"
        );

        String tratamientoValidado = textoObligatorio(
                tratamiento,
                "tratamiento"
        );

        String observacionesValidadas = textoOpcional(
                observaciones,
                "observaciones"
        );

        // ==================================================
        // CREAR LA ATENCION MEDICA
        // ==================================================

        AtencionMedica atencion = new AtencionMedica();

        atencion.setCita(cita);

        atencion.setDiagnostico(
                diagnosticoValidado
        );

        atencion.setTratamiento(
                tratamientoValidado
        );

        atencion.setObservaciones(
                observacionesValidadas
        );

        // ==================================================
        // MEDICAMENTOS OPCIONALES
        // ==================================================
        //
        // Por ahora quedan vacios.
        //
        // Cuando creemos el inventario:
        // - ADMIN registra los medicamentos.
        // - VETERINARIO selecciona los existentes.
        // - Si se utiliza un medicamento, se registrara
        //   la cantidad consumida.
        // - La prescripcion no descuenta existencias.
        //
        // ==================================================

        atencion.setMedicamentosUtilizados(null);
        atencion.setMedicamentosRecetados(null);

        // ==================================================
        // GUARDAR ATENCION MEDICA
        // ==================================================

        AtencionMedica guardada =
                atencionRepository.save(atencion);

        // ==================================================
        // COMPLETAR LA CITA AUTOMATICAMENTE
        // ==================================================
        //
        // La cita pasa de CONFIRMADA a COMPLETADA.
        //
        // Si ocurre un error en la transaccion,
        // se revierten tanto el cambio de estado
        // como el registro de la atencion.
        //
        // ==================================================

        cita.setEstado("COMPLETADA");

        citaService.guardar(cita);

        return guardada;

    }

    // ======================================================
    // 2. BUSCAR UNA ATENCION POR CITA
    // ======================================================
    //
    // ADMIN:
    // Puede consultar cualquier atencion.
    //
    // USER:
    // Solo puede consultar atenciones de sus mascotas.
    //
    // VETERINARIO:
    // Solo puede consultar atenciones de sus citas.
    //
    // ======================================================

    @PreAuthorize("hasAnyRole('ADMIN','USER','VETERINARIO')")
    @Transactional(readOnly = true)
    public AtencionMedica buscarPorCita(
            Long citaId,
            String correoAutenticado
    ) {

        // Buscar al usuario autenticado.
        Usuario usuario = buscarUsuarioAutenticado(
                correoAutenticado
        );

        // Buscar la atencion registrada para la cita.
        AtencionMedica atencion =
                atencionRepository.findByCita_Id(citaId)

                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Atencion medica no encontrada para la cita: "
                        + citaId
                    )
                );

        // Validar permisos de lectura.
        validarLecturaAtencion(
                atencion,
                usuario
        );

        return atencion;

    }

    // ======================================================
    // 3. CONSULTAR HISTORIAL CLINICO DE UNA MASCOTA
    // ======================================================
    //
    // USER:
    // Solo puede consultar el historial de sus mascotas.
    //
    // VETERINARIO:
    // Solo si tiene alguna cita asignada con la mascota.
    //
    // ADMIN:
    // Puede consultar todos los historiales.
    //
    // ======================================================

    @PreAuthorize("hasAnyRole('ADMIN','USER','VETERINARIO')")
    @Transactional(readOnly = true)
    public List<AtencionMedica> historialDeMascota(
            Long mascotaId,
            String correoAutenticado
    ) {

        // ==================================================
        // BUSCAR USUARIO Y MASCOTA
        // ==================================================

        Usuario usuario = buscarUsuarioAutenticado(
                correoAutenticado
        );

        Mascota mascota = mascotaService.buscarPorId(mascotaId)

                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Mascota no encontrada con id: " + mascotaId
                    )
                );

        // ==================================================
        // VALIDAR PERMISOS DEL USER
        // ==================================================

        if (usuario.getRol() == Rol.USER) {

            if (
                    mascota.getUsuario() == null
                    ||
                    !usuario.getId().equals(
                            mascota.getUsuario().getId()
                    )
            ) {

                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No puedes consultar el historial de otra mascota"
                );

            }

        }

        // ==================================================
        // VALIDAR PERMISOS DEL VETERINARIO
        // ==================================================

        else if (usuario.getRol() == Rol.VETERINARIO) {

            Veterinario veterinario =
                    buscarVeterinarioVinculado(usuario);

            // Comprobar que tenga alguna cita con esta mascota.
            boolean tieneCitaConMascota = citaService

                    .listarPorVeterinario(veterinario.getId())

                    .stream()

                    .anyMatch(cita ->

                            cita.getMascota() != null
                            &&
                            mascotaId.equals(
                                    cita.getMascota().getId()
                            )

                    );

            if (!tieneCitaConMascota) {

                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "La mascota no tiene citas asignadas a este veterinario"
                );

            }

        }

        // ==================================================
        // IMPEDIR ACCESO DE ROLES NO PERMITIDOS
        // ==================================================

        else if (usuario.getRol() != Rol.ADMIN) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tienes permisos para consultar historiales"
            );

        }

        // ==================================================
        // DEVOLVER HISTORIAL CLINICO
        // ==================================================
        //
        // Se ordena de la atencion mas reciente
        // a la mas antigua.
        //
        // ==================================================

        return atencionRepository
                .findByCita_Mascota_IdOrderByFechaAtencionDesc(
                        mascotaId
                );

    }

    // ======================================================
    // 4. CONSULTAR MIS ATENCIONES COMO VETERINARIO
    // ======================================================
    //
    // Solo devuelve las atenciones correspondientes
    // al veterinario autenticado.
    //
    // ======================================================

    @PreAuthorize("hasRole('VETERINARIO')")
    @Transactional(readOnly = true)
    public List<AtencionMedica> misAtenciones(
            String correoAutenticado
    ) {

        Usuario usuario = buscarUsuarioAutenticado(
                correoAutenticado
        );

        Veterinario veterinario =
                buscarVeterinarioVinculado(usuario);

        return atencionRepository
                .findByCita_Veterinario_IdOrderByFechaAtencionDesc(
                        veterinario.getId()
                );

    }

    // ======================================================
    // NUEVO: CONSULTAR TODAS LAS ATENCIONES MEDICAS
    // ======================================================
    //
    // SOLO ADMIN.
    //
    // Devuelve todas las atenciones registradas, sin
    // restringir los resultados a un veterinario concreto.
    //
    // Esta consulta es de solo lectura: no permite cambiar
    // diagnosticos, tratamientos ni datos clinicos.
    //
    // GET /api/atenciones-medicas se agregara despues
    // en AtencionMedicaController.
    //
    // ======================================================

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<AtencionMedica> listarTodas() {

        return atencionRepository.findAll();

    }

    // ======================================================
    // PASO 50: CONSULTAR ATENCIONES POR VETERINARIO
    // ======================================================
    //
    // SOLO ADMIN.
    //
    // Permite consultar las atenciones de un veterinario
    // especifico por su ID, ordenadas de la mas reciente
    // a la mas antigua.
    //
    // Se reutiliza un metodo existente del repositorio,
    // utilizado tambien por misAtenciones().
    //
    // Esta consulta no modifica datos clinicos.
    //
    // ======================================================

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<AtencionMedica> listarPorVeterinario(Long veterinarioId) {

        // Validar que el identificador sea positivo.
        if (veterinarioId == null || veterinarioId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El ID del veterinario debe ser mayor que cero"
            );
        }

        // Devolver las atenciones del veterinario indicado.
        // Si no tiene atenciones, se obtiene una lista vacia.
        return atencionRepository
                .findByCita_Veterinario_IdOrderByFechaAtencionDesc(
                        veterinarioId
                );
    }

    // ======================================================
    // PASO 51: CONSULTAR ATENCIONES POR RANGO DE FECHAS
    // ======================================================
    //
    // SOLO ADMIN.
    //
    // Filtra por fechaAtencion, incluyendo ambos dias.
    // La fecha final se convierte al inicio del dia siguiente
    // para incluir todas las horas y fracciones de segundo.
    //
    // Ejemplo:
    // desde = 2026-10-01
    // hasta = 2026-10-20
    //
    // Se consulta desde 2026-10-01T00:00:00 (inclusive)
    // hasta 2026-10-21T00:00:00 (exclusivo).
    //
    // ======================================================

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<AtencionMedica> listarPorFechas(
            LocalDate desde,
            LocalDate hasta
    ) {

        // Ambas fechas son obligatorias.
        if (desde == null || hasta == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes indicar las fechas desde y hasta"
            );
        }

        // El inicio del periodo no puede superar al final.
        if (desde.isAfter(hasta)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La fecha inicial no puede ser posterior a la fecha final"
            );
        }

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime finExclusivo;

        try {
            finExclusivo = hasta.plusDays(1).atStartOfDay();
        } catch (DateTimeException excepcion) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La fecha final esta fuera del rango permitido",
                    excepcion
            );
        }

        // El repositorio devuelve los resultados de mas recientes
        // a mas antiguos. Sin coincidencias devuelve una lista vacia.
        return atencionRepository
                .findByFechaAtencionGreaterThanEqualAndFechaAtencionLessThanOrderByFechaAtencionDesc(
                        inicio,
                        finExclusivo
                );
    }

    // ======================================================
    // PASO 52: ESTADISTICAS GENERALES DE ATENCIONES MEDICAS
    // ======================================================
    //
    // SOLO ADMIN.
    //
    // Calcula las estadisticas directamente de las atenciones
    // registradas, sin modificar registros ni crear tablas.
    //
    // - Total de atenciones medicas.
    // - Cantidad de mascotas diferentes atendidas.
    // - Cantidad de veterinarios con atenciones registradas.
    // - Total de atenciones realizadas por cada veterinario.
    //
    // Los veterinarios se muestran de mayor a menor cantidad
    // de atenciones. Si no existen datos, devuelve ceros
    // y una lista vacia.
    //
    // ======================================================

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public EstadisticasAtenciones obtenerEstadisticas() {

        // Leer todas las atenciones desde PostgreSQL.
        List<AtencionMedica> atenciones = atencionRepository.findAll();

        // Cada ID de mascota se cuenta una sola vez.
        Set<Long> mascotasAtendidas = new HashSet<>();

        // Guardar cantidad y nombre para cada veterinario.
        Map<Long, Long> cantidadesPorVeterinario = new HashMap<>();
        Map<Long, String> nombresPorVeterinario = new HashMap<>();

        // Recorrer las atenciones sin cambiar los datos guardados.
        for (AtencionMedica atencion : atenciones) {

            Cita cita = atencion.getCita();
            if (cita == null) {
                continue;
            }

            // Contabilizar la mascota asociada a la cita.
            if (cita.getMascota() != null && cita.getMascota().getId() != null) {
                mascotasAtendidas.add(cita.getMascota().getId());
            }

            // Agrupar las atenciones por veterinario.
            if (cita.getVeterinario() != null
                    && cita.getVeterinario().getId() != null) {

                Long veterinarioId = cita.getVeterinario().getId();

                cantidadesPorVeterinario.merge(veterinarioId, 1L, Long::sum);
                nombresPorVeterinario.putIfAbsent(
                        veterinarioId,
                        cita.getVeterinario().getNombre()
                );
            }
        }

        // Construir la lista de estadisticas por veterinario.
        List<AtencionesPorVeterinario> resumenVeterinarios = new ArrayList<>();

        for (Map.Entry<Long, Long> entrada : cantidadesPorVeterinario.entrySet()) {
            Long veterinarioId = entrada.getKey();

            resumenVeterinarios.add(new AtencionesPorVeterinario(
                    veterinarioId,
                    nombresPorVeterinario.get(veterinarioId),
                    entrada.getValue()
            ));
        }

        // Ordenar de mayor a menor numero de atenciones.
        resumenVeterinarios.sort(
                Comparator.comparingLong(
                        AtencionesPorVeterinario::totalAtenciones
                ).reversed().thenComparing(AtencionesPorVeterinario::veterinarioId)
        );

        // Devolver exclusivamente los datos estadisticos.
        return new EstadisticasAtenciones(
                atenciones.size(),
                mascotasAtendidas.size(),
                cantidadesPorVeterinario.size(),
                resumenVeterinarios
        );
    }

    // ======================================================
    // DTO: RESUMEN DE ATENCIONES POR VETERINARIO
    // ======================================================
    // Contiene solo el ID, nombre y cantidad de atenciones.

    public record AtencionesPorVeterinario(
            Long veterinarioId,
            String nombreVeterinario,
            long totalAtenciones
    ) {
    }

    // ======================================================
    // DTO: ESTADISTICAS GENERALES
    // ======================================================
    // Este DTO sera devuelto como JSON desde el Controller.

    public record EstadisticasAtenciones(
            long totalAtenciones,
            long totalMascotasAtendidas,
            long totalVeterinariosConAtenciones,
            List<AtencionesPorVeterinario> atencionesPorVeterinario
    ) {
    }

    // ======================================================
    // METODOS AUXILIARES
    // ======================================================

    // ======================================================
    // BUSCAR USUARIO AUTENTICADO
    // ======================================================
    //
    // La identidad se obtiene del JWT.
    // No se permite utilizar un correo enviado por el usuario.
    //
    // ======================================================

    private Usuario buscarUsuarioAutenticado(
            String correoAutenticado
    ) {

        if (
                correoAutenticado == null
                ||
                correoAutenticado.isBlank()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "No se identifico al usuario autenticado"
            );

        }

        return usuarioRepository.findByCorreo(
                correoAutenticado
        )

        .orElseThrow(() ->
            new ResourceNotFoundException(
                "Usuario autenticado no encontrado"
            )
        );

    }

    // ======================================================
    // BUSCAR VETERINARIO VINCULADO
    // ======================================================
    //
    // Busca la ficha profesional relacionada
    // con la cuenta que inicio sesion.
    //
    // ======================================================

    private Veterinario buscarVeterinarioVinculado(
            Usuario usuario
    ) {

        if (usuario.getRol() != Rol.VETERINARIO) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "La cuenta no tiene el rol VETERINARIO"
            );

        }

        return veterinarioService
                .buscarPorUsuarioId(usuario.getId())

                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "La cuenta no tiene un veterinario vinculado"
                    )
                );

    }

    // ======================================================
    // BUSCAR CITA POR ID
    // ======================================================

    private Cita buscarCita(Long citaId) {

        return citaService.buscarPorId(citaId)

                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Cita no encontrada con id: " + citaId
                    )
                );

    }

    // ======================================================
    // VALIDAR PERMISOS DE LECTURA
    // ======================================================
    //
    // ADMIN consulta cualquier atencion.
    // VETERINARIO consulta las de sus citas.
    // USER consulta las de sus mascotas.
    //
    // ======================================================

    private void validarLecturaAtencion(
            AtencionMedica atencion,
            Usuario usuario
    ) {

        // ADMIN puede consultar todo.
        if (usuario.getRol() == Rol.ADMIN) {
            return;
        }

        Cita cita = atencion.getCita();

        // ==================================================
        // VETERINARIO
        // ==================================================

        if (usuario.getRol() == Rol.VETERINARIO) {

            Veterinario veterinario =
                    buscarVeterinarioVinculado(usuario);

            if (
                    cita.getVeterinario() != null
                    &&
                    veterinario.getId().equals(
                            cita.getVeterinario().getId()
                    )
            ) {
                return;
            }

        }

        // ==================================================
        // USER
        // ==================================================

        else if (usuario.getRol() == Rol.USER) {

            if (
                    cita.getMascota() != null
                    &&
                    cita.getMascota().getUsuario() != null
                    &&
                    usuario.getId().equals(
                            cita.getMascota()
                                    .getUsuario()
                                    .getId()
                    )
            ) {
                return;
            }

        }

        // Si no cumple ninguna validacion,
        // se rechaza la consulta.
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No tienes permiso para consultar esta atencion medica"
        );

    }

    // ======================================================
    // VALIDAR TEXTO OBLIGATORIO
    // ======================================================
    //
    // Diagnostico y tratamiento no pueden estar vacios.
    //
    // ======================================================

    private String textoObligatorio(
            String valor,
            String campo
    ) {

        if (
                valor == null
                ||
                valor.isBlank()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El campo " + campo + " es obligatorio"
            );

        }

        return validarLongitud(
                valor.trim(),
                campo
        );

    }

    // ======================================================
    // VALIDAR TEXTO OPCIONAL
    // ======================================================
    //
    // Las observaciones pueden ser nulas.
    //
    // ======================================================

    private String textoOpcional(
            String valor,
            String campo
    ) {

        if (
                valor == null
                ||
                valor.isBlank()
        ) {
            return null;
        }

        return validarLongitud(
                valor.trim(),
                campo
        );

    }

    // ======================================================
    // VALIDAR LONGITUD DE CAMPOS
    // ======================================================
    //
    // Se permite un maximo de 2000 caracteres.
    //
    // ======================================================

    private String validarLongitud(
            String valor,
            String campo
    ) {

        if (valor.length() > 2000) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El campo " + campo
                    + " no puede superar 2000 caracteres"
            );

        }

        return valor;

    }

}
