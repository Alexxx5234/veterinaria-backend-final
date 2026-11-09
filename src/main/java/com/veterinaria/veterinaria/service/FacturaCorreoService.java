
package com.veterinaria.veterinaria.service;

// ==========================================================
// VETERINARIA AREVALO
// ENVIO DE FACTURAS PDF POR CORREO ELECTRONICO
//
// FUNCIONALIDADES:
// 1. Permitir el envio al veterinario responsable.
// 2. Verificar que la factura este EMITIDA.
// 3. Impedir envios duplicados.
// 4. Generar el PDF existente.
// 5. Enviar mediante Gmail API en Railway.
// 6. Conservar SMTP para desarrollo local.
// 7. Registrar el envio solamente si el proveedor acepta.
// 8. Mantener los permisos y respuestas existentes.
//
// IMPORTANTE:
// El comprobante es interno y no sustituye una factura SRI.
// ==========================================================

import java.time.LocalDateTime;
import java.util.Properties;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.veterinaria.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.veterinaria.model.Factura;
import com.veterinaria.veterinaria.model.Factura.EstadoFactura;
import com.veterinaria.veterinaria.repository.FacturaRepository;
import com.veterinaria.veterinaria.service.FacturaService.FacturaRespuesta;


// ==========================================================
// SERVICIO DE ENVIO DE FACTURAS
// ==========================================================

@Service
public class FacturaCorreoService {

    // ======================================================
    // 1. DEPENDENCIAS
    // ======================================================

    private final FacturaService facturaService;

    private final FacturaPdfService facturaPdfService;

    private final FacturaRepository facturaRepository;

    private final ObjectProvider<JavaMailSender>
            javaMailSenderProvider;

    private final GmailApiSender gmailApiSender;

    private final EntityManager entityManager;


    // ======================================================
    // 2. CORREO REMITENTE
    // ======================================================

    @Value("${spring.mail.username:}")
    private String correoRemitente;


    // ======================================================
    // 3. CONSTRUCTOR
    // ======================================================

    public FacturaCorreoService(
            FacturaService facturaService,
            FacturaPdfService facturaPdfService,
            FacturaRepository facturaRepository,
            ObjectProvider<JavaMailSender> javaMailSenderProvider,
            GmailApiSender gmailApiSender,
            EntityManager entityManager
    ) {

        this.facturaService = facturaService;

        this.facturaPdfService = facturaPdfService;

        this.facturaRepository = facturaRepository;

        this.javaMailSenderProvider = javaMailSenderProvider;

        this.gmailApiSender = gmailApiSender;

        this.entityManager = entityManager;
    }


    // ======================================================
    // 4. ENVIAR FACTURA POR CORREO
    // ======================================================
    //
    // Solo el veterinario responsable puede enviar.
    // El metodo conserva la seguridad del proyecto.
    //
    // ======================================================

    @PreAuthorize("hasRole('VETERINARIO')")
    @Transactional
    public ResultadoEnvioFactura enviarFacturaPorCorreo(
            Long facturaId,
            String correoAutenticado
    ) {

        // ==================================================
        // PASO 1. VALIDAR ID
        // ==================================================

        if (facturaId == null || facturaId <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El ID de la factura debe ser positivo"
            );
        }


        // ==================================================
        // PASO 2. BLOQUEAR FACTURA
        // ==================================================
        //
        // Evita dos operaciones simultaneas de envio.
        //
        // ==================================================

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


        // ==================================================
        // PASO 3. VERIFICAR VETERINARIO RESPONSABLE
        // ==================================================

        FacturaRespuesta datos = facturaService.buscarPorId(
                facturaId,
                correoAutenticado
        );


        // ==================================================
        // PASO 4. VERIFICAR ESTADO
        // ==================================================

        if (factura.getEstado() != EstadoFactura.EMITIDA) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Primero debes emitir la factura"
            );
        }


        // ==================================================
        // PASO 5. EVITAR ENVIOS DUPLICADOS
        // ==================================================

        if (factura.isCorreoEnviado()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Esta factura ya fue enviada por correo"
            );
        }


        // ==================================================
        // PASO 6. OBTENER DESTINATARIO
        // ==================================================
        //
        // El correo viene de la factura almacenada.
        // No se acepta un destinatario enviado por Angular.
        //
        // ==================================================

        String destinatario = factura.getCorreoCliente();

        validarDestinatario(destinatario);

        destinatario = destinatario.trim();


        // ==================================================
        // PASO 7. SELECCIONAR PROVEEDOR
        // ==================================================

        boolean usarGmailApi = gmailApiSender.estaHabilitado();

        JavaMailSender javaMailSender =
                javaMailSenderProvider.getIfAvailable();


        // En desarrollo local necesitamos SMTP.
        if (!usarGmailApi && javaMailSender == null) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No existe un servicio SMTP configurado"
            );
        }


        // Gmail API requiere un remitente autorizado.
        if (usarGmailApi &&
                (correoRemitente == null ||
                 correoRemitente.isBlank())) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No esta configurado el remitente de Gmail API"
            );
        }


        // ==================================================
        // PASO 8. GENERAR PDF
        // ==================================================
        //
        // Utilizamos el servicio existente.
        // No modificamos precios ni datos de facturacion.
        //
        // ==================================================

        byte[] contenidoPdf = facturaPdfService.generarPdf(
                facturaId,
                correoAutenticado
        );


        // ==================================================
        // PASO 9. PREPARAR CORREO CON PDF ADJUNTO
        // ==================================================

        try {

            MimeMessage mensaje;

            if (javaMailSender != null) {

                mensaje = javaMailSender.createMimeMessage();

            } else {

                // Permite crear mensajes MIME aunque
                // solamente utilicemos Gmail API.

                Session sesion = Session.getInstance(
                        new Properties()
                );

                mensaje = new MimeMessage(sesion);
            }


            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            mensaje,
                            true,
                            "UTF-8"
                    );


            // ==================================================
            // REMITENTE
            // ==================================================

            if (correoRemitente != null &&
                    !correoRemitente.isBlank()) {

                helper.setFrom(correoRemitente.trim());
            }


            // ==================================================
            // DESTINATARIO
            // ==================================================

            helper.setTo(destinatario);


            // ==================================================
            // ASUNTO
            // ==================================================

            helper.setSubject(
                    "Comprobante de atencion veterinaria - "
                    + datos.numeroFactura()
            );


            // ==================================================
            // CONTENIDO DEL CORREO
            // ==================================================

            String textoCorreo =
                    "Estimado/a " + datos.nombreCliente()
                    + ",\n\n"

                    + "Adjuntamos el comprobante interno "
                    + "correspondiente a la atencion de su mascota "

                    + datos.nombreMascota()
                    + ".\n\n"

                    + "Numero de comprobante: "
                    + datos.numeroFactura()
                    + "\n\n"

                    + "Este documento es un comprobante interno "
                    + "y no sustituye una factura autorizada "
                    + "por el SRI.\n\n"

                    + "Atentamente,\n"
                    + "Veterinaria Arevalo";


            helper.setText(textoCorreo, false);


            // ==================================================
            // PASO 10. ADJUNTAR PDF
            // ==================================================

            helper.addAttachment(
                    "comprobante-interno-" + facturaId + ".pdf",
                    new ByteArrayResource(contenidoPdf),
                    "application/pdf"
            );


            // ==================================================
            // PASO 11. ENVIAR CORREO
            // ==================================================

            if (usarGmailApi) {

                // Produccion Railway:
                // Gmail API utiliza HTTPS.
                // Incluye el documento PDF adjunto.

                gmailApiSender.enviar(mensaje);

            } else {

                // Desarrollo local:
                // Conservamos SMTP.

                javaMailSender.send(mensaje);
            }


        } catch (
                MessagingException |
                MailException |
                IllegalStateException ex
        ) {

            // Si el proveedor rechaza el mensaje,
            // no marcamos la factura como enviada.

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo enviar el PDF por correo. "
                    + "Revisa el proveedor de correo "
                    + "y los registros del backend",
                    ex
            );
        }


        // ==================================================
        // PASO 12. REGISTRAR ENVIO EXITOSO
        // ==================================================
        //
        // Se registra despues de la aceptacion del proveedor.
        // Esto no garantiza entrega en bandeja de entrada.
        //
        // ==================================================

        LocalDateTime fechaEnvio = LocalDateTime.now();

        factura.setCorreoEnviado(true);

        factura.setFechaEnvioCorreo(fechaEnvio);

        facturaRepository.saveAndFlush(factura);


        // ==================================================
        // PASO 13. RESPUESTA A ANGULAR Y SWAGGER
        // ==================================================

        return new ResultadoEnvioFactura(
                factura.getId(),
                factura.getNumeroFactura(),
                destinatario,
                true,
                fechaEnvio,
                "El proveedor de correo acepto "
                + "el mensaje con el PDF adjunto"
        );
    }


    // ======================================================
    // 5. VALIDAR CORREO DEL DESTINATARIO
    // ======================================================

    private void validarDestinatario(String correo) {

        if (correo == null || correo.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La factura no tiene un correo "
                    + "de propietario registrado"
            );
        }

        try {

            InternetAddress direccion =
                    new InternetAddress(
                            correo.trim(),
                            true
                    );

            direccion.validate();

        } catch (AddressException ex) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El correo del propietario "
                    + "no tiene un formato valido"
            );
        }
    }


    // ======================================================
    // 6. RESPUESTA DEL ENVIO
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
