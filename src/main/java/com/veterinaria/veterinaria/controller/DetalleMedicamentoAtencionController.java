
package com.veterinaria.veterinaria.controller;

// ==========================================================
// IMPORTACIONES GENERALES
// ==========================================================

// Para manejar cantidades, precios y subtotales.
import java.math.BigDecimal;

// Para manejar la fecha de registro.
import java.time.LocalDateTime;

// Para devolver listas de medicamentos.
import java.util.List;

// Para manejar respuestas HTTP.
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

// Para proteger los endpoints segun el rol.
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

// Para crear los endpoints REST.
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// Para validar los datos enviados.
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Documentacion de Swagger.
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

// ==========================================================
// MODELOS
// ==========================================================

import com.veterinaria.veterinaria.model.DetalleMedicamentoAtencion;
import com.veterinaria.veterinaria.model.DetalleMedicamentoAtencion.TipoRegistro;

// ==========================================================
// SERVICIOS
// ==========================================================

import com.veterinaria.veterinaria.service.DetalleMedicamentoAtencionService;


// ==========================================================
// CONTROLADOR DE MEDICAMENTOS EN ATENCIONES MEDICAS
// ==========================================================
//
// OBJETIVO:
//
// Permitir registrar los medicamentos utilizados,
// recetados o recetados y entregados durante las atenciones. 
//
// ==========================================================
// PERMISOS
// ==========================================================
//
// ADMIN:
//
// - Administra el catalogo de medicamentos.
// - Administra precios y existencias.
// - Consulta registros de medicamentos utilizados.
//
// VETERINARIO:
//
// - Selecciona medicamentos existentes.
// - Registra medicamentos en sus propias atenciones.
// - Indica la cantidad y el tipo de registro.
// - Consulta registros autorizados.
//
// NO PUEDE:
//
// - Crear medicamentos nuevos.
// - Modificar precios.
// - Ajustar inventario directamente.
// - Registrar medicamentos en atenciones ajenas.
//
// USER:
//
// - Puede consultar los registros correspondientes
//   a sus propias mascotas.
//
// - No puede registrar medicamentos.
//
// ==========================================================
// FUNCIONAMIENTO
// ==========================================================
//
// UTILIZADO:
//
// - Descuenta el stock.
// - Guarda el precio aplicado.
// - Calcula el subtotal.
//
// RECETADO:
//
// - No descuenta stock.
// - No genera cobro automatico.
//
// RECETADO_ENTREGADO:
//
// - Descuenta stock.
// - Guarda el precio aplicado.
// - Calcula el subtotal.
//
// Los permisos y las operaciones de inventario
// se comprueban dentro del servicio.
//
// ==========================================================

@RestController
@RequestMapping("/api/detalles-medicamentos-atencion")
public class DetalleMedicamentoAtencionController {

    // ======================================================
    // 1. DEPENDENCIA DEL SERVICIO
    // ======================================================

    private final DetalleMedicamentoAtencionService detalleService;

    // ======================================================
    // 2. CONSTRUCTOR
    // ======================================================

    public DetalleMedicamentoAtencionController(
            DetalleMedicamentoAtencionService detalleService
    ) {

        this.detalleService = detalleService;

    }

    // ======================================================
    // 3. REGISTRAR MEDICAMENTO EN UNA ATENCION
    // ======================================================
    //
    // POST /api/detalles-medicamentos-atencion
    //
    // SOLO VETERINARIO.
    //
    // El veterinario envia:
    //
    // - atencionId
    // - medicamentoId
    // - tipoRegistro
    // - cantidad
    // - indicaciones
    //
    // No envia:
    //
    // - precio
    // - subtotal
    // - stock
    // - veterinarioId
    //
    // El precio se obtiene del catalogo.
    // El veterinario se identifica por su JWT.
    //
    // ======================================================

    @Operation(
        summary = "Registrar medicamento en atencion medica",
        description = "Permite al veterinario registrar un medicamento "
                + "UTILIZADO, RECETADO o RECETADO_ENTREGADO "
                + "en una atencion que tenga asignada."
    )

    @ApiResponse(
        responseCode = "201",
        description = "Medicamento registrado correctamente"
    )

    @ApiResponse(
        responseCode = "400",
        description = "Datos enviados incorrectos"
    )

    @ApiResponse(
        responseCode = "403",
        description = "El usuario no tiene permisos"
    )

    @ApiResponse(
        responseCode = "404",
        description = "Atencion o medicamento no encontrado"
    )

    @ApiResponse(
        responseCode = "409",
        description = "Medicamento inactivo, duplicado o stock insuficiente"
    )

    @PostMapping
    @PreAuthorize("hasRole('VETERINARIO')")

    public ResponseEntity<DetalleMedicamentoRespuesta> registrar(
            @Valid @RequestBody RegistroMedicamentoRequest datos,
            Authentication authentication
    ) {

        // 1. Identificar al usuario mediante su JWT.
        String correo = obtenerCorreoAutenticado(
                authentication
        );

        // 2. Enviar los datos al servicio.
        //
        // El servicio valida:
        // - Veterinario asignado.
        // - Atencion existente.
        // - Medicamento activo.
        // - Existencias suficientes.
        // - Registros duplicados.
        //
        // Tambien calcula los importes y descuenta
        // el inventario cuando corresponde.

        DetalleMedicamentoAtencion detalle =
                detalleService.registrar(
                        datos.getAtencionId(),
                        datos.getMedicamentoId(),
                        datos.getTipoRegistro(),
                        datos.getCantidad(),
                        datos.getIndicaciones(),
                        correo
                );

        // 3. Devolver solamente los datos necesarios.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(DetalleMedicamentoRespuesta.desde(detalle));

    }

    // ======================================================
    // 4. CONSULTAR MEDICAMENTOS DE UNA ATENCION
    // ======================================================
    //
    // GET /api/detalles-medicamentos-atencion/atencion/{id}
    //
    // ADMIN:
    // Puede consultar cualquier atencion.
    //
    // VETERINARIO:
    // Solo las atenciones autorizadas.
    //
    // USER:
    // Solo las atenciones de sus mascotas.
    //
    // ======================================================

    @Operation(
        summary = "Consultar medicamentos de una atencion",
        description = "Devuelve los medicamentos utilizados "
                + "y recetados de una atencion medica."
    )

    @GetMapping("/atencion/{atencionId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','VETERINARIO')")

    public ResponseEntity<List<DetalleMedicamentoRespuesta>> listarPorAtencion(
            @PathVariable Long atencionId,
            Authentication authentication
    ) {

        String correo = obtenerCorreoAutenticado(
                authentication
        );

        List<DetalleMedicamentoAtencion> detalles =
                detalleService.listarPorAtencion(
                        atencionId,
                        correo
                );

        List<DetalleMedicamentoRespuesta> respuesta =
                convertirLista(detalles);

        return ResponseEntity.ok(respuesta);

    }

    // ======================================================
    // 5. CONSULTAR MEDICAMENTOS POR TIPO
    // ======================================================
    //
    // GET /api/detalles-medicamentos-atencion/
    //     atencion/{id}/tipo/{tipoRegistro}
    //
    // Permite consultar solamente:
    //
    // - UTILIZADO
    // - RECETADO
    // - RECETADO_ENTREGADO
    //
    // ======================================================

    @Operation(
        summary = "Consultar medicamentos por tipo",
        description = "Permite consultar los medicamentos "
                + "UTILIZADOS, RECETADOS o RECETADOS_ENTREGADOS "
                + "de una atencion."
    )

    @GetMapping("/atencion/{atencionId}/tipo/{tipoRegistro}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','VETERINARIO')")

    public ResponseEntity<List<DetalleMedicamentoRespuesta>> listarPorTipo(
            @PathVariable Long atencionId,
            @PathVariable TipoRegistro tipoRegistro,
            Authentication authentication
    ) {

        String correo = obtenerCorreoAutenticado(
                authentication
        );

        List<DetalleMedicamentoAtencion> detalles =
                detalleService.listarPorAtencionYTipo(
                        atencionId,
                        tipoRegistro,
                        correo
                );

        return ResponseEntity.ok(
                convertirLista(detalles)
        );

    }

    // ======================================================
    // 6. CONSULTAR MEDICAMENTOS DE UNA MASCOTA
    // ======================================================
    //
    // GET /api/detalles-medicamentos-atencion/mascota/{id}
    //
    // Permite recuperar el historial de medicamentos
    // registrados en las atenciones de una mascota.
    //
    // El servicio verifica los permisos.
    //
    // ======================================================

    @Operation(
        summary = "Consultar medicamentos de una mascota",
        description = "Obtiene los medicamentos registrados "
                + "en las atenciones de una mascota."
    )

    @GetMapping("/mascota/{mascotaId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','VETERINARIO')")

    public ResponseEntity<List<DetalleMedicamentoRespuesta>> listarPorMascota(
            @PathVariable Long mascotaId,
            Authentication authentication
    ) {

        String correo = obtenerCorreoAutenticado(
                authentication
        );

        List<DetalleMedicamentoAtencion> detalles =
                detalleService.listarPorMascota(
                        mascotaId,
                        correo
                );

        return ResponseEntity.ok(
                convertirLista(detalles)
        );

    }

    // ======================================================
    // 7. HISTORIAL DE UN MEDICAMENTO
    // ======================================================
    //
    // GET /api/detalles-medicamentos-atencion/
    //     medicamento/{medicamentoId}/historial
    //
    // SOLO ADMIN.
    //
    // Permite consultar las atenciones en las que
    // se utilizo o receto un medicamento.
    //
    // IMPORTANTE:
    //
    // El veterinario puede consultar el catalogo
    // completo de medicamentos, pero no el historial
    // de atenciones de otros profesionales.
    //
    // ======================================================

    @Operation(
        summary = "Consultar historial de medicamento",
        description = "Permite al ADMIN consultar las atenciones "
                + "donde aparece un medicamento determinado."
    )

    @GetMapping("/medicamento/{medicamentoId}/historial")
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<List<DetalleMedicamentoRespuesta>> historialMedicamento(
            @PathVariable Long medicamentoId
    ) {

        List<DetalleMedicamentoAtencion> detalles =
                detalleService.historialMedicamento(
                        medicamentoId
                );

        return ResponseEntity.ok(
                convertirLista(detalles)
        );

    }

    // ======================================================
    // 8. CONSULTAR DETALLE POR ID
    // ======================================================
    //
    // GET /api/detalles-medicamentos-atencion/{id}
    //
    // ADMIN:
    // Puede consultar cualquier detalle.
    //
    // USER y VETERINARIO:
    // Solo los detalles a los que tienen acceso.
    //
    // ======================================================

    @Operation(
        summary = "Consultar detalle de medicamento por ID",
        description = "Obtiene un detalle de medicamento "
                + "comprobando los permisos del usuario."
    )

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','VETERINARIO')")

    public ResponseEntity<DetalleMedicamentoRespuesta> buscarPorId(
            @PathVariable Long id,
            Authentication authentication
    ) {

        String correo = obtenerCorreoAutenticado(
                authentication
        );

        DetalleMedicamentoAtencion detalle =
                detalleService.buscarPorId(
                        id,
                        correo
                );

        return ResponseEntity.ok(
                DetalleMedicamentoRespuesta.desde(detalle)
        );

    }

    // ======================================================
    // 9. OBTENER CORREO DEL USUARIO AUTENTICADO
    // ======================================================
    //
    // El correo se obtiene desde Spring Security.
    //
    // No se recibe desde el JSON para evitar
    // que un usuario suplante a otro.
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
    // 10. CONVERTIR LISTA DE ENTIDADES A RESPUESTAS
    // ======================================================
    //
    // Evita devolver directamente las relaciones JPA.
    //
    // Esto ayuda a prevenir ciclos JSON
    // y evita mostrar datos innecesarios.
    //
    // ======================================================

    private List<DetalleMedicamentoRespuesta> convertirLista(
            List<DetalleMedicamentoAtencion> detalles
    ) {

        return detalles.stream()
                .map(DetalleMedicamentoRespuesta::desde)
                .toList();

    }

    // ======================================================
    // 11. DTO PARA REGISTRAR MEDICAMENTOS
    // ======================================================
    //
    // Representa los datos que enviara el veterinario.
    //
    // Solo recibe identificadores, tipo, cantidad
    // e indicaciones.
    //
    // No recibe precio, subtotal ni stock.
    //
    // ======================================================

    public static class RegistroMedicamentoRequest {

        // Identificador de la atencion medica.
        @NotNull(message = "La atencion medica es obligatoria")
        private Long atencionId;

        // Identificador de un medicamento existente.
        @NotNull(message = "El medicamento es obligatorio")
        private Long medicamentoId;

        // Tipos permitidos: UTILIZADO, RECETADO y RECETADO_ENTREGADO.
        @NotNull(message = "El tipo de registro es obligatorio")
        @Schema(
            description = "UTILIZADO: cobro y descuento de stock; "
                    + "RECETADO: sin cobro ni descuento; "
                    + "RECETADO_ENTREGADO: cobro y descuento de stock",
            allowableValues = {"UTILIZADO", "RECETADO", "RECETADO_ENTREGADO"}
        )
        private TipoRegistro tipoRegistro;

        // Cantidad utilizada o recetada.
        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(
            value = "0.01",
            message = "La cantidad debe ser mayor que cero"
        )
        @Digits(
            integer = 10,
            fraction = 2,
            message = "La cantidad admite maximo dos decimales"
        )
        private BigDecimal cantidad;

        // Indicaciones opcionales.
        @Size(
            max = 1000,
            message = "Las indicaciones no pueden superar 1000 caracteres"
        )
        private String indicaciones;

        // Constructor necesario para Jackson.
        public RegistroMedicamentoRequest() {
        }

        // ==================================================
        // GETTERS Y SETTERS
        // ==================================================

        public Long getAtencionId() {
            return atencionId;
        }

        public void setAtencionId(Long atencionId) {
            this.atencionId = atencionId;
        }

        public Long getMedicamentoId() {
            return medicamentoId;
        }

        public void setMedicamentoId(Long medicamentoId) {
            this.medicamentoId = medicamentoId;
        }

        public TipoRegistro getTipoRegistro() {
            return tipoRegistro;
        }

        public void setTipoRegistro(TipoRegistro tipoRegistro) {
            this.tipoRegistro = tipoRegistro;
        }

        public BigDecimal getCantidad() {
            return cantidad;
        }

        public void setCantidad(BigDecimal cantidad) {
            this.cantidad = cantidad;
        }

        public String getIndicaciones() {
            return indicaciones;
        }

        public void setIndicaciones(String indicaciones) {
            this.indicaciones = indicaciones;
        }

    }

    // ======================================================
    // 12. DTO DE RESPUESTA
    // ======================================================
    //
    // Devuelve los datos del detalle sin incluir
    // todas las entidades relacionadas.
    //
    // Guarda los precios historicos y cantidades
    // necesarias para la futura facturacion.
    //
    // ======================================================

    public record DetalleMedicamentoRespuesta(

            Long id,

            Long atencionId,

            Long medicamentoId,

            TipoRegistro tipoRegistro,

            String nombreMedicamento,

            String unidadMedida,

            BigDecimal cantidad,

            BigDecimal precioUnitarioAplicado,

            BigDecimal subtotal,

            String indicaciones,

            LocalDateTime fechaRegistro

    ) {

        // ==================================================
        // CONVERTIR ENTIDAD EN RESPUESTA
        // ==================================================

        public static DetalleMedicamentoRespuesta desde(
                DetalleMedicamentoAtencion detalle
        ) {

            return new DetalleMedicamentoRespuesta(

                    detalle.getId(),

                    detalle.getAtencionMedica().getId(),

                    detalle.getMedicamento().getId(),

                    detalle.getTipoRegistro(),

                    detalle.getNombreMedicamento(),

                    detalle.getUnidadMedida(),

                    detalle.getCantidad(),

                    detalle.getPrecioUnitarioAplicado(),

                    detalle.getSubtotal(),

                    detalle.getIndicaciones(),

                    detalle.getFechaRegistro()

            );

        }

    }

}
