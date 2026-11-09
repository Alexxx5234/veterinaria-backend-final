
package com.veterinaria.veterinaria.service;

// ==========================================================
// VETERINARIA AREVALO
// SERVICIO DE CORREOS ELECTRONICOS
//
// FUNCIONALIDADES:
// 1. Enviar codigos de verificacion.
// 2. Enviar correos con HTML.
// 3. Utilizar Gmail API por HTTPS en Railway.
// 4. Conservar SMTP para desarrollo local.
// 5. Mantener la compatibilidad con UsuarioService.
// 6. Registrar errores sin revelar codigos ni credenciales.
// ==========================================================

import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    // ======================================================
    // 1. REGISTRO DE EVENTOS
    // ======================================================

    private static final Logger log =
            LoggerFactory.getLogger(EmailService.class);


    // ======================================================
    // 2. SERVICIOS DE ENVIO
    // ======================================================

    // SMTP para trabajar localmente.
    private final JavaMailSender mailSender;

    // Gmail API mediante HTTPS para Railway.
    private final GmailApiSender gmailApiSender;


    // ======================================================
    // 3. CORREO REMITENTE
    // ======================================================

    private final String correoRemitente;


    // ======================================================
    // 4. CONSTRUCTOR
    // ======================================================

    public EmailService(
            JavaMailSender mailSender,
            GmailApiSender gmailApiSender,
            @Value("${spring.mail.username:}")
            String correoRemitente
    ) {

        this.mailSender = mailSender;

        this.gmailApiSender = gmailApiSender;

        this.correoRemitente =
                correoRemitente == null
                        ? ""
                        : correoRemitente.trim();
    }


    // ======================================================
    // 5. ENVIAR CODIGO DE VERIFICACION
    // ======================================================

    // Se conserva el metodo original utilizado por
    // UsuarioService para el registro de usuarios.

    public void enviarCodigoVerificacion(
            String correo,
            String codigo
    ) {

        // ==================================================
        // PASO 1. VALIDAR DESTINATARIO
        // ==================================================

        if (correo == null || correo.isBlank()) {

            throw new IllegalArgumentException(
                    "El correo del destinatario es obligatorio"
            );
        }


        // ==================================================
        // PASO 2. VALIDAR CODIGO
        // ==================================================

        if (codigo == null || codigo.isBlank()) {

            throw new IllegalArgumentException(
                    "El codigo de verificacion es obligatorio"
            );
        }


        // ==================================================
        // PASO 3. VALIDAR REMITENTE
        // ==================================================

        if (correoRemitente.isBlank()) {

            log.error(
                    "No se configuro spring.mail.username"
            );

            throw new IllegalStateException(
                    "No esta configurada la cuenta remitente"
            );
        }


        // ==================================================
        // PASO 4. PREPARAR DATOS
        // ==================================================

        String destinatario = correo.trim();

        String codigoVerificacion = codigo.trim();


        // ==================================================
        // PASO 5. CONSTRUIR CORREO HTML
        // ==================================================

        String contenidoHtml = """
            <!DOCTYPE html>
            <html lang="es">

            <head>
                <meta charset="UTF-8">
                <meta name="viewport"
                      content="width=device-width, initial-scale=1">

                <title>
                    Verificación - Veterinaria Arévalo
                </title>
            </head>

            <body style="
                margin:0;
                padding:24px;
                background:#f3faf6;
                font-family:Arial,sans-serif;
                color:#183d33;
            ">

                <div style="
                    max-width:600px;
                    margin:0 auto;
                    background:#ffffff;
                    border:1px solid #dcece2;
                    border-radius:16px;
                    overflow:hidden;
                ">

                    <div style="
                        background:#08745a;
                        color:#ffffff;
                        text-align:center;
                        padding:30px 20px;
                    ">

                        <h1 style="
                            margin:0;
                            font-size:26px;
                        ">
                            🐾 Veterinaria Arévalo
                        </h1>

                        <p style="
                            margin:12px 0 0;
                            font-size:14px;
                        ">
                            Cuidado y bienestar animal
                        </p>

                    </div>

                    <div style="
                        padding:32px 26px;
                    ">

                        <h2 style="
                            color:#075e47;
                            margin-top:0;
                        ">
                            ¡Bienvenido!
                        </h2>

                        <p style="
                            font-size:15px;
                            line-height:1.7;
                        ">
                            Gracias por registrarte en
                            <strong>
                                Veterinaria Arévalo
                            </strong>.
                        </p>

                        <p style="
                            font-size:15px;
                            line-height:1.7;
                        ">
                            Para verificar tu dirección
                            de correo electrónico,
                            introduce el siguiente código
                            en nuestra plataforma:
                        </p>

                        <div style="
                            text-align:center;
                            margin:30px 0;
                        ">

                            <div style="
                                display:inline-block;
                                padding:20px 30px;
                                background:#e1f5e9;
                                border:2px solid #08745a;
                                border-radius:12px;
                                color:#075e47;
                                font-size:34px;
                                font-weight:bold;
                                letter-spacing:8px;
                            ">
                                %s
                            </div>

                        </div>

                        <p style="
                            font-size:14px;
                            line-height:1.7;
                        ">
                            Después de verificar tu correo,
                            tu cuenta deberá ser aprobada
                            por el administrador.
                        </p>

                        <p style="
                            font-size:13px;
                            color:#71877c;
                            line-height:1.7;
                        ">
                            Si no solicitaste este registro,
                            puedes ignorar este mensaje.
                        </p>

                    </div>

                    <div style="
                        padding:20px;
                        background:#f0f9f4;
                        text-align:center;
                        border-top:1px solid #dcece2;
                        color:#688777;
                        font-size:12px;
                    ">

                        © 2026 Veterinaria Arévalo

                        <p style="margin:8px 0 0;">
                            Sistema de gestión veterinaria
                        </p>

                    </div>

                </div>

            </body>
            </html>
            """.formatted(codigoVerificacion);


        // ==================================================
        // PASO 6. VERSION DE TEXTO PLANO
        // ==================================================

        String contenidoTexto = """
                Veterinaria Arévalo

                Gracias por registrarte.

                Tu código de verificación es:

                %s

                Después de verificar tu correo,
                deberás esperar la aprobación
                del administrador.

                Si no solicitaste este registro,
                ignora el mensaje.
                """.formatted(codigoVerificacion);


        // ==================================================
        // PASO 7. PREPARAR Y ENVIAR CORREO
        // ==================================================

        try {

            // Crear mensaje compatible con Gmail API y SMTP.
            MimeMessage mensaje =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            mensaje,
                            true,
                            "UTF-8"
                    );

            // Correo remitente autorizado.
            helper.setFrom(correoRemitente);

            // Destinatario.
            helper.setTo(destinatario);

            // Asunto.
            helper.setSubject(
                    "Código de verificación - Veterinaria Arévalo"
            );

            // Contenido HTML y texto plano.
            helper.setText(
                    contenidoTexto,
                    contenidoHtml
            );


            // ==================================================
            // PASO 8. SELECCIONAR PROVEEDOR
            // ==================================================

            if (gmailApiSender.estaHabilitado()) {

                // Railway:
                // Envio mediante Gmail API por HTTPS.

                gmailApiSender.enviar(mensaje);

                log.info(
                        "Gmail API acepto el mensaje de verificacion"
                );

            } else {

                // Desarrollo local:
                // Conservamos el servidor SMTP anterior.

                mailSender.send(mensaje);

                log.info(
                        "SMTP acepto el mensaje de verificacion"
                );
            }

            // La aceptacion del proveedor no garantiza
            // la entrega final en la bandeja de entrada.

        } catch (Exception e) {

            // No registrar codigos de verificacion
            // ni contrasenas.

            log.error(
                    "Fallo el envio del correo de verificacion",
                    e
            );

            throw new IllegalStateException(
                    "No se pudo enviar el correo de verificacion",
                    e
            );
        }
    }
}
