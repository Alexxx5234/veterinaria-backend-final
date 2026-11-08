package com.veterinaria.veterinaria.service;

// ==========================================================
// IMPORTACIONES GENERALES
// ==========================================================

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Bloqueos transaccionales.
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

// ==========================================================
// MODELOS
// ==========================================================

import com.veterinaria.veterinaria.model.AtencionMedica;
import com.veterinaria.veterinaria.model.Cita;
import com.veterinaria.veterinaria.model.DetalleMedicamentoAtencion;
import com.veterinaria.veterinaria.model.DetalleMedicamentoAtencion.TipoRegistro;
import com.veterinaria.veterinaria.model.Medicamento;
import com.veterinaria.veterinaria.model.Rol;
import com.veterinaria.veterinaria.model.Usuario;
import com.veterinaria.veterinaria.model.Veterinario;

// ==========================================================
// REPOSITORIOS
// ==========================================================

import com.veterinaria.veterinaria.repository.AtencionMedicaRepository;
import com.veterinaria.veterinaria.repository.DetalleMedicamentoAtencionRepository;
import com.veterinaria.veterinaria.repository.FacturaRepository;
import com.veterinaria.veterinaria.repository.MedicamentoRepository;
import com.veterinaria.veterinaria.repository.UsuarioRepository;

// ==========================================================
// EXCEPCIONES
// ==========================================================

import com.veterinaria.veterinaria.exception.ResourceNotFoundException;


// ==========================================================
// SERVICIO DE DETALLES DE MEDICAMENTOS
// VETERINARIA ARÉVALO
// ==========================================================
//
// FUNCIONALIDADES:
//
// 1. Registrar un medicamento individual.
// 2. Registrar varios medicamentos en una operación.
// 3. Consultar medicamentos de una atención.
// 4. Consultar medicamentos por tipo.
// 5. Consultar medicamentos por mascota.
// 6. Consultar historial de un medicamento.
// 7. Buscar un detalle por ID.
//
// PERMISOS:
//
// ADMIN:
// - Administra catálogo e inventario.
// - Consulta registros de medicamentos.
//
// VETERINARIO:
// - Registra medicamentos en sus atenciones.
// - Selecciona medicamentos activos.
// - No modifica precios ni inventario directamente.
//
// USER:
// - Consulta los registros autorizados
//   de las atenciones de sus mascotas.
//
// TIPOS:
//
// UTILIZADO:
// - Descuenta stock.
// - Genera importe.
//
// RECETADO:
// - No descuenta stock.
// - No genera cobro.
//
// RECETADO_ENTREGADO:
// - Descuenta stock.
// - Genera importe.
//
// REGISTRO POR LOTES:
//
// - Hasta 30 medicamentos por operación.
// - Una sola transacción.
// - Si un medicamento falla, se revierte todo.
// - No permite duplicados.
// - Respeta los permisos y validaciones existentes.
//
// ==========================================================

@Service
public class DetalleMedicamentoAtencionService {

    // ======================================================
    // DEPENDENCIAS
    // ======================================================

    private final DetalleMedicamentoAtencionRepository detalleRepository;

    private final FacturaRepository facturaRepository;

    private final AtencionMedicaRepository atencionRepository;

    private final MedicamentoRepository medicamentoRepository;

    private final UsuarioRepository usuarioRepository;

    private final VeterinarioService veterinarioService;

    private final AtencionMedicaService atencionMedicaService;

    private final EntityManager entityManager;


    // ======================================================
    // CONSTRUCTOR
    // ======================================================

    public DetalleMedicamentoAtencionService(
            DetalleMedicamentoAtencionRepository detalleRepository,
            FacturaRepository facturaRepository,
            AtencionMedicaRepository atencionRepository,
            MedicamentoRepository medicamentoRepository,
            UsuarioRepository usuarioRepository,
            VeterinarioService veterinarioService,
            AtencionMedicaService atencionMedicaService,
            EntityManager entityManager
    ) {

        this.detalleRepository = detalleRepository;
        this.facturaRepository = facturaRepository;
        this.atencionRepository = atencionRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.veterinarioService = veterinarioService;
        this.atencionMedicaService = atencionMedicaService;
        this.entityManager = entityManager;

    }


    // ======================================================
    // 1. REGISTRAR UN MEDICAMENTO INDIVIDUAL
    // ======================================================

    @PreAuthorize("hasRole('VETERINARIO')")
    @Transactional
    public DetalleMedicamentoAtencion registrar(
            Long atencionId,
            Long medicamentoId,
            TipoRegistro tipoRegistro,
            BigDecimal cantidad,
            String indicaciones,
            String correoAutenticado
    ) {

        // PASO 1. VALIDAR DATOS.

        if (
                atencionId == null
                || atencionId <= 0
                || medicamentoId == null
                || medicamentoId <= 0
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe seleccionar una atencion y un medicamento validos"
            );

        }

        if (tipoRegistro == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe seleccionar un tipo de registro valido"
            );

        }

        BigDecimal cantidadValidada =
                validarCantidad(cantidad);

        String indicacionesValidadas =
                validarIndicaciones(indicaciones);


        // PASO 2. IDENTIFICAR AL VETERINARIO.

        Usuario usuario = obtenerUsuarioAutenticado(
                correoAutenticado
        );

        if (usuario.getRol() != Rol.VETERINARIO) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo un veterinario puede registrar medicamentos"
            );

        }

        Veterinario veterinario = veterinarioService
                .buscarPorUsuarioId(usuario.getId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "La cuenta no tiene una ficha veterinaria vinculada"
                        )
                );


        // PASO 3. BLOQUEAR ATENCION.

        AtencionMedica atencion = entityManager.find(
                AtencionMedica.class,
                atencionId,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (atencion == null) {

            throw new ResourceNotFoundException(
                    "Atencion medica no encontrada con id: " + atencionId
            );

        }


        // PASO 4. VALIDAR VETERINARIO ASIGNADO.

        Cita cita = atencion.getCita();

        if (
                cita == null
                || cita.getVeterinario() == null
                || !veterinario.getId().equals(
                        cita.getVeterinario().getId()
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo puedes registrar medicamentos en tus propias atenciones"
            );

        }


        // PASO 5. VALIDAR ESTADO DE CITA.

        if (!"COMPLETADA".equals(cita.getEstado())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La cita debe estar COMPLETADA para registrar medicamentos"
            );

        }


        // PASO 6. IMPEDIR MODIFICACIONES DESPUES DE FACTURAR.

        if (facturaRepository.existsByAtencionMedica_Id(atencionId)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Esta atencion ya tiene una factura. No puedes agregar medicamentos"
            );

        }


        // PASO 7. EVITAR DUPLICADOS.

        boolean existeDetalle = detalleRepository
                .existsByAtencionMedica_IdAndMedicamento_IdAndTipoRegistro(
                        atencionId,
                        medicamentoId,
                        tipoRegistro
                );

        if (existeDetalle) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este medicamento ya fue registrado con el mismo tipo"
            );

        }


        // PASO 8. BUSCAR Y BLOQUEAR MEDICAMENTO.

        Medicamento medicamento = entityManager.find(
                Medicamento.class,
                medicamentoId,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (medicamento == null) {

            throw new ResourceNotFoundException(
                    "Medicamento no encontrado con id: " + medicamentoId
            );

        }


        // PASO 9. VALIDAR ESTADO.

        if (!medicamento.isActivo()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El medicamento esta inactivo y no puede seleccionarse"
            );

        }


        // PASO 10. OBTENER PRECIO REAL.

        BigDecimal precioUnitario =
                medicamento.getPrecioUnitario();

        if (
                precioUnitario == null
                || precioUnitario.compareTo(BigDecimal.ZERO) < 0
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El medicamento no tiene un precio valido"
            );

        }


        // PASO 11. CALCULAR SUBTOTAL.

        BigDecimal subtotal = BigDecimal.ZERO.setScale(2);

        if (
                tipoRegistro == TipoRegistro.UTILIZADO
                || tipoRegistro == TipoRegistro.RECETADO_ENTREGADO
        ) {

            subtotal = precioUnitario
                    .multiply(cantidadValidada)
                    .setScale(2, RoundingMode.HALF_UP);

            if (subtotal.precision() - subtotal.scale() > 10) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El subtotal supera el valor maximo permitido"
                );

            }

        }


        // PASO 12. DESCONTAR STOCK.

        if (
                tipoRegistro == TipoRegistro.UTILIZADO
                || tipoRegistro == TipoRegistro.RECETADO_ENTREGADO
        ) {

            BigDecimal stockActual =
                    medicamento.getStockDisponible();

            if (stockActual == null) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El medicamento no tiene stock registrado"
                );

            }

            if (stockActual.compareTo(cantidadValidada) < 0) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Stock insuficiente. Disponible: "
                                + stockActual
                                + " "
                                + medicamento.getUnidadMedida()
                );

            }

            BigDecimal nuevoStock =
                    stockActual.subtract(cantidadValidada);

            medicamento.setStockDisponible(nuevoStock);

            medicamentoRepository.save(medicamento);

        }


        // PASO 13. CREAR DETALLE.

        DetalleMedicamentoAtencion detalle =
                new DetalleMedicamentoAtencion();

        detalle.setId(null);

        detalle.setAtencionMedica(atencion);

        detalle.setMedicamento(medicamento);

        detalle.setTipoRegistro(tipoRegistro);

        detalle.setCantidad(cantidadValidada);


        // PASO 14. CONSERVAR DATOS HISTORICOS.

        detalle.setNombreMedicamento(
                medicamento.getNombre()
        );

        detalle.setUnidadMedida(
                medicamento.getUnidadMedida()
        );

        detalle.setPrecioUnitarioAplicado(
                precioUnitario
        );

        detalle.setSubtotal(subtotal);

        detalle.setIndicaciones(
                indicacionesValidadas
        );


        // PASO 15. GUARDAR.

        return detalleRepository.save(detalle);

    }


    // ======================================================
    // 2. REGISTRAR VARIOS MEDICAMENTOS
    // ======================================================
    //
    // Endpoint:
    //
    // POST /api/detalles-medicamentos-atencion/lote
    //
    // IMPORTANTE:
    //
    // Se reutiliza registrar() para conservar todas
    // las validaciones, permisos y reglas del inventario.
    //
    // La transaccion exterior incluye todos los registros.
    //
    // Si el segundo o tercer medicamento falla,
    // se revierte el lote completo.
    //
    // ======================================================

    @PreAuthorize("hasRole('VETERINARIO')")
    @Transactional(rollbackFor = Exception.class)
    public List<DetalleMedicamentoAtencion> registrarVarios(
            Long atencionId,
            List<RegistroMedicamentoLoteItem> medicamentos,
            String correoAutenticado
    ) {

        // VALIDAR ATENCION.

        if (
                atencionId == null
                || atencionId <= 0
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe seleccionar una atencion valida"
            );

        }


        // VALIDAR LISTA.

        if (
                medicamentos == null
                || medicamentos.isEmpty()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe agregar al menos un medicamento"
            );

        }

        if (medicamentos.size() > 30) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Puedes registrar como maximo 30 medicamentos por operacion"
            );

        }


        // VALIDAR DUPLICADOS ANTES DE REGISTRAR.

        Set<String> combinaciones = new HashSet<>();

        for (RegistroMedicamentoLoteItem item : medicamentos) {

            if (item == null) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La lista contiene un medicamento vacio"
                );

            }

            if (
                    item.medicamentoId() == null
                    || item.medicamentoId() <= 0
            ) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Todos los medicamentos deben tener un ID valido"
                );

            }

            if (item.tipoRegistro() == null) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Todos los medicamentos deben tener un tipo de registro"
                );

            }

            String clave =
                    item.medicamentoId()
                    + ":"
                    + item.tipoRegistro().name();

            if (!combinaciones.add(clave)) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "No puedes repetir el mismo medicamento con el mismo tipo"
                );

            }

        }


        // REGISTRAR TODO EL LOTE.

        List<DetalleMedicamentoAtencion> guardados =
                new ArrayList<>();

        for (RegistroMedicamentoLoteItem item : medicamentos) {

            DetalleMedicamentoAtencion detalle = registrar(
                    atencionId,
                    item.medicamentoId(),
                    item.tipoRegistro(),
                    item.cantidad(),
                    item.indicaciones(),
                    correoAutenticado
            );

            guardados.add(detalle);

        }


        // DEVOLVER TODOS LOS REGISTROS.

        return List.copyOf(guardados);

    }


    // ======================================================
    // DTO INTERNO PARA REGISTRO POR LOTES
    // ======================================================

    public record RegistroMedicamentoLoteItem(
            Long medicamentoId,
            TipoRegistro tipoRegistro,
            BigDecimal cantidad,
            String indicaciones
    ) {}


    // ======================================================
    // 3. CONSULTAR MEDICAMENTOS DE UNA ATENCION
    // ======================================================

    @PreAuthorize("hasAnyRole('ADMIN','USER','VETERINARIO')")
    @Transactional(readOnly = true)
    public List<DetalleMedicamentoAtencion> listarPorAtencion(
            Long atencionId,
            String correoAutenticado
    ) {

        AtencionMedica atencion = obtenerAtencion(atencionId);

        atencionMedicaService.buscarPorCita(
                atencion.getCita().getId(),
                correoAutenticado
        );

        return detalleRepository
                .findByAtencionMedica_IdOrderByIdAsc(
                        atencionId
                );

    }


    // ======================================================
    // 4. CONSULTAR MEDICAMENTOS POR TIPO
    // ======================================================

    @PreAuthorize("hasAnyRole('ADMIN','USER','VETERINARIO')")
    @Transactional(readOnly = true)
    public List<DetalleMedicamentoAtencion> listarPorAtencionYTipo(
            Long atencionId,
            TipoRegistro tipoRegistro,
            String correoAutenticado
    ) {

        if (tipoRegistro == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar el tipo de medicamento"
            );

        }

        AtencionMedica atencion = obtenerAtencion(atencionId);

        atencionMedicaService.buscarPorCita(
                atencion.getCita().getId(),
                correoAutenticado
        );

        return detalleRepository
                .findByAtencionMedica_IdAndTipoRegistroOrderByIdAsc(
                        atencionId,
                        tipoRegistro
                );

    }


    // ======================================================
    // 5. CONSULTAR MEDICAMENTOS DE UNA MASCOTA
    // ======================================================

    @PreAuthorize("hasAnyRole('ADMIN','USER','VETERINARIO')")
    @Transactional(readOnly = true)
    public List<DetalleMedicamentoAtencion> listarPorMascota(
            Long mascotaId,
            String correoAutenticado
    ) {

        atencionMedicaService.historialDeMascota(
                mascotaId,
                correoAutenticado
        );

        return detalleRepository
                .findByAtencionMedica_Cita_Mascota_IdOrderByFechaRegistroDesc(
                        mascotaId
                );

    }


    // ======================================================
    // 6. HISTORIAL DE UN MEDICAMENTO
    // ======================================================

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<DetalleMedicamentoAtencion> historialMedicamento(
            Long medicamentoId
    ) {

        if (!medicamentoRepository.existsById(medicamentoId)) {

            throw new ResourceNotFoundException(
                    "Medicamento no encontrado con id: " + medicamentoId
            );

        }

        return detalleRepository
                .findByMedicamento_IdOrderByFechaRegistroDesc(
                        medicamentoId
                );

    }


    // ======================================================
    // 7. BUSCAR DETALLE POR ID
    // ======================================================

    @PreAuthorize("hasAnyRole('ADMIN','USER','VETERINARIO')")
    @Transactional(readOnly = true)
    public DetalleMedicamentoAtencion buscarPorId(
            Long id,
            String correoAutenticado
    ) {

        DetalleMedicamentoAtencion detalle =
                detalleRepository.findById(id)

                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Detalle de medicamento no encontrado con id: "
                                        + id
                        )
                );

        atencionMedicaService.buscarPorCita(
                detalle.getAtencionMedica().getCita().getId(),
                correoAutenticado
        );

        return detalle;

    }


    // ======================================================
    // MÉTODO AUXILIAR: OBTENER ATENCIÓN
    // ======================================================

    private AtencionMedica obtenerAtencion(Long id) {

        return atencionRepository.findById(id)

                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Atencion medica no encontrada con id: " + id
                        )
                );

    }


    // ======================================================
    // MÉTODO AUXILIAR: OBTENER USUARIO AUTENTICADO
    // ======================================================

    private Usuario obtenerUsuarioAutenticado(
            String correoAutenticado
    ) {

        if (
                correoAutenticado == null
                || correoAutenticado.isBlank()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "No se identifico al usuario autenticado"
            );

        }

        return usuarioRepository.findByCorreo(correoAutenticado)

                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Usuario autenticado no encontrado"
                        )
                );

    }


    // ======================================================
    // MÉTODO AUXILIAR: VALIDAR CANTIDAD
    // ======================================================

    private BigDecimal validarCantidad(BigDecimal cantidad) {

        if (cantidad == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La cantidad es obligatoria"
            );

        }

        if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La cantidad debe ser mayor que cero"
            );

        }

        BigDecimal normalizada =
                cantidad.stripTrailingZeros();

        if (normalizada.scale() > 2) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La cantidad admite como maximo dos decimales"
            );

        }

        int digitosEnteros =
                normalizada.precision() - normalizada.scale();

        if (digitosEnteros > 10) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La cantidad supera el maximo permitido"
            );

        }

        return cantidad.setScale(
                2,
                RoundingMode.UNNECESSARY
        );

    }


    // ======================================================
    // MÉTODO AUXILIAR: VALIDAR INDICACIONES
    // ======================================================

    private String validarIndicaciones(String indicaciones) {

        if (
                indicaciones == null
                || indicaciones.isBlank()
        ) {

            return null;

        }

        String texto = indicaciones.trim();

        if (texto.length() > 1000) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Las indicaciones no pueden superar 1000 caracteres"
            );

        }

        return texto;

    }

}