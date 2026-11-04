package com.veterinaria.veterinaria.controller;

// ==========================================================
// IMPORTACIONES DE JAVA
// ==========================================================

// Permite devolver listas de facturas en formato JSON.
import java.util.List;

// ==========================================================
// IMPORTACIONES DE SPRING BOOT Y SEGURIDAD
// ==========================================================

// Define el codigo HTTP que devolvera la API.
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

// Permite controlar el codigo y el contenido de la respuesta.
import org.springframework.http.ResponseEntity;

// Verifica los roles autorizados para cada endpoint.
import org.springframework.security.access.prepost.PreAuthorize;

// Contiene la identidad del usuario que inicio sesion con JWT.
import org.springframework.security.core.Authentication;

// Define las rutas REST para el frontend y Swagger.
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Permite responder con errores HTTP controlados.
import org.springframework.web.server.ResponseStatusException;

// ==========================================================
// DOCUMENTACION PARA SWAGGER
// ==========================================================

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

// ==========================================================
// SERVICIO Y DTOs DE FACTURACION
// ==========================================================

import com.veterinaria.veterinaria.service.FacturaCorreoService;
import com.veterinaria.veterinaria.service.FacturaCorreoService.ResultadoEnvioFactura;
import com.veterinaria.veterinaria.service.FacturaPdfService;
import com.veterinaria.veterinaria.service.FacturaService;
import com.veterinaria.veterinaria.service.FacturaService.FacturaRespuesta;
import com.veterinaria.veterinaria.service.FacturaService.VistaPreviaFactura;

// ==========================================================
// PASO 66 - CONTROLADOR DE FACTURAS
// ==========================================================
//
// OBJETIVO:
// Permitir consultar, previsualizar y emitir facturas internas
// a partir de una atencion medica previamente completada.
//
// FLUJO DEL SISTEMA:
//
// 1. El veterinario completa la atencion medica.
// 2. Registra los medicamentos utilizados o entregados.
// 3. Abre la vista previa de factura desde Angular.
// 4. Revisa los datos recuperados automaticamente.
// 5. Emite la factura sin volver a escoger los conceptos.
// 6. Consulta el comprobante emitido.
//
// PERMISOS:
// - VETERINARIO: vista previa y emision de sus atenciones.
// - ADMIN: vista previa y consulta de todas las facturas.
// - USER: consulta solo facturas de sus mascotas.
//
// SEGURIDAD:
// El correo del usuario se obtiene desde su autenticacion JWT.
// Nunca se recibe el correo o el veterinarioId desde el JSON.
// FacturaService vuelve a validar permisos y propiedad.
//
// IMPORTANTE:
// PASO 64: permite descargar el PDF de una factura EMITIDA.
// El envio por correo se implementara en el siguiente paso.
// Las facturas son comprobantes internos del proyecto.
// ==========================================================

@RestController
@RequestMapping("/api/facturas")
public class FacturaController {

    // ======================================================
    // 1. SERVICIOS DE FACTURACION
    // ======================================================

    private final FacturaService facturaService;

    // Servicio que construye el PDF de la factura sin modificarla.
    private final FacturaPdfService facturaPdfService;

    // Servicio que envia el PDF al correo del propietario.
    private final FacturaCorreoService facturaCorreoService;

    // ======================================================
    // 2. CONSTRUCTOR: INYECCION DE DEPENDENCIAS
    // ======================================================

    public FacturaController(
            FacturaService facturaService,
            FacturaPdfService facturaPdfService,
            FacturaCorreoService facturaCorreoService
    ) {
        this.facturaService = facturaService;
        this.facturaPdfService = facturaPdfService;
        this.facturaCorreoService = facturaCorreoService;
    }

    // ======================================================
    // 3. PREVISUALIZAR FACTURA SIN GUARDARLA
    // ======================================================
    // GET /api/facturas/previsualizar/{atencionId}
    //
    // - ADMIN consulta cualquier atencion.
    // - VETERINARIO solo consulta sus propias atenciones.
    // - No registra una factura ni descuenta stock.
    // - Recupera servicio, propietario, mascota y medicamentos.
    // ======================================================

    @Operation(
        summary = "Previsualizar factura de una atencion",
        description = "Calcula la factura con el servicio y los medicamentos "
                + "registrados, sin crearla en la base de datos. "
                + "Disponible para ADMIN y el VETERINARIO asignado."
    )
    @ApiResponse(responseCode = "200", description = "Vista previa calculada")
    @ApiResponse(responseCode = "401", description = "Usuario sin autenticar")
    @ApiResponse(responseCode = "403", description = "No tiene permiso para esta atencion")
    @ApiResponse(responseCode = "404", description = "Atencion no encontrada")
    @ApiResponse(responseCode = "409", description = "Cita o datos incompletos")
    @GetMapping("/previsualizar/{atencionId}")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO')")
    public ResponseEntity<VistaPreviaFactura> previsualizar(
            @PathVariable Long atencionId,
            Authentication authentication
    ) {
        String correo = obtenerCorreoAutenticado(authentication);

        VistaPreviaFactura vista = facturaService.previsualizar(
                atencionId,
                correo
        );

        return ResponseEntity.ok(vista);
    }

    // ======================================================
    // 4. EMITIR FACTURA DE UNA ATENCION
    // ======================================================
    // POST /api/facturas/emitir/{atencionId}
    //
    // - Solamente puede hacerlo el VETERINARIO asignado.
    // - No necesita RequestBody porque todos los datos
    //   se recuperan de la atencion medica existente.
    // - Genera un numero interno de comprobante.
    // - Impide duplicar la factura de la misma atencion.
    // - Retorna HTTP 201 si se guarda correctamente.
    // ======================================================

    @Operation(
        summary = "Emitir factura de una atencion",
        description = "Emite una factura interna para una atencion "
                + "completada del veterinario autenticado. "
                + "Calcula servicio y medicamentos automaticamente. "
                + "No requiere datos adicionales en el cuerpo de la peticion."
    )
    @ApiResponse(responseCode = "201", description = "Factura emitida correctamente")
    @ApiResponse(responseCode = "401", description = "Usuario sin autenticar")
    @ApiResponse(responseCode = "403", description = "No es el veterinario asignado")
    @ApiResponse(responseCode = "404", description = "Atencion no encontrada")
    @ApiResponse(responseCode = "409", description = "Factura duplicada o datos invalidos")
    @PostMapping("/emitir/{atencionId}")
    @PreAuthorize("hasRole('VETERINARIO')")
    public ResponseEntity<FacturaRespuesta> emitir(
            @PathVariable Long atencionId,
            Authentication authentication
    ) {
        String correo = obtenerCorreoAutenticado(authentication);

        FacturaRespuesta factura = facturaService.emitir(
                atencionId,
                correo
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(factura);
    }

    // ======================================================
    // 5. CONSULTAR LAS FACTURAS AUTORIZADAS
    // ======================================================
    // GET /api/facturas/mis-facturas
    //
    // - ADMIN: devuelve todas las facturas.
    // - VETERINARIO: devuelve las de sus atenciones.
    // - USER: devuelve las facturas de sus mascotas.
    // ======================================================

    @Operation(
        summary = "Consultar mis facturas",
        description = "Lista las facturas permitidas segun el rol "
                + "del usuario autenticado con JWT."
    )
    @ApiResponse(responseCode = "200", description = "Lista de facturas autorizadas")
    @ApiResponse(responseCode = "401", description = "Usuario sin autenticar")
    @ApiResponse(responseCode = "403", description = "Acceso no permitido")
    @GetMapping("/mis-facturas")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','USER')")
    public ResponseEntity<List<FacturaRespuesta>> listarMisFacturas(
            Authentication authentication
    ) {
        String correo = obtenerCorreoAutenticado(authentication);

        List<FacturaRespuesta> facturas =
                facturaService.listarMisFacturas(correo);

        return ResponseEntity.ok(facturas);
    }

    // ======================================================
    // 6. LISTAR TODAS LAS FACTURAS (ADMIN)
    // ======================================================
    // GET /api/facturas
    //
    // Solo ADMIN consulta la totalidad de los comprobantes.
    // ======================================================

    @Operation(
        summary = "Listar todas las facturas",
        description = "Consulta administrativa de todas las facturas "
                + "internas, ordenadas de la mas reciente a la mas antigua."
    )
    @ApiResponse(responseCode = "200", description = "Lista general de facturas")
    @ApiResponse(responseCode = "403", description = "Solo ADMIN")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<FacturaRespuesta>> listarTodas() {
        return ResponseEntity.ok(facturaService.listarTodas());
    }

    // ======================================================
    // 7. CONSULTAR FACTURA POR ATENCION MEDICA
    // ======================================================
    // GET /api/facturas/atencion/{atencionId}
    //
    // Permite que Angular abra la factura ya emitida.
    // Valida que el usuario tenga acceso a esa atencion.
    // ======================================================

    @Operation(
        summary = "Consultar factura por atencion medica",
        description = "Recupera la factura existente de una atencion "
                + "y verifica que el usuario pueda acceder a ella."
    )
    @ApiResponse(responseCode = "200", description = "Factura encontrada")
    @ApiResponse(responseCode = "403", description = "Sin permiso")
    @ApiResponse(responseCode = "404", description = "Factura o atencion no encontrada")
    @GetMapping("/atencion/{atencionId}")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','USER')")
    public ResponseEntity<FacturaRespuesta> buscarPorAtencion(
            @PathVariable Long atencionId,
            Authentication authentication
    ) {
        String correo = obtenerCorreoAutenticado(authentication);

        FacturaRespuesta factura = facturaService.buscarPorAtencion(
                atencionId,
                correo
        );

        return ResponseEntity.ok(factura);
    }

    // ======================================================
    // PASO 64. DESCARGAR LA FACTURA EMITIDA EN PDF
    // ======================================================
    // GET /api/facturas/{facturaId}/pdf
    //
    // - ADMIN: puede descargar cualquier factura.
    // - VETERINARIO: solo las facturas de sus atenciones.
    // - USER: solo las facturas de sus propias mascotas.
    //
    // FacturaPdfService consulta FacturaService.buscarPorId(),
    // que verifica la autorizacion sobre la factura concreta.
    //
    // No se recibe el correo ni el usuario desde la URL:
    // la identidad siempre se obtiene del JWT.
    //
    // El PDF se devuelve en memoria como archivo adjunto.
    // No se vuelve a emitir la factura, no se cambian precios
    // ni se envian mensajes de correo en este endpoint.
    // ======================================================

    @Operation(
        summary = "Descargar PDF de una factura emitida",
        description = "Genera el comprobante interno PDF de una factura "
                + "EMITIDA. Solo pueden descargarlo ADMIN, el "
                + "VETERINARIO responsable o el propietario de la mascota. "
                + "Este documento no sustituye una factura autorizada por el SRI."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Documento PDF listo para descargar",
        content = @Content(
            mediaType = "application/pdf",
            schema = @Schema(type = "string", format = "binary")
        )
    )
    @ApiResponse(responseCode = "401", description = "Usuario sin autenticar")
    @ApiResponse(responseCode = "403", description = "Sin permiso para esta factura")
    @ApiResponse(responseCode = "404", description = "Factura no encontrada")
    @ApiResponse(responseCode = "409", description = "La factura aun no ha sido emitida")
    @ApiResponse(responseCode = "500", description = "Error al generar el PDF")
    @GetMapping(value = "/{facturaId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','USER')")
    public ResponseEntity<byte[]> descargarPdf(
            @PathVariable Long facturaId,
            Authentication authentication
    ) {
        // Correo obtenido del usuario autenticado mediante JWT.
        String correo = obtenerCorreoAutenticado(authentication);

        // El servicio valida propiedad, existencia y estado EMITIDA.
        byte[] contenidoPdf = facturaPdfService.generarPdf(
                facturaId,
                correo
        );

        // Nombre seguro: solamente utiliza el identificador numerico.
        String nombreArchivo = "comprobante-interno-" + facturaId + ".pdf";

        // Devolver el PDF para descarga, sin escribir archivos
        // temporales en el servidor y sin exponer datos de terceros.
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + nombreArchivo + "\""
                )
                .contentLength(contenidoPdf.length)
                .body(contenidoPdf);
    }

    // ======================================================
    // PASO 66. ENVIAR POR CORREO EL PDF DE LA FACTURA
    // ======================================================
    // POST /api/facturas/{facturaId}/enviar-correo
    //
    // - Solo el VETERINARIO responsable puede solicitarlo.
    // - La factura debe encontrarse EMITIDA.
    // - El correo destinatario sale de la factura guardada.
    // - No es necesario enviar ningun JSON en el cuerpo.
    // - FacturaCorreoService genera y adjunta el PDF.
    // - La factura se marca como enviada solo despues de que
    //   el servidor SMTP acepte el mensaje.
    //
    // Una aceptacion SMTP no garantiza que el mensaje
    // aparezca inmediatamente en la bandeja de entrada.
    // ======================================================

    @Operation(
        summary = "Enviar PDF de factura al correo del propietario",
        description = "Permite que el VETERINARIO responsable envie el PDF "
                + "de una factura EMITIDA al correo del propietario, "
                + "recuperado automaticamente de los datos de la factura. "
                + "No requiere cuerpo JSON ni permite elegir destinatario."
    )
    @ApiResponse(
        responseCode = "200",
        description = "El servidor SMTP acepto el correo con el PDF adjunto"
    )
    @ApiResponse(responseCode = "401", description = "Usuario sin autenticar")
    @ApiResponse(responseCode = "403", description = "No es el veterinario responsable")
    @ApiResponse(responseCode = "404", description = "Factura no encontrada")
    @ApiResponse(responseCode = "409", description = "Factura no emitida o correo ya enviado")
    @ApiResponse(responseCode = "503", description = "No se pudo utilizar el servicio SMTP")
    @PostMapping("/{facturaId}/enviar-correo")
    @PreAuthorize("hasRole('VETERINARIO')")
    public ResponseEntity<ResultadoEnvioFactura> enviarCorreo(
            @PathVariable Long facturaId,
            Authentication authentication
    ) {
        // La identidad proviene del token JWT, no del cliente.
        String correo = obtenerCorreoAutenticado(authentication);

        // El Service valida propietario, estado y configuracion SMTP.
        ResultadoEnvioFactura resultado =
                facturaCorreoService.enviarFacturaPorCorreo(
                        facturaId,
                        correo
                );

        // 200 OK: servidor SMTP acepto el envio.
        return ResponseEntity.ok(resultado);
    }

    // ======================================================
    // 8. CONSULTAR FACTURA POR SU ID
    // ======================================================
    // GET /api/facturas/{facturaId}
    //
    // Permisos evaluados en FacturaService:
    // ADMIN: cualquiera.
    // VETERINARIO: de sus atenciones.
    // USER: de sus mascotas.
    // ======================================================

    @Operation(
        summary = "Consultar factura por ID",
        description = "Devuelve los datos de una factura "
                + "si el usuario autenticado tiene permisos."
    )
    @ApiResponse(responseCode = "200", description = "Factura encontrada")
    @ApiResponse(responseCode = "403", description = "Sin permiso")
    @ApiResponse(responseCode = "404", description = "Factura no encontrada")
    @GetMapping("/{facturaId}")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','USER')")
    public ResponseEntity<FacturaRespuesta> buscarPorId(
            @PathVariable Long facturaId,
            Authentication authentication
    ) {
        String correo = obtenerCorreoAutenticado(authentication);

        FacturaRespuesta factura = facturaService.buscarPorId(
                facturaId,
                correo
        );

        return ResponseEntity.ok(factura);
    }

    // ======================================================
    // 9. OBTENER EL CORREO DEL JWT
    // ======================================================
    // No recibe el correo desde formularios o URLs para evitar
    // que un usuario consulte datos de otro propietario.
    // ======================================================

    private String obtenerCorreoAutenticado(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()
                || "anonymousUser".equals(authentication.getName())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Debes iniciar sesion para consultar facturas"
            );
        }

        return authentication.getName();
    }
}
