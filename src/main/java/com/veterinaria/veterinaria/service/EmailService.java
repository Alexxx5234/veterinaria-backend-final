package com.veterinaria.veterinaria.service;


import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;



@Service
public class EmailService {



    private final JavaMailSender mailSender;



    public EmailService(JavaMailSender mailSender){

        this.mailSender = mailSender;

    }





    // ==================================
    // ENVIO DE CORREO CON CODIGO DE 4 DIGITOS
    // ==================================

    public void enviarCodigoVerificacion(
            String correo,
            String codigo){


        System.out.println(
                "ENVIANDO CODIGO A: " + correo
        );


        System.out.println(
                "CODIGO: " + codigo
        );



        try {


            MimeMessage mensaje =
                    mailSender.createMimeMessage();



            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            mensaje,
                            true,
                            "UTF-8"
                    );





            String contenido = """

            <!DOCTYPE html>
            <html>

            <body style="
            margin:0;
            padding:30px;
            background:#f4f7f9;
            font-family:Arial,sans-serif;
            ">



            <div style="
            max-width:600px;
            margin:auto;
            background:white;
            border-radius:15px;
            overflow:hidden;
            box-shadow:0 4px 15px rgba(0,0,0,0.15);
            ">




            <div style="
            background:#2E7D32;
            color:white;
            text-align:center;
            padding:25px;
            ">


            <h1>
            🐾 Veterinaria Arevalo
            </h1>


            <p>
            Cuidado y bienestar para tus mascotas
            </p>


            </div>





            <div style="
            padding:35px;
            color:#333;
            ">



            <h2 style="color:#2E7D32;">
            ¡Bienvenido!
            </h2>




            <p>
            Tu cuenta fue creada correctamente en
            <b>Veterinaria Arevalo</b>.
            </p>



            <p>
            Para activar tu cuenta ingresa el siguiente
            código de verificación:
            </p>





            <div style="
            text-align:center;
            margin:30px 0;
            ">


            <div style="
            display:inline-block;
            background:#2E7D32;
            color:white;
            font-size:35px;
            font-weight:bold;
            letter-spacing:8px;
            padding:20px 35px;
            border-radius:12px;
            ">


            %s


            </div>


            </div>





            <p>
            Este código permite confirmar que eres el propietario
            del correo registrado.
            </p>



            <p>
            Si no realizaste este registro puedes ignorar este mensaje.
            </p>




            </div>






            <div style="
            background:#eeeeee;
            text-align:center;
            padding:15px;
            color:#666;
            font-size:12px;
            ">


            © 2026 Veterinaria Arevalo

            <br>

            Sistema de gestión veterinaria


            </div>



            </div>


            </body>

            </html>


            """.formatted(codigo);






            helper.setTo(correo);



            helper.setSubject(
                    "Código de verificación - Veterinaria Arevalo"
            );



            helper.setText(
                    contenido,
                    true
            );



            mailSender.send(mensaje);



            System.out.println(
                    "CORREO CON CODIGO ENVIADO CORRECTAMENTE"
            );



        } catch(Exception e){


            throw new RuntimeException(
                    "Error enviando correo: "
                    + e.getMessage()
            );


        }


    }

}