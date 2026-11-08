package com.veterinaria.veterinaria.controller;

// ==========================================================
// IMPORTACIONES
// ==========================================================

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import com.veterinaria.veterinaria.model.DetalleMedicamentoAtencion;
import com.veterinaria.veterinaria.model.DetalleMedicamentoAtencion.TipoRegistro;

import com.veterinaria.veterinaria.service.DetalleMedicamentoAtencionService;
import com.veterinaria.veterinaria.service.DetalleMedicamentoAtencionService.RegistroMedicamentoLoteItem;


// ==========================================================
// CONTROLADOR DE MEDICAMENTOS DE ATENCIONES
// VETERINARIA ARÉVALO
// ==========================================================
//
// FUNCIONES:
//
// 1. Registrar un medicamento.
// 2. Registrar varios medicamentos en una operación.
// 3. Consultar medicamentos por atención.
// 4. Consultar medicamentos por tipo.
// 5. Consultar medicamentos por mascota.
// 6. Consultar historial de un medicamento.
// 7. Buscar un detalle por ID.
//
// PERMISOS:
//
// ADMIN:
// - Consulta registros.
// - Administra catálogo e inventario desde su módulo.
//
// VETERINARIO:
// - Registra medicamentos en sus propias atenciones.
// - Consulta los registros autorizados.
//
// USER:
// - Consulta medicamentos de sus propias mascotas.
//
// ==========================================================

@RestController
@RequestMapping("/api/detalles-medicamentos-atencion")
public class DetalleMedicamentoAtencionController {

    // ======================================================
    // 1. DEPENDENCIA
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
    // 3. REGISTRAR UN MEDICAMENTO
    // ======================================================
    //
    // POST /api/detalles-medicamentos-atencion
    //
    // SOLO VETERINARIO.
    //
    // ======================================================

    @Operation(
        summary = "Registrar medicamento en una atención",
        description = "Registra un medicamento utilizado, recetado "
                + "o recetado y entregado en una atención médica."
    )
    @ApiResponse(
        responseCode = "201",
        description = "Medicamento registrado correctamente"
    )
    @ApiResponse(
        responseCode = "400",
        description = "Datos incorrectos"
    )
    @ApiResponse(
        responseCode = "403",
        description = "Sin permisos"
    )
    @ApiResponse(
        responseCode = "404",
        description = "Atención o medicamento no encontrado"
    )
    @ApiResponse(
        responseCode = "409",
        description = "Medicamento duplicado, inactivo o stock insuficiente"
    )
    @PostMapping
    @PreAuthorize("hasRole('VETERINARIO')")
    public ResponseEntity<DetalleMedicamentoRespuesta> registrar(
            @Valid @RequestBody RegistroMedicamentoRequest datos,
            Authentication authentication
    ) {

        String correo = obtenerCorreoAutenticado(authentication);

        DetalleMedicamentoAtencion detalle =
                detalleService.registrar(
                        datos.getAtencionId(),
                        datos.getMedicamentoId(),
                        datos.getTipoRegistro(),
                        datos.getCantidad(),
                        datos.getIndicaciones(),
                        correo
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(DetalleMedicamentoRespuesta.desde(detalle));

    }


    // ======================================================
    // 4. REGISTRAR VARIOS MEDICAMENTOS
    // ======================================================
    //
    // POST /api/detalles-medicamentos-atencion/lote
    //
    // SOLO VETERINARIO.
    //
    // Recibe una atención y una lista de medicamentos.
    //
    // El servicio registra todos los elementos
    // en una sola transacción.
    //
    // Si uno falla, se revierte toda la operación.
    //
    // ======================================================

    @Operation(
        summary = "Registrar varios medicamentos",
        description = "Registra hasta 30 medicamentos "
                + "en una sola transacción."
    )
    @ApiResponse(
        responseCode = "201",
        description = "Medicamentos registrados correctamente"
    )
    @ApiResponse(
        responseCode = "400",
        description = "Datos incompletos o incorrectos"
    )
    @ApiResponse(
        responseCode = "403",
        description = "Sin permisos"
    )
    @ApiResponse(
        responseCode = "409",
        description = "Duplicados, medicamentos inactivos o stock insuficiente"
    )
    @PostMapping("/lote")
    @PreAuthorize("hasRole('VETERINARIO')")
    public ResponseEntity<List<DetalleMedicamentoRespuesta>> registrarLote(
            @Valid @RequestBody RegistroMedicamentosLoteRequest datos,
            Authentication authentication
    ) {

        // Obtener correo desde el JWT.
        String correo = obtenerCorreoAutenticado(authentication);

        // Convertir los DTO recibidos al formato del servicio.
        List<RegistroMedicamentoLoteItem> items =
                datos.getMedicamentos()
                        .stream()
                        .map(item -> new RegistroMedicamentoLoteItem(
                                item.getMedicamentoId(),
                                item.getTipoRegistro(),
                                item.getCantidad(),
                                item.getIndicaciones()
                        ))
                        .toList();

        // Registrar todo el lote.
        List<DetalleMedicamentoAtencion> guardados =
                detalleService.registrarVarios(
                        datos.getAtencionId(),
                        items,
                        correo
                );

        // Devolver los registros guardados.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(convertirLista(guardados));

    }


    // ======================================================
    // 5. CONSULTAR MEDICAMENTOS DE UNA ATENCIÓN
    // ======================================================
    //
    // GET /api/detalles-medicamentos-atencion/atencion/{id}
    //
    // ADMIN, VETERINARIO y USER.
    //
    // El servicio comprueba los permisos.
    //
    // ======================================================

    @Operation(
        summary = "Consultar medicamentos de una atención",
        description = "Obtiene los medicamentos registrados "
                + "en una atención médica."
    )
    @GetMapping("/atencion/{atencionId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','VETERINARIO')")
    public ResponseEntity<List<DetalleMedicamentoRespuesta>> listarPorAtencion(
            @PathVariable Long atencionId,
            Authentication authentication
    ) {

        String correo = obtenerCorreoAutenticado(authentication);

        List<DetalleMedicamentoAtencion> detalles =
                detalleService.listarPorAtencion(
                        atencionId,
                        correo
                );

        return ResponseEntity.ok(convertirLista(detalles));

    }


    // ======================================================
    // 6. CONSULTAR MEDICAMENTOS POR TIPO
    // ======================================================
    //
    // GET /api/detalles-medicamentos-atencion/
    //     atencion/{id}/tipo/{tipoRegistro}
    //
    // Tipos:
    // UTILIZADO
    // RECETADO
    // RECETADO_ENTREGADO
    //
    // ======================================================

    @Operation(
        summary = "Consultar medicamentos por tipo",
        description = "Filtra los medicamentos de una atención "
                + "según su tipo de registro."
    )
    @GetMapping("/atencion/{atencionId}/tipo/{tipoRegistro}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','VETERINARIO')")
    public ResponseEntity<List<DetalleMedicamentoRespuesta>> listarPorTipo(
            @PathVariable Long atencionId,
            @PathVariable TipoRegistro tipoRegistro,
            Authentication authentication
    ) {

        String correo = obtenerCorreoAutenticado(authentication);

        List<DetalleMedicamentoAtencion> detalles =
                detalleService.listarPorAtencionYTipo(
                        atencionId,
                        tipoRegistro,
                        correo
                );

        return ResponseEntity.ok(convertirLista(detalles));

    }


    // ======================================================
    // 7. CONSULTAR MEDICAMENTOS DE UNA MASCOTA
    // ======================================================
    //
    // GET /api/detalles-medicamentos-atencion/mascota/{id}
    //
    // ======================================================

    @Operation(
        summary = "Consultar medicamentos de una mascota",
        description = "Obtiene el historial de medicamentos "
                + "de las atenciones de una mascota."
    )
    @GetMapping("/mascota/{mascotaId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','VETERINARIO')")
    public ResponseEntity<List<DetalleMedicamentoRespuesta>> listarPorMascota(
            @PathVariable Long mascotaId,
            Authentication authentication
    ) {

        String correo = obtenerCorreoAutenticado(authentication);

        List<DetalleMedicamentoAtencion> detalles =
                detalleService.listarPorMascota(
                        mascotaId,
                        correo
                );

        return ResponseEntity.ok(convertirLista(detalles));

    }


    // ======================================================
    // 8. HISTORIAL DE UN MEDICAMENTO
    // ======================================================
    //
    // GET /api/detalles-medicamentos-atencion/
    // medicamento/{medicamentoId}/historial
    //
    // SOLO ADMIN.
    //
    // ======================================================

    @Operation(
        summary = "Consultar historial de un medicamento",
        description = "Consulta las atenciones donde aparece "
                + "un medicamento determinado."
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

        return ResponseEntity.ok(convertirLista(detalles));

    }


    // ======================================================
    // 9. BUSCAR DETALLE POR ID
    // ======================================================
    //
    // GET /api/detalles-medicamentos-atencion/{id}
    //
    // ======================================================

    @Operation(
        summary = "Consultar detalle por ID",
        description = "Busca un registro de medicamento "
                + "comprobando los permisos del usuario."
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER','VETERINARIO')")
    public ResponseEntity<DetalleMedicamentoRespuesta> buscarPorId(
            @PathVariable Long id,
            Authentication authentication
    ) {

        String correo = obtenerCorreoAutenticado(authentication);

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
    // 10. OBTENER CORREO DEL USUARIO AUTENTICADO
    // ======================================================
    //
    // El correo procede de Spring Security.
    // No se acepta desde el JSON.
    //
    // ======================================================

    private String obtenerCorreoAutenticado(
            Authentication authentication
    ) {

        if (
                authentication == null
                || !authentication.isAuthenticated()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Debes iniciar sesion para continuar"
            );

        }

        return authentication.getName();

    }


    // ======================================================
    // 11. CONVERTIR LISTA DE ENTIDADES A DTO
    // ======================================================

    private List<DetalleMedicamentoRespuesta> convertirLista(
            List<DetalleMedicamentoAtencion> detalles
    ) {

        return detalles.stream()
                .map(DetalleMedicamentoRespuesta::desde)
                .toList();

    }


    // ======================================================
    // 12. DTO PARA REGISTRAR UN MEDICAMENTO
    // ======================================================

    public static class RegistroMedicamentoRequest {

        @NotNull(message = "La atencion medica es obligatoria")
        private Long atencionId;

        @NotNull(message = "El medicamento es obligatorio")
        private Long medicamentoId;

        @NotNull(message = "El tipo de registro es obligatorio")
        @Schema(
            description = "UTILIZADO, RECETADO o RECETADO_ENTREGADO",
            allowableValues = {
                "UTILIZADO",
                "RECETADO",
                "RECETADO_ENTREGADO"
            }
        )
        private TipoRegistro tipoRegistro;

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

        @Size(
            max = 1000,
            message = "Las indicaciones no pueden superar 1000 caracteres"
        )
        private String indicaciones;

        public RegistroMedicamentoRequest() {
        }

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
    // 13. DTO PARA REGISTRAR VARIOS MEDICAMENTOS
    // ======================================================

    public static class RegistroMedicamentosLoteRequest {

        @NotNull(message = "La atencion medica es obligatoria")
        private Long atencionId;

        @NotEmpty(message = "Agrega al menos un medicamento")
        @Size(
            max = 30,
            message = "Puedes agregar como maximo 30 medicamentos"
        )
        private List<
                @NotNull
                @Valid
                RegistroMedicamentoItemRequest
        > medicamentos = new ArrayList<>();

        public RegistroMedicamentosLoteRequest() {
        }

        public Long getAtencionId() {
            return atencionId;
        }

        public void setAtencionId(Long atencionId) {
            this.atencionId = atencionId;
        }

        public List<RegistroMedicamentoItemRequest> getMedicamentos() {
            return medicamentos;
        }

        public void setMedicamentos(
                List<RegistroMedicamentoItemRequest> medicamentos
        ) {
            this.medicamentos = medicamentos;
        }

    }


    // ======================================================
    // 14. DTO DE CADA MEDICAMENTO DEL LOTE
    // ======================================================

    public static class RegistroMedicamentoItemRequest {

        @NotNull(message = "El medicamento es obligatorio")
        private Long medicamentoId;

        @NotNull(message = "El tipo de registro es obligatorio")
        @Schema(
            allowableValues = {
                "UTILIZADO",
                "RECETADO",
                "RECETADO_ENTREGADO"
            }
        )
        private TipoRegistro tipoRegistro;

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

        @Size(
            max = 1000,
            message = "Las indicaciones no pueden superar 1000 caracteres"
        )
        private String indicaciones;

        public RegistroMedicamentoItemRequest() {
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
    // 15. DTO DE RESPUESTA
    // ======================================================
    //
    // No devolvemos directamente las entidades JPA.
    // Evitamos ciclos JSON y datos innecesarios.
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