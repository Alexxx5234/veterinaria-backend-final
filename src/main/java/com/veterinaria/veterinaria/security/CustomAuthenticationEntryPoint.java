package com.veterinaria.veterinaria.security;


import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;



@Component
public class CustomAuthenticationEntryPoint

implements org.springframework.security.web.AuthenticationEntryPoint {



    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.core.AuthenticationException authException)

            throws IOException, ServletException {



        response.setStatus(
                HttpStatus.UNAUTHORIZED.value()
        );


        response.setContentType(
                "application/json"
        );



        response.getWriter().write("""

        {
            "codigo":401,
            "error":"UNAUTHORIZED",
            "mensaje":"Debe iniciar sesión para acceder a este recurso",
            "detalle":"No se encontró un token JWT válido"
        }

        """);


    }


}