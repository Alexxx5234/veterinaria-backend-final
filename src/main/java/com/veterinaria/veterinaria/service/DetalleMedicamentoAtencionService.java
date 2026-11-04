
package com.veterinaria.veterinaria.service;

// ==========================================================
// IMPORTACIONES GENERALES
// ==========================================================

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Permiten bloquear registros durante una transaccion.
// Evitan que dos atenciones descuenten simultaneamente
// el mismo stock sin comprobar las existencias.
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
// ==========================================================
//
// OBJETIVO:
//
// Registrar medicamentos utilizados o recetados
// durante una atencion medica veterinaria.
//
// ==========================================================
// PERMISOS
// ==========================================================
//
// ADMIN:
// - Administra el catalogo y el inventario.
// - Puede consultar los registros de medicamentos.
//
// VETERINARIO:
// - Registra medicamentos en SUS atenciones.
// - Solo selecciona medicamentos existentes y activos.
// - Indica tipo, cantidad y observaciones.
// - NO crea medicamentos.
// - NO decide el precio.
// - NO modifica directamente el stock.
//
// USER:
// - No registra medicamentos.
// - Puede consultar los detalles autorizados
//   de las atenciones de sus mascotas.
//
// ==========================================================
// FUNCIONAMIENTO
// ==========================================================
//
// UTILIZADO:
// - Descuenta existencias.
// - Copia el precio del catalogo.
// - Calcula el subtotal.
//
// RECETADO:
// - No descuenta existencias.
// - Guarda la prescripcion.
// - Su subtotal es cero.
//
// RECETADO_ENTREGADO:
// - El medicamento se receta y se entrega al propietario.
// - Descuenta existencias y calcula su subtotal.
//
// Los tres tipos requieren un medicamento ACTIVO.
//
// ==========================================================

@Service
public class DetalleMedicamentoAtencionService {

    // ======================================================
    // DEPENDENCIAS
    // ======================================================

    private final DetalleMedicamentoAtencionRepository detalleRepository;

    // Permite comprobar si la atencion ya tiene factura.
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
    // 1. REGISTRAR MEDICAMENTO EN UNA ATENCION
    // ======================================================
    //
    // SOLO VETERINARIO.
    //
    // Recibe:
    // - ID de la atencion.
    // - ID del medicamento existente.
    // - Tipo: UTILIZADO, RECETADO o RECETADO_ENTREGADO.
    // - Cantidad.
    // - Indicaciones opcionales.
    // - Correo obtenido del JWT.
    //
    // IMPORTANTE:
    //
    // El veterinario NO envia precios ni stock.
    //
    // Como AtencionMedicaService completa la cita
    // al guardar la atencion, los medicamentos
    // se registran en una atencion ya COMPLETADA.
    //
    // @Transactional garantiza que el descuento
    // y el detalle se guarden juntos o se reviertan.
    //
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

        // ==================================================
        // PASO 1. VALIDAR DATOS RECIBIDOS
        // ==================================================

        if (atencionId == null || medicamentoId == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe seleccionar la atencion y el medicamento"
            );

        }

        if (tipoRegistro == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El tipo debe ser UTILIZADO, RECETADO o RECETADO_ENTREGADO"
            );

        }

        BigDecimal cantidadValidada =
                validarCantidad(cantidad);

        String indicacionesValidadas =
                validarIndicaciones(indicaciones);


        // ==================================================
        // PASO 2. IDENTIFICAR AL VETERINARIO
        // ==================================================
        //
        // El correo debe proceder del JWT.
        //
        // No confiamos en un usuarioId ni veterinarioId
        // enviado libremente desde Swagger o Angular.
        //
        // ==================================================

        Usuario usuario = obtenerUsuarioAutenticado(
                correoAutenticado
        );

        if (usuario.getRol() != Rol.VETERINARIO) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo un veterinario puede registrar medicamentos en una atencion"
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


        // ==================================================
        // PASO 3. BUSCAR Y BLOQUEAR LA ATENCION MEDICA
        // ==================================================
        //
        // El bloqueo evita que dos peticiones simultaneas
        // registren el mismo medicamento y tipo
        // en la misma atencion.
        //
        // El bloqueo se libera al finalizar la transaccion.
        //
        // ==================================================

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


        // ==================================================
        // PASO 4. VALIDAR VETERINARIO ASIGNADO
        // ==================================================

        Cita cita = atencion.getCita();

        if (
                cita == null
                ||
                cita.getVeterinario() == null
                ||
                !veterinario.getId().equals(
                        cita.getVeterinario().getId()
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo puedes registrar medicamentos en tus propias atenciones"
            );

        }


        // ==================================================
        // PASO 5. VALIDAR ESTADO DE LA CITA
        // ==================================================
        //
        // Nuestro flujo actual es:
        //
        // CONFIRMADA -> REGISTRAR ATENCION -> COMPLETADA
        //
        // Por eso permitimos agregar medicamentos
        // despues de registrar la atencion.
        //
        // No se admiten registros en citas canceladas.
        //
        // ==================================================

        if (!"COMPLETADA".equals(cita.getEstado())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La cita debe estar COMPLETADA para registrar los medicamentos de su atencion"
            );

        }

        // ==================================================
        // PASO 59. BLOQUEAR CAMBIOS SI EXISTE UNA FACTURA
        // ==================================================
        //
        // IMPORTANTE:
        // El servicio ya tiene un bloqueo PESSIMISTIC_WRITE
        // sobre la atencion medica en esta transaccion.
        // FacturaService.emitir() utiliza el mismo bloqueo.
        //
        // Asi se evita que se emita la factura a la vez
        // que se agrega otro medicamento: una operacion
        // termina primero y la otra verifica la factura.
        //
        // Una factura guardada inmoviliza los cargos. Esto
        // aplica tambien si en el futuro existe el estado
        // PENDIENTE y evita diferencias con los importes.
        //
        // Se comprueba ANTES del descuento de stock.
        // ==================================================

        if (facturaRepository.existsByAtencionMedica_Id(atencionId)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Esta atencion ya tiene una factura registrada. "
                    + "No puedes agregar medicamentos a una atencion facturada"
            );

        }


        // ==================================================
        // PASO 6. EVITAR MEDICAMENTOS DUPLICADOS
        // ==================================================
        //
        // En una misma atencion evitamos repetir
        // el mismo medicamento con el mismo tipo.
        //
        // Ejemplo:
        // Medicamento 1 + UTILIZADO = un solo detalle.
        //
        // El mismo medicamento puede tener diferentes tipos
        // cuando correspondan a usos o entregas distintos.
        //
        // ==================================================

        boolean existeDetalle = detalleRepository
                .existsByAtencionMedica_IdAndMedicamento_IdAndTipoRegistro(
                        atencionId,
                        medicamentoId,
                        tipoRegistro
                );

        if (existeDetalle) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este medicamento ya fue registrado con el mismo tipo en esta atencion"
            );

        }


        // ==================================================
        // PASO 7. BUSCAR Y BLOQUEAR EL MEDICAMENTO
        // ==================================================
        //
        // PESSIMISTIC_WRITE bloquea el registro
        // mientras calculamos y descontamos existencias.
        //
        // Esto protege frente a consumos simultaneos
        // realizados a traves de este servicio.
        //
        // ==================================================

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


        // ==================================================
        // PASO 8. VALIDAR QUE ESTE ACTIVO
        // ==================================================
        //
        // Los medicamentos inactivos pueden consultarse,
        // pero no utilizarse en nuevos registros.
        //
        // ==================================================

        if (!medicamento.isActivo()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El medicamento esta inactivo y no puede seleccionarse"
            );

        }


        // ==================================================
        // PASO 9. OBTENER PRECIO DEL CATALOGO
        // ==================================================
        //
        // El precio se obtiene directamente de PostgreSQL.
        //
        // El veterinario no puede enviar un precio
        // personalizado ni modificar el existente.
        //
        // ==================================================

        BigDecimal precioUnitario =
                medicamento.getPrecioUnitario();

        if (
                precioUnitario == null
                ||
                precioUnitario.compareTo(BigDecimal.ZERO) < 0
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El medicamento no tiene un precio valido"
            );

        }


        // ==================================================
        // PASO 10. CALCULAR SUBTOTAL
        // ==================================================

        BigDecimal subtotal = BigDecimal.ZERO.setScale(2);

        if (tipoRegistro == TipoRegistro.UTILIZADO
                || tipoRegistro == TipoRegistro.RECETADO_ENTREGADO) {

            // Multiplicar cantidad por precio unitario.
            subtotal = precioUnitario
                    .multiply(cantidadValidada)
                    .setScale(2, RoundingMode.HALF_UP);

            // Comprobar que el importe quepa
            // en la columna NUMERIC(12,2).
            if (subtotal.precision() - subtotal.scale() > 10) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El subtotal supera el valor maximo permitido"
                );

            }

        }


        // ==================================================
        // PASO 11. DESCONTAR STOCK SI FUE UTILIZADO O ENTREGADO
        // ==================================================
        //
        // UTILIZADO y RECETADO_ENTREGADO:
        // - Validar existencias y descontar la cantidad.
        //
        // RECETADO:
        // - No descontar existencias ni cobrar.
        //
        // ==================================================

        if (tipoRegistro == TipoRegistro.UTILIZADO
                || tipoRegistro == TipoRegistro.RECETADO_ENTREGADO) {

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


        // ==================================================
        // PASO 12. CREAR EL DETALLE
        // ==================================================

        DetalleMedicamentoAtencion detalle =
                new DetalleMedicamentoAtencion();

        detalle.setId(null);

        detalle.setAtencionMedica(atencion);

        detalle.setMedicamento(medicamento);

        detalle.setTipoRegistro(tipoRegistro);

        detalle.setCantidad(cantidadValidada);


        // ==================================================
        // PASO 13. GUARDAR DATOS HISTORICOS
        // ==================================================
        //
        // Copiamos el nombre, unidad y precio actuales.
        //
        // Si ADMIN modifica el catalogo en el futuro,
        // esta atencion conserva los valores originales.
        //
        // ==================================================

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

        detalle.setIndicaciones(indicacionesValidadas);


        // ==================================================
        // PASO 14. GUARDAR EN POSTGRESQL
        // ==================================================
        //
        // La transaccion incluye:
        //
        // - Descuento de stock, si corresponde.
        // - Registro del medicamento.
        //
        // Si algo falla, no se confirma ninguno.
        //
        // ==================================================

        return detalleRepository.save(detalle);

    }


    // ======================================================
    // 2. CONSULTAR MEDICAMENTOS DE UNA ATENCION
    // ======================================================
    //
    // ADMIN:
    // Puede consultar cualquier atencion.
    //
    // USER:
    // Solo atenciones de sus mascotas.
    //
    // VETERINARIO:
    // Solo atenciones de sus citas asignadas.
    //
    // ======================================================

    @PreAuthorize("hasAnyRole('ADMIN','USER','VETERINARIO')")
    @Transactional(readOnly = true)
    public List<DetalleMedicamentoAtencion> listarPorAtencion(
            Long atencionId,
            String correoAutenticado
    ) {

        AtencionMedica atencion = obtenerAtencion(atencionId);

        // Reutilizamos las validaciones de acceso
        // que ya tiene AtencionMedicaService.
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
    // 3. CONSULTAR MEDICAMENTOS POR TIPO
    // ======================================================
    //
    // Permite filtrar UTILIZADO, RECETADO y RECETADO_ENTREGADO.
    //
    // Se validan los mismos permisos de lectura
    // de la atencion medica.
    //
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
    // 4. CONSULTAR MEDICAMENTOS DE UNA MASCOTA
    // ======================================================
    //
    // ADMIN:
    // Puede consultar todas las mascotas.
    //
    // USER:
    // Solo las mascotas de su propiedad.
    //
    // VETERINARIO:
    // Solo mascotas con citas asignadas a el.
    //
    // ======================================================

    @PreAuthorize("hasAnyRole('ADMIN','USER','VETERINARIO')")
    @Transactional(readOnly = true)
    public List<DetalleMedicamentoAtencion> listarPorMascota(
            Long mascotaId,
            String correoAutenticado
    ) {

        // Este metodo comprueba los permisos
        // sobre el historial de la mascota.
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
    // 5. HISTORIAL DE UN MEDICAMENTO
    // ======================================================
    //
    // SOLO ADMIN.
    //
    // Muestra las atenciones donde aparece un medicamento.
    //
    // No permitimos que un veterinario consulte
    // los consumos clinicos de otros profesionales.
    //
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
    // 6. BUSCAR DETALLE POR ID
    // ======================================================
    //
    // Comprueba los permisos de acceso a la atencion
    // antes de devolver el detalle solicitado.
    //
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

        // Verificar permisos de lectura.
        atencionMedicaService.buscarPorCita(
                detalle.getAtencionMedica().getCita().getId(),
                correoAutenticado
        );

        return detalle;

    }


    // ======================================================
    // METODO AUXILIAR: OBTENER ATENCION
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
    // METODO AUXILIAR: OBTENER USUARIO AUTENTICADO
    // ======================================================
    //
    // No aceptamos un correo del JSON.
    // Siempre debe proceder del JWT.
    //
    // ======================================================

    private Usuario obtenerUsuarioAutenticado(
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

        return usuarioRepository.findByCorreo(correoAutenticado)

                .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Usuario autenticado no encontrado"
                    )
                );

    }


    // ======================================================
    // METODO AUXILIAR: VALIDAR CANTIDAD
    // ======================================================
    //
    // - Obligatoria.
    // - Mayor que cero.
    // - Maximo dos decimales.
    // - Maximo diez digitos enteros.
    //
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
    // METODO AUXILIAR: VALIDAR INDICACIONES
    // ======================================================
    //
    // Campo opcional con maximo 1000 caracteres.
    //
    // ======================================================

    private String validarIndicaciones(String indicaciones) {

        if (
                indicaciones == null
                ||
                indicaciones.isBlank()
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
