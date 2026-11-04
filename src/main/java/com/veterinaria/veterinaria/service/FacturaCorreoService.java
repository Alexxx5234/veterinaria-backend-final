package com.veterinaria.veterinaria.service;

// ==========================================================
// IMPORTACIONES DE JAVA
// ==========================================================
import java.time.LocalDateTime;

// ==========================================================
// IMPORTACIONES DE CORREO
// ==========================================================
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

// ==========================================================
// IMPORTACIONES DE SPRING Y JPA
// ==========================================================
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

// ==========================================================
// IMPORTACIONES DEL PROYECTO
// ==========================================================
import com.veterinaria.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.veterinaria.model.Factura;
import com.veterinaria.veterinaria.model.Factura.EstadoFactura;
import com.veterinaria.veterinaria.repository.FacturaRepository;
import com.veterinaria.veterinaria.service.FacturaService.FacturaRespuesta;

// ==========================================================
// PASO 65 - ENVIO DE LA FACTURA EN PDF POR CORREO
// ==========================================================
// 1. El VETERINARIO solicita expresamente el envio.
// 2. Se valida que sea responsable de esa atencion.
// 3. Se verifica que la factura este EMITIDA.
// 4. Se genera el PDF existente, sin recalcular precios.
// 5. Se adjunta el PDF al correo del propietario.
// 6. Solo si SMTP acepta el mensaje se marca correoEnviado.
//
// IMPORTANTE:
// - Descargar el PDF NO envia un correo automaticamente.
// - El destinatario sale de la factura, nunca del JSON.
// - No permite enviar dos veces el mismo comprobante.
// - correoEnviado indica aceptacion del servidor SMTP;
//   no garantiza la entrega final en la bandeja de entrada.
// - Se trata de un comprobante interno, no autorizado SRI.
// ==========================================================
@Service
public class FacturaCorreoService {

    // ======================================================
    // 1. DEPENDENCIAS
    // ======================================================
    private final FacturaService facturaService;
    private final FacturaPdfService facturaPdfService;
    private final FacturaRepository facturaRepository;
    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;
    private final EntityManager entityManager;

    // Cuenta configurada en application.properties.
    // Si no esta configurada, se deja que JavaMailSender
    // utilice su remitente predeterminado.
    @Value("${spring.mail.username:}")
    private String correoRemitente;

    // ======================================================
    // 2. CONSTRUCTOR
    // ======================================================
    public FacturaCorreoService(
            FacturaService facturaService,
            FacturaPdfService facturaPdfService,
            FacturaRepository facturaRepository,
            ObjectProvider<JavaMailSender> javaMailSenderProvider,
            EntityManager entityManager
    ) {
        this.facturaService = facturaService;
        this.facturaPdfService = facturaPdfService;
        this.facturaRepository = facturaRepository;
        this.javaMailSenderProvider = javaMailSenderProvider;
        this.entityManager = entityManager;
    }

    // ======================================================
    // 3. ENVIAR EL PDF DE UNA FACTURA EMITIDA
    // ======================================================
    // Solo el veterinario asignado puede ejecutar el envio.
    // El bloqueo evita solicitudes simultaneas para una
    // misma factura mientras se realiza la operacion.
    //
    // NOTA: Esta implementacion sencilla mantiene el bloqueo
    // durante el envio SMTP. Para produccion se recomienda
    // implementar una cola de trabajos / patron outbox.
    // ======================================================
    @PreAuthorize("hasRole('VETERINARIO')")
    @Transactional
    public ResultadoEnvioFactura enviarFacturaPorCorreo(
            Long facturaId,
            String correoAutenticado
    ) {
        // 3.1. Validar el identificador solicitado.
        if (facturaId == null || facturaId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El ID de la factura debe ser positivo"
            );
        }

        // 3.2. Bloquear la factura contra dos envios paralelos.
        Factura factura = entityManager.find(
                Factura.class,
                facturaId,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (factura == null) {
            throw new ResourceNotFoundException(
                    "Factura no encontrada con id: " + facturaId
            );
        }

        // 3.3. Validar con el Service EXISTENTE que el JWT
        // pertenezca al veterinario responsable de la cita.
        // No se elimina ni se modifica la seguridad previa.
        FacturaRespuesta datos = facturaService.buscarPorId(
                facturaId,
                correoAutenticado
        );

        // 3.4. Solo pueden enviarse facturas emitidas.
        if (factura.getEstado() != EstadoFactura.EMITIDA) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Primero debes emitir la factura"
            );
        }

        // 3.5. No duplicar un mensaje ya aceptado por SMTP.
        if (factura.isCorreoEnviado()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Esta factura ya fue enviada por correo"
            );
        }

        // 3.6. El correo del destinatario se obtiene de
        // la factura guardada, nunca del cliente HTTP.
        String destinatario = factura.getCorreoCliente();
        validarDestinatario(destinatario);
        destinatario = destinatario.trim();

        // 3.7. Verificar que SMTP este configurado en Spring.
        // La API puede arrancar aunque aun no exista un emisor.
        JavaMailSender javaMailSender = javaMailSenderProvider.getIfAvailable();
        if (javaMailSender == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No hay servicio de correo SMTP configurado. "
                    + "Revisa spring.mail.host en application.properties"
            );
        }

        // 3.8. Generar el PDF mediante el Service existente.
        // Tambien vuelve a verificar los permisos del JWT.
        byte[] contenidoPdf = facturaPdfService.generarPdf(
                facturaId,
                correoAutenticado
        );

        // 3.9. Construir y enviar el mensaje con adjunto.
        try {
            MimeMessage mensaje = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mensaje,
                    true,
                    "UTF-8"
            );

            if (correoRemitente != null && !correoRemitente.isBlank()) {
                helper.setFrom(correoRemitente.trim());
            }

            helper.setTo(destinatario);
            helper.setSubject(
                    "Comprobante de atencion veterinaria - "
                    + datos.numeroFactura()
            );

            // Mensaje sencillo y entendible para el cliente.
            String textoCorreo =
                    "Estimado/a " + datos.nombreCliente() + ",\n\n"
                    + "Adjuntamos el comprobante interno correspondiente "
                    + "a la atencion de su mascota "
                    + datos.nombreMascota() + ".\n\n"
                    + "Numero de comprobante: "
                    + datos.numeroFactura() + "\n\n"
                    + "Este documento es un comprobante interno y no "
                    + "sustituye una factura autorizada por el SRI.\n\n"
                    + "Atentamente,\nVeterinaria";

            helper.setText(textoCorreo, false);

            // Se adjuntan los mismos bytes descargables de Swagger.
            helper.addAttachment(
                    "comprobante-interno-" + facturaId + ".pdf",
                    new ByteArrayResource(contenidoPdf),
                    "application/pdf"
            );

            // Si el servidor SMTP rechaza el mensaje, lanzara
            // una excepcion y NO se marcara como enviado.
            javaMailSender.send(mensaje);

        } catch (MessagingException | MailException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo enviar el PDF por correo. "
                    + "Revisa la configuracion SMTP y el registro del servidor",
                    ex
            );
        }

        // 3.10. Registrar el envio solo tras aceptacion SMTP.
        LocalDateTime fechaEnvio = LocalDateTime.now();
        factura.setCorreoEnviado(true);
        factura.setFechaEnvioCorreo(fechaEnvio);
        facturaRepository.saveAndFlush(factura);

        // 3.11. Devolver el resultado al Controller / Swagger.
        return new ResultadoEnvioFactura(
                factura.getId(),
                factura.getNumeroFactura(),
                destinatario,
                true,
                fechaEnvio,
                "El servidor de correo acepto el mensaje con el PDF adjunto"
        );
    }

    // ======================================================
    // 4. VERIFICAR EL CORREO DESTINATARIO
    // ======================================================
    private void validarDestinatario(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La factura no tiene un correo de propietario registrado"
            );
        }

        try {
            InternetAddress direccion = new InternetAddress(correo.trim(), true);
            direccion.validate();
        } catch (AddressException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El correo del propietario no tiene un formato valido"
            );
        }
    }

    // ======================================================
    // 5. RESPUESTA DEL ENVIO PARA SWAGGER Y ANGULAR
    // ======================================================
    public record ResultadoEnvioFactura(
            Long facturaId,
            String numeroFactura,
            String correoDestinatario,
            boolean correoEnviado,
            LocalDateTime fechaEnvioCorreo,
            String mensaje
    ) {}
}
