
package com.veterinaria.veterinaria.service;

// ==========================================================
// VETERINARIA ARÉVALO
// SERVICIO DE ENVÍO DE CORREOS ELECTRÓNICOS
//
// FUNCIONALIDADES:
// 1. Enviar códigos de verificación.
// 2. Enviar mensajes HTML.
// 3. Identificar al remitente.
// 4. Utilizar configuración SMTP de Spring Boot.
// 5. Registrar errores sin revelar códigos.
// 6. Mantener compatibilidad con el registro existente.
//
// SEGURIDAD:
// - No imprimir códigos de verificación.
// - No almacenar contraseñas SMTP en Java.
// - No enviar credenciales al frontend.
// ==========================================================

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;


// ==========================================================
// SERVICIO DE CORREO
// ==========================================================

@Service
public class EmailService {

    // ======================================================
    // 1. REGISTRO DE EVENTOS
    // ======================================================

    private static final Logger log =
            LoggerFactory.getLogger(EmailService.class);


    // ======================================================
    // 2. SERVIDOR SMTP
    // ======================================================

    private final JavaMailSender mailSender;


    // ======================================================
    // 3. CORREO REMITENTE
    // ======================================================
    //
    // Se obtiene desde:
    // spring.mail.username
    //
    // Debe corresponder a la cuenta autorizada
    // para enviar correos desde el servidor SMTP.
    // ======================================================

    private final String correoRemitente;


    // ======================================================
    // 4. CONSTRUCTOR
    // ======================================================

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}")
            String correoRemitente
    ) {

        this.mailSender = mailSender;

        this.correoRemitente =
                correoRemitente == null
                        ? ""
                        : correoRemitente.trim();

    }


    // ======================================================
    // 5. ENVIAR CÓDIGO DE VERIFICACIÓN
    // ======================================================
    //
    // Se conserva el método original.
    //
    // Esto permite que AuthService o UsuarioService
    // continúe llamando:
    //
    // emailService.enviarCodigoVerificacion(
    //     correo,
    //     codigo
    // );
    //
    // No cambia la firma del método.
    // ======================================================

    public void enviarCodigoVerificacion(
            String correo,
            String codigo
    ) {

        // ==================================================
        // PASO 1. VALIDAR DESTINATARIO
        // ==================================================

        if (
                correo == null ||
                correo.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "El correo del destinatario es obligatorio"
            );

        }


        // ==================================================
        // PASO 2. VALIDAR CÓDIGO
        // ==================================================

        if (
                codigo == null ||
                codigo.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "El código de verificación es obligatorio"
            );

        }


        // ==================================================
        // PASO 3. VALIDAR REMITENTE
        // ==================================================

        if (correoRemitente.isBlank()) {

            log.error(
                    "No se configuró spring.mail.username"
            );

            throw new IllegalStateException(
                    "No está configurada la cuenta de envío de correos"
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
        // PASO 6. CREAR VERSIÓN DE TEXTO PLANO
        // ==================================================
        //
        // Se utiliza como alternativa para clientes
        // de correo que no muestran HTML.
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
        // PASO 7. ENVIAR CORREO
        // ==================================================

        try {

            MimeMessage mensaje =
                    mailSender.createMimeMessage();


            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            mensaje,
                            true,
                            "UTF-8"
                    );


            // Remitente autorizado.
            // ======================================================
            // REMITENTE DEL CORREO
// ======================================================
// Utiliza el correo configurado en application.properties.
// Evita la excepción adicional de codificación.

helper.setFrom(correoRemitente);


            // Destinatario.
            helper.setTo(destinatario);


            // Asunto.
            helper.setSubject(
                    "Código de verificación - Veterinaria Arévalo"
            );


            // Texto plano y versión HTML.
            helper.setText(
                    contenidoTexto,
                    contenidoHtml
            );


            // Solicitar envío al servidor SMTP.
            mailSender.send(mensaje);


            // ==================================================
            // PASO 8. CONFIRMACIÓN
            // ==================================================
            //
            // Significa que mailSender terminó
            // sin lanzar una excepción.
            //
            // No garantiza la entrega en bandeja
            // de entrada del destinatario.
            // ==================================================

            log.info(
                    "El servidor SMTP aceptó el correo de verificación"
            );


        } catch (
                MessagingException |
                MailException e
        ) {

            // Registrar el error técnico completo.
            // No mostrar el código ni la contraseña.

            log.error(
                    "Falló el envío del correo de verificación",
                    e
            );


            // Conservar la causa para depuración.
            throw new IllegalStateException(
                    "No se pudo enviar el correo de verificación",
                    e
            );

        }

    }

}
