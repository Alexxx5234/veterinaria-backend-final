package com.veterinaria.veterinaria.security;


import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {


    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {


        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        response.setContentType("application/json");


        response.getWriter().write("""
        {
          "codigo":403,
          "error":"FORBIDDEN",
          "mensaje":"No tiene permisos suficientes para realizar esta operación",
          "detalle":"El usuario autenticado no posee el rol requerido"
        }
        """);

    }

}