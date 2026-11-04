package com.veterinaria.veterinaria.exception;

// ==========================================================
// IMPORTACIONES GENERALES
// ==========================================================
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.ConstraintViolationException;

// ==========================================================
// MANEJADOR GLOBAL DE ERRORES DE LA API
// ==========================================================
// Centraliza las respuestas de error para el frontend y Swagger.
// Conserva las excepciones que ya utilizaba la aplicación.
// NUEVO: respeta los códigos de ResponseStatusException
// (400, 403, 404, 409...), en lugar de convertirlos en 500.
// ==========================================================
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Registro interno de errores inesperados. Nunca se envía
    // la información sensible de PostgreSQL al cliente.
    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ========================================================
    // RECURSO NO ENCONTRADO - 404 (ORIGINAL)
    // ========================================================
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> recursoNoEncontrado(
            ResourceNotFoundException ex
    ) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                404,
                "NOT_FOUND",
                ex.getMessage(),
                "El recurso solicitado no fue encontrado"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // ========================================================
    // DATOS INCORRECTOS - 400 (ORIGINAL)
    // ========================================================
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> datosIncorrectos(
            BadRequestException ex
    ) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                400,
                "BAD_REQUEST",
                ex.getMessage(),
                "Los datos enviados no son válidos"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ========================================================
    // VALIDACIONES @VALID - 400 (ORIGINAL)
    // ========================================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> erroresValidacion(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse respuesta = new ErrorResponse(
                LocalDateTime.now(),
                400,
                "VALIDATION_ERROR",
                "Los datos enviados no cumplen las validaciones",
                errores.toString()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
    }

    // ========================================================
    // VALIDACIÓN DE RESTRICCIONES - 400 (ORIGINAL)
    // ========================================================
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> errorValidacionJPA(
            ConstraintViolationException ex
    ) {
        String detalle = ex.getConstraintViolations()
                .stream()
                .map(error -> error.getMessage())
                .reduce("", (a, b) -> a + b + "; ");

        ErrorResponse respuesta = new ErrorResponse(
                LocalDateTime.now(),
                400,
                "VALIDATION_ERROR",
                "Los datos enviados no cumplen las validaciones",
                detalle
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
    }

    // ========================================================
    // DUPLICADOS - 409 (ORIGINAL)
    // ========================================================
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> duplicado(
            DuplicateResourceException ex
    ) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                409,
                "CONFLICT",
                ex.getMessage(),
                "El recurso ya existe en la base de datos"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // ========================================================
    // PROHIBIDO - 403 (ORIGINAL)
    // ========================================================
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> prohibido(
            ForbiddenException ex
    ) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                403,
                "FORBIDDEN",
                ex.getMessage(),
                "La cuenta no está autorizada para iniciar sesión"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // ========================================================
    // SPRING SECURITY - ACCESO DENEGADO - 403 (ORIGINAL)
    // ========================================================
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accesoDenegado(
            AccessDeniedException ex
    ) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                403,
                "FORBIDDEN",
                "No tiene permisos suficientes para realizar esta operación",
                "El usuario está autenticado pero su rol no permite acceder a este recurso"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // ========================================================
    // NO AUTENTICADO - 401 (ORIGINAL)
    // ========================================================
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> noAutorizado(
            UnauthorizedException ex
    ) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                401,
                "UNAUTHORIZED",
                ex.getMessage(),
                "No fue posible autenticar al usuario"
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // ========================================================
    // NUEVO: RESPETAR ERRORES HTTP DEL CONTROLADOR
    // ========================================================
    // Los controladores usan ResponseStatusException para
    // indicar solicitudes inválidas, permisos y conflictos.
    // Antes, Exception.class capturaba estas excepciones y
    // respondía incorrectamente con 500.
    // ========================================================
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> errorEstadoHttp(
            ResponseStatusException ex
    ) {
        int codigo = ex.getStatusCode().value();
        HttpStatus estado = HttpStatus.resolve(codigo);
        String tipo = estado != null ? estado.name() : "HTTP_ERROR";

        // Evitar exponer información interna en respuestas 5xx.
        String mensaje = codigo >= 500
                ? "Ocurrió un error interno en el servidor"
                : (ex.getReason() != null && !ex.getReason().isBlank()
                    ? ex.getReason()
                    : "No se pudo completar la operación solicitada");

        if (codigo >= 500) {
            log.error("Error HTTP generado por el controlador", ex);
        }

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                codigo,
                tipo,
                mensaje,
                "La solicitud no cumple las condiciones para realizar esta operación"
        );
        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    // ========================================================
    // NUEVO: JSON MAL FORMADO O DATOS NO INTERPRETABLES - 400
    // ========================================================
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> jsonIncorrecto(
            HttpMessageNotReadableException ex
    ) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                400,
                "BAD_REQUEST",
                "El cuerpo de la solicitud contiene datos inválidos",
                "Revisa el formato JSON y los tipos de datos enviados"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ========================================================
    // NUEVO: ID O PARÁMETRO CON FORMATO INVÁLIDO - 400
    // ========================================================
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> parametroIncorrecto(
            MethodArgumentTypeMismatchException ex
    ) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                400,
                "BAD_REQUEST",
                "El parámetro '" + ex.getName() + "' tiene un formato inválido",
                "Comprueba el valor enviado en la URL"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ========================================================
    // NUEVO: CONFLICTOS DE INTEGRIDAD EN BASE DE DATOS - 409
    // ========================================================
    // Ejemplo: una cédula duplicada o una clave relacionada.
    // No enviamos al navegador el SQL ni los datos internos.
    // ========================================================
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> conflictoBaseDatos(
            DataIntegrityViolationException ex
    ) {
        log.warn("Conflicto de integridad en la base de datos", ex);

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                409,
                "CONFLICT",
                "No se pudo completar la operación por un conflicto de datos",
                "Comprueba si el registro está duplicado o tiene datos relacionados"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // ========================================================
    // ERROR GENERAL - 500 (ORIGINAL, MÁS SEGURO)
    // ========================================================
    // Solo captura errores inesperados que no hayan sido
    // gestionados anteriormente. El detalle técnico se guarda
    // en la consola del servidor, no en Swagger ni Angular.
    // ========================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> errorGeneral(
            Exception ex
    ) {
        log.error("Error inesperado en la API veterinaria", ex);

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                500,
                "INTERNAL_SERVER_ERROR",
                "Ocurrió un error interno en el servidor",
                "Contacta al administrador si el problema continúa"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
