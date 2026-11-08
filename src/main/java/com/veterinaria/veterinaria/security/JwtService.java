
package com.veterinaria.veterinaria.security;

// ==========================================================
// VETERINARIA ARÉVALO
// SERVICIO JWT - CONFIGURACIÓN SEGURA
//
// - Clave obtenida desde Spring Boot.
// - No contiene claves secretas fijas.
// - Firma y valida tokens JWT.
// - Conserva el correo como identificador.
// - Conserva la duración del token: 5 horas.
// - Compatible con ADMIN, VETERINARIO y USER.
// ==========================================================

import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    // Clave criptográfica utilizada para firmar tokens.
    // Se configura una sola vez al iniciar Spring Boot.
    private final SecretKey secretKey;

    // Duración original: cinco horas.
    private static final long DURACION_TOKEN =
            TimeUnit.HOURS.toMillis(5);

    // ======================================================
    // 1. CONSTRUCTOR
    //
    // Lee app.jwt.secret desde application.properties
    // o desde la configuración privada importada.
    //
    // La clave debe estar codificada en Base64
    // y representar al menos 32 bytes aleatorios.
    // ======================================================

    public JwtService(
            @Value("${app.jwt.secret:}") String claveBase64) {

        if (claveBase64 == null || claveBase64.isBlank()) {
            throw new IllegalStateException(
                    "Falta configurar app.jwt.secret."
            );
        }

        try {

            byte[] claveBytes = Decoders.BASE64.decode(
                    claveBase64.trim()
            );

            if (claveBytes.length < 32) {
                throw new IllegalStateException(
                        "La clave JWT debe tener al menos 32 bytes."
                );
            }

            this.secretKey = Keys.hmacShaKeyFor(
                    claveBytes
            );

        } catch (Exception error) {

            throw new IllegalStateException(
                    "La configuración de la clave JWT no es válida.",
                    error
            );
        }
    }

    // ======================================================
    // 2. OBTENER CLAVE
    // ======================================================

    private SecretKey getKey() {
        return secretKey;
    }

    // ======================================================
    // 3. GENERAR TOKEN JWT
    //
    // El correo identifica al usuario.
    // El rol se incorpora como claim.
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
    // 4. EXTRAER CORREO DEL TOKEN
    //
    // Primero comprueba la firma del JWT.
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
    // 5. VALIDAR TOKEN
    //
    // Devuelve false si el token:
    // - Tiene una firma incorrecta.
    // - Está vencido.
    // - Tiene un formato inválido.
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

        } catch (JwtException | IllegalArgumentException error) {

            return false;
        }
    }
}
