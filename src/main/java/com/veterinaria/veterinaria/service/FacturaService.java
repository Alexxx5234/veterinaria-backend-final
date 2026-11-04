package com.veterinaria.veterinaria.service;

// ==========================================================
// IMPORTACIONES
// ==========================================================

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import com.veterinaria.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.veterinaria.model.AtencionMedica;
import com.veterinaria.veterinaria.model.Cita;
import com.veterinaria.veterinaria.model.DetalleMedicamentoAtencion;
import com.veterinaria.veterinaria.model.DetalleMedicamentoAtencion.TipoRegistro;
import com.veterinaria.veterinaria.model.Factura;
import com.veterinaria.veterinaria.model.Factura.EstadoFactura;
import com.veterinaria.veterinaria.model.Mascota;
import com.veterinaria.veterinaria.model.Rol;
import com.veterinaria.veterinaria.model.Servicio;
import com.veterinaria.veterinaria.model.Usuario;
import com.veterinaria.veterinaria.model.Veterinario;
import com.veterinaria.veterinaria.repository.AtencionMedicaRepository;
import com.veterinaria.veterinaria.repository.DetalleMedicamentoAtencionRepository;
import com.veterinaria.veterinaria.repository.FacturaRepository;
import com.veterinaria.veterinaria.repository.UsuarioRepository;

// ==========================================================
// SERVICIO DE FACTURACION INTERNA - PASO 58
// ==========================================================
// La factura se obtiene de una atencion ya completada.
// El veterinario NO vuelve a elegir servicio ni medicamentos.
// - VETERINARIO: previsualiza y emite sus facturas.
// - ADMIN: previsualiza y consulta todas las facturas.
// - USER: consulta las facturas de sus propias mascotas.
//
// IMPORTANTE: Este paso NO genera ni envia el PDF todavia.
// El campo impuestos = 0 es PROVISIONAL para el proyecto;
// no equivale a una factura electronica autorizada por el SRI.
// ==========================================================

@Service
public class FacturaService {

    private static final BigDecimal CERO = new BigDecimal("0.00");

    private final FacturaRepository facturaRepository;
    private final AtencionMedicaRepository atencionRepository;
    private final DetalleMedicamentoAtencionRepository detalleRepository;
    private final UsuarioRepository usuarioRepository;
    private final VeterinarioService veterinarioService;
    private final EntityManager entityManager;

    public FacturaService(
            FacturaRepository facturaRepository,
            AtencionMedicaRepository atencionRepository,
            DetalleMedicamentoAtencionRepository detalleRepository,
            UsuarioRepository usuarioRepository,
            VeterinarioService veterinarioService,
            EntityManager entityManager
    ) {
        this.facturaRepository = facturaRepository;
        this.atencionRepository = atencionRepository;
        this.detalleRepository = detalleRepository;
        this.usuarioRepository = usuarioRepository;
        this.veterinarioService = veterinarioService;
        this.entityManager = entityManager;
    }

    // ======================================================
    // 1. VISTA PREVIA AUTOMATICA POR ATENCION
    // ======================================================
    // Solo VETERINARIO asignado o ADMIN.
    // No crea ni modifica registros en PostgreSQL.
    // Se utilizara al abrir la pantalla de factura en Angular.
    // ======================================================

    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO')")
    @Transactional(readOnly = true)
    public VistaPreviaFactura previsualizar(
            Long atencionId,
            String correoAutenticado
    ) {
        Usuario usuario = obtenerUsuario(correoAutenticado);
        AtencionMedica atencion = obtenerAtencion(atencionId);
        validarPermisoDeAtencion(atencion, usuario, false);
        validarCitaCompletada(atencion);

        boolean yaEmitida = facturaRepository
                .existsByAtencionMedica_Id(atencionId);

        return construirVistaPrevia(atencion, yaEmitida);
    }

    // ======================================================
    // 2. EMITIR FACTURA DE LA ATENCION
    // ======================================================
    // SOLO el VETERINARIO de la cita.
    // El bloqueo sobre la atencion coordina esta operacion con
    // el bloqueo usado por DetalleMedicamentoAtencionService.
    // Tambien se mantiene UNIQUE(atencion_medica_id).
    // ======================================================

    @PreAuthorize("hasRole('VETERINARIO')")
    @Transactional
    public FacturaRespuesta emitir(
            Long atencionId,
            String correoAutenticado
    ) {
        Usuario usuario = obtenerUsuario(correoAutenticado);
        validarId(atencionId, "atencionId");

        // Bloquear la atencion durante la emision.
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

        validarPermisoDeAtencion(atencion, usuario, false);
        validarCitaCompletada(atencion);

        if (facturaRepository.existsByAtencionMedica_Id(atencionId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La atencion ya tiene una factura. Consulta la factura existente"
            );
        }

        // Capturar nombres, cantidades y precios registrados.
        VistaPreviaFactura vista = construirVistaPrevia(atencion, false);

        Factura factura = new Factura();
        factura.setAtencionMedica(atencion);
        factura.setNombreCliente(vista.nombreCliente());
        factura.setCorreoCliente(vista.correoCliente());
        factura.setNombreMascota(vista.nombreMascota());
        factura.setNombreVeterinario(vista.nombreVeterinario());
        factura.setNombreServicio(vista.nombreServicio());
        factura.setPrecioServicio(vista.precioServicio());
        factura.setTotalMedicamentos(vista.totalMedicamentos());
        factura.setSubtotal(vista.subtotal());
        factura.setImpuestos(vista.impuestos());
        factura.setTotal(vista.total());
        factura.setEstado(EstadoFactura.EMITIDA);
        factura.setFechaEmision(LocalDateTime.now());
        factura.setCorreoEnviado(false);

        // Primero guardar para obtener el ID unico generado por PostgreSQL.
        Factura guardada = facturaRepository.saveAndFlush(factura);

        // Numero interno de comprobante, NO numero fiscal del SRI.
        guardada.setNumeroFactura(
                "INT-" + String.format(Locale.ROOT, "%08d", guardada.getId())
        );
        guardada = facturaRepository.saveAndFlush(guardada);

        // El envio de PDF se implementara en otro paso.
        return convertirFactura(guardada);
    }

    // ======================================================
    // 3. BUSCAR FACTURA POR ID
    // ======================================================
    // ADMIN: cualquiera. VETERINARIO: de sus citas.
    // USER: solo de sus mascotas.
    // ======================================================

    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','USER')")
    @Transactional(readOnly = true)
    public FacturaRespuesta buscarPorId(Long facturaId, String correoAutenticado) {
        Usuario usuario = obtenerUsuario(correoAutenticado);
        validarId(facturaId, "facturaId");

        Factura factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Factura no encontrada con id: " + facturaId
                ));

        validarPermisoDeAtencion(factura.getAtencionMedica(), usuario, true);
        return convertirFactura(factura);
    }

    // ======================================================
    // 4. BUSCAR FACTURA POR ATENCION
    // ======================================================
    // Angular podra abrir una factura ya emitida sin duplicarla.
    // ======================================================

    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','USER')")
    @Transactional(readOnly = true)
    public FacturaRespuesta buscarPorAtencion(
            Long atencionId,
            String correoAutenticado
    ) {
        Usuario usuario = obtenerUsuario(correoAutenticado);
        AtencionMedica atencion = obtenerAtencion(atencionId);
        validarPermisoDeAtencion(atencion, usuario, true);

        Factura factura = facturaRepository.findByAtencionMedica_Id(atencionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Todavia no existe una factura para esta atencion"
                ));

        return convertirFactura(factura);
    }

    // ======================================================
    // 5. LISTAR FACTURAS SEGUN ROL
    // ======================================================
    // ADMIN ve todas; VETERINARIO solo sus atenciones;
    // USER solo las facturas de sus mascotas.
    // ======================================================

    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','USER')")
    @Transactional(readOnly = true)
    public List<FacturaRespuesta> listarMisFacturas(String correoAutenticado) {
        Usuario usuario = obtenerUsuario(correoAutenticado);
        List<Factura> facturas;

        if (usuario.getRol() == Rol.ADMIN) {
            facturas = facturaRepository.findAllByOrderByFechaCreacionDesc();
        } else if (usuario.getRol() == Rol.VETERINARIO) {
            Veterinario veterinario = buscarVeterinario(usuario);
            facturas = facturaRepository
                    .findByAtencionMedica_Cita_Veterinario_IdOrderByFechaCreacionDesc(
                            veterinario.getId()
                    );
        } else if (usuario.getRol() == Rol.USER) {
            facturas = facturaRepository
                    .findByAtencionMedica_Cita_Mascota_Usuario_IdOrderByFechaCreacionDesc(
                            usuario.getId()
                    );
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tienes permiso para consultar facturas"
            );
        }

        return facturas.stream().map(this::convertirFactura).toList();
    }

    // ======================================================
    // 6. LISTADO GENERAL EXCLUSIVO PARA ADMIN
    // ======================================================

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<FacturaRespuesta> listarTodas() {
        return facturaRepository.findAllByOrderByFechaCreacionDesc()
                .stream()
                .map(this::convertirFactura)
                .toList();
    }

    // ======================================================
    // AUXILIARES: VISTA PREVIA Y CALCULOS
    // ======================================================

    private VistaPreviaFactura construirVistaPrevia(
            AtencionMedica atencion,
            boolean yaEmitida
    ) {
        Cita cita = atencion.getCita();
        validarDatosDeFacturacion(cita);

        Mascota mascota = cita.getMascota();
        Usuario propietario = mascota.getUsuario();
        Servicio servicio = cita.getServicio();

        // BigDecimal.valueOf transforma el Double existente sin
        // usar new BigDecimal(double), que introduce imprecision.
        BigDecimal precioServicio = BigDecimal
                .valueOf(servicio.getPrecio())
                .setScale(2, RoundingMode.HALF_UP);

        if (precioServicio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El servicio no tiene un precio valido para facturar"
            );
        }

        List<LineaMedicamentoFactura> lineas = detalleRepository
                .findByAtencionMedica_IdOrderByIdAsc(atencion.getId())
                .stream()
                .map(this::convertirLinea)
                .toList();

        BigDecimal totalMedicamentos = lineas.stream()
                .map(LineaMedicamentoFactura::subtotal)
                .reduce(CERO, BigDecimal::add);

        BigDecimal subtotal = precioServicio.add(totalMedicamentos);

        // PROVISIONAL: impuestos sin configurar. No emitir como
        // comprobante tributario hasta definir las reglas aplicables.
        BigDecimal impuestos = CERO;
        BigDecimal total = subtotal.add(impuestos);

        verificarImporte(precioServicio);
        verificarImporte(totalMedicamentos);
        verificarImporte(subtotal);
        verificarImporte(total);

        return new VistaPreviaFactura(
                atencion.getId(),
                cita.getId(),
                propietario.getNombre(),
                propietario.getCorreo(),
                mascota.getNombre(),
                cita.getVeterinario().getNombre(),
                servicio.getNombre(),
                precioServicio,
                lineas,
                totalMedicamentos,
                subtotal,
                impuestos,
                total,
                yaEmitida
        );
    }

    private LineaMedicamentoFactura convertirLinea(DetalleMedicamentoAtencion d) {
        if (d.getTipoRegistro() == null || d.getCantidad() == null
                || d.getPrecioUnitarioAplicado() == null || d.getSubtotal() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Una atencion tiene un medicamento con datos incompletos"
            );
        }

        BigDecimal esperado = CERO;
        if (d.getTipoRegistro() == TipoRegistro.UTILIZADO
                || d.getTipoRegistro() == TipoRegistro.RECETADO_ENTREGADO) {
            esperado = d.getCantidad()
                    .multiply(d.getPrecioUnitarioAplicado())
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Evitar facturar importes inconsistentes con los detalles guardados.
        if (d.getSubtotal().compareTo(esperado) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El subtotal del medicamento " + d.getId()
                            + " no coincide con su cantidad y precio"
            );
        }

        verificarImporte(esperado);

        return new LineaMedicamentoFactura(
                d.getId(),
                d.getMedicamento().getId(),
                d.getNombreMedicamento(),
                d.getUnidadMedida(),
                d.getTipoRegistro(),
                d.getCantidad(),
                d.getPrecioUnitarioAplicado(),
                esperado,
                d.getIndicaciones()
        );
    }

    private FacturaRespuesta convertirFactura(Factura factura) {
        Long atencionId = factura.getAtencionMedica().getId();
        List<LineaMedicamentoFactura> lineas = detalleRepository
                .findByAtencionMedica_IdOrderByIdAsc(atencionId)
                .stream()
                .map(this::convertirLinea)
                .toList();

        return new FacturaRespuesta(
                factura.getId(),
                factura.getNumeroFactura(),
                atencionId,
                factura.getAtencionMedica().getCita().getId(),
                factura.getNombreCliente(),
                factura.getCorreoCliente(),
                factura.getNombreMascota(),
                factura.getNombreVeterinario(),
                factura.getNombreServicio(),
                factura.getPrecioServicio(),
                lineas,
                factura.getTotalMedicamentos(),
                factura.getSubtotal(),
                factura.getImpuestos(),
                factura.getTotal(),
                factura.getEstado(),
                factura.isCorreoEnviado(),
                factura.getFechaCreacion(),
                factura.getFechaEmision()
        );
    }

    // ======================================================
    // AUXILIARES: SEGURIDAD Y VALIDACIONES
    // ======================================================

    private Usuario obtenerUsuario(String correoAutenticado) {
        if (correoAutenticado == null || correoAutenticado.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "No se identifico al usuario autenticado"
            );
        }

        return usuarioRepository.findByCorreo(correoAutenticado)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario autenticado no encontrado"
                ));
    }

    private AtencionMedica obtenerAtencion(Long atencionId) {
        validarId(atencionId, "atencionId");
        return atencionRepository.findById(atencionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Atencion medica no encontrada con id: " + atencionId
                ));
    }

    private Veterinario buscarVeterinario(Usuario usuario) {
        return veterinarioService.buscarPorUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Tu cuenta no tiene una ficha veterinaria vinculada"
                ));
    }

    private void validarPermisoDeAtencion(
            AtencionMedica atencion,
            Usuario usuario,
            boolean permitirPropietario
    ) {
        if (usuario.getRol() == Rol.ADMIN) {
            return;
        }

        Cita cita = atencion.getCita();
        if (cita == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La atencion no tiene una cita relacionada"
            );
        }

        if (usuario.getRol() == Rol.VETERINARIO) {
            Veterinario veterinario = buscarVeterinario(usuario);
            if (cita.getVeterinario() != null
                    && veterinario.getId().equals(cita.getVeterinario().getId())) {
                return;
            }
        } else if (permitirPropietario && usuario.getRol() == Rol.USER) {
            if (cita.getMascota() != null
                    && cita.getMascota().getUsuario() != null
                    && usuario.getId().equals(cita.getMascota().getUsuario().getId())) {
                return;
            }
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No tienes permiso para consultar o emitir esta factura"
        );
    }

    private void validarCitaCompletada(AtencionMedica atencion) {
        if (atencion.getCita() == null
                || !"COMPLETADA".equals(atencion.getCita().getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La cita debe estar COMPLETADA para facturar"
            );
        }
    }

    private void validarDatosDeFacturacion(Cita cita) {
        if (cita == null || cita.getMascota() == null
                || cita.getMascota().getUsuario() == null
                || cita.getVeterinario() == null || cita.getServicio() == null
                || cita.getServicio().getPrecio() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La cita no tiene todos los datos necesarios para facturar"
            );
        }

        Usuario propietario = cita.getMascota().getUsuario();
        if (propietario.getNombre() == null || propietario.getNombre().isBlank()
                || propietario.getCorreo() == null || propietario.getCorreo().isBlank()
                || cita.getMascota().getNombre() == null
                || cita.getVeterinario().getNombre() == null
                || cita.getServicio().getNombre() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Faltan nombres o correo del propietario para facturar"
            );
        }
    }

    private void verificarImporte(BigDecimal importe) {
        if (importe == null || importe.compareTo(BigDecimal.ZERO) < 0
                || importe.precision() - importe.scale() > 10) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Un importe de la factura es invalido o supera el limite permitido"
            );
        }
    }

    private void validarId(Long id, String campo) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El campo " + campo + " debe ser un ID positivo"
            );
        }
    }

    // ======================================================
    // DTOs SEGUROS PARA EL FUTURO CONTROLLER Y ANGULAR
    // ======================================================

    public record LineaMedicamentoFactura(
            Long detalleId,
            Long medicamentoId,
            String nombreMedicamento,
            String unidadMedida,
            TipoRegistro tipoRegistro,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal,
            String indicaciones
    ) {}

    public record VistaPreviaFactura(
            Long atencionId,
            Long citaId,
            String nombreCliente,
            String correoCliente,
            String nombreMascota,
            String nombreVeterinario,
            String nombreServicio,
            BigDecimal precioServicio,
            List<LineaMedicamentoFactura> medicamentos,
            BigDecimal totalMedicamentos,
            BigDecimal subtotal,
            BigDecimal impuestos,
            BigDecimal total,
            boolean yaEmitida
    ) {}

    public record FacturaRespuesta(
            Long id,
            String numeroFactura,
            Long atencionId,
            Long citaId,
            String nombreCliente,
            String correoCliente,
            String nombreMascota,
            String nombreVeterinario,
            String nombreServicio,
            BigDecimal precioServicio,
            List<LineaMedicamentoFactura> medicamentos,
            BigDecimal totalMedicamentos,
            BigDecimal subtotal,
            BigDecimal impuestos,
            BigDecimal total,
            EstadoFactura estado,
            boolean correoEnviado,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaEmision
    ) {}
}
