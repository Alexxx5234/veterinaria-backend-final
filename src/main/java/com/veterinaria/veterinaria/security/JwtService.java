
package com.veterinaria.veterinaria.security;

// ==========================================================
// VETERINARIA ARÉVALO
// SERVICIO DE AUTENTICACIÓN JWT
//
// Compatible con:
// - ADMIN
// - VETERINARIO
// - USER
//
// FUNCIONALIDADES:
// 1. Obtener la clave desde variables de entorno.
// 2. Utilizar la configuración privada local.
// 3. Generar tokens JWT.
// 4. Extraer el correo del usuario autenticado.
// 5. Validar la firma y vencimiento del token.
// 6. Mantener una duración de sesión de 5 horas.
//
// SEGURIDAD:
// - No contiene contraseñas ni claves fijas.
// - Requiere una clave Base64 de al menos 32 bytes.
// - Compatible con despliegue en Railway.
// ==========================================================

import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    // ======================================================
    // 1. CLAVE CRIPTOGRÁFICA
    // ======================================================

    private final SecretKey secretKey;

    // Duración original: 5 horas.
    private static final long DURACION_TOKEN =
            TimeUnit.HOURS.toMillis(5);

    // ======================================================
    // 2. CONSTRUCTOR
    //
    // Busca la clave JWT en este orden:
    //
    // 1. Variable de entorno JWT_SECRET.
    // 2. Propiedad app.jwt.secret.
    //
    // La segunda puede estar definida en
    // application-local.properties.
    //
    // No se utiliza ninguna clave predeterminada.
    // ======================================================

    public JwtService(Environment environment) {

        // Primero revisar variable de entorno.
        String claveBase64 =
                environment.getProperty("JWT_SECRET");

        // Si no existe, buscar propiedad local.
        if (claveBase64 == null || claveBase64.isBlank()) {

            claveBase64 =
                    environment.getProperty("app.jwt.secret");
        }

        // Si no se configuró ninguna clave, detener inicio.
        if (claveBase64 == null || claveBase64.isBlank()) {

            throw new IllegalStateException(
                    "Falta configurar la clave JWT. " +
                    "Utiliza JWT_SECRET o app.jwt.secret."
            );
        }

        // ==================================================
        // 3. DECODIFICAR LA CLAVE BASE64
        // ==================================================

        byte[] claveBytes;

        try {

            claveBytes = Decoders.BASE64.decode(
                    claveBase64.trim()
            );

        } catch (Exception error) {

            throw new IllegalStateException(
                    "La clave JWT debe estar codificada " +
                    "correctamente en Base64.",
                    error
            );
        }

        // ==================================================
        // 4. VALIDAR LONGITUD MÍNIMA
        //
        // HS256 requiere como mínimo 256 bits.
        // 256 bits equivalen a 32 bytes.
        // ==================================================

        if (claveBytes.length < 32) {

            throw new IllegalStateException(
                    "La clave JWT debe contener " +
                    "al menos 32 bytes aleatorios."
            );
        }

        // Crear clave criptográfica.
        this.secretKey = Keys.hmacShaKeyFor(
                claveBytes
        );
    }

    // ======================================================
    // 5. OBTENER CLAVE
    // ======================================================

    private SecretKey getKey() {

        return secretKey;
    }

    // ======================================================
    // 6. GENERAR TOKEN JWT
    //
    // - Identifica al usuario mediante su correo.
    // - Incluye su rol en el token.
    // - Conserva duración de 5 horas.
    // ======================================================

    public String generarToken(
            String correo,
            String rol) {

        long ahora = System.currentTimeMillis();

        return Jwts.builder()

                .subject(correo)

                .claim("rol", rol)

                .issuedAt(
                        new Date(ahora)
                )

                .expiration(
                        new Date(
                                ahora + DURACION_TOKEN
                        )
                )

                .signWith(
                        getKey()
                )

                .compact();
    }

    // ======================================================
    // 7. EXTRAER CORREO DEL TOKEN
    //
    // Comprueba primero la firma del JWT.
    // ======================================================

    public String extraerCorreo(String token) {

        return Jwts.parser()

                .verifyWith(
                        getKey()
                )

                .build()

                .parseSignedClaims(token)

                .getPayload()

                .getSubject();
    }

    // ======================================================
    // 8. VALIDAR TOKEN JWT
    //
    // Retorna TRUE cuando el token es válido.
    //
    // Retorna FALSE cuando:
    // - El token está vacío.
    // - La firma es incorrecta.
    // - El token ha expirado.
    // - El formato es inválido.
    // ======================================================

    public boolean validarToken(String token) {

        if (token == null || token.isBlank()) {

            return false;
        }

        try {

            Jwts.parser()

                    .verifyWith(
                            getKey()
                    )

                    .build()

                    .parseSignedClaims(token);

            return true;

        } catch (
                JwtException |
                IllegalArgumentException error
        ) {

            return false;
        }
    }
}
