
package com.veterinaria.veterinaria.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class GmailApiSender {

    private final boolean habilitado;
    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;
    private final RestClient clienteHttp;

    public GmailApiSender(
            @Value("${GMAIL_API_ENABLED:false}") boolean habilitado,
            @Value("${GMAIL_CLIENT_ID:}") String clientId,
            @Value("${GMAIL_CLIENT_SECRET:}") String clientSecret,
            @Value("${GMAIL_REFRESH_TOKEN:}") String refreshToken
    ) {
        this.habilitado = habilitado;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(30));

        this.clienteHttp = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    public boolean estaHabilitado() {
        return habilitado;
    }

    // Enviar un mensaje MIME por Gmail API HTTPS.
    // Compatible con HTML y archivos adjuntos.
    public void enviar(MimeMessage mensaje) {

        if (!habilitado) {
            throw new IllegalStateException(
                    "Gmail API no esta habilitada"
            );
        }

        if (clientId.isBlank()
                || clientSecret.isBlank()
                || refreshToken.isBlank()) {
            throw new IllegalStateException(
                    "Faltan variables privadas de Gmail API"
            );
        }

        try {
            // Renovar el access token.
            MultiValueMap<String, String> datos =
                    new LinkedMultiValueMap<>();

            datos.add("client_id", clientId);
            datos.add("client_secret", clientSecret);
            datos.add("refresh_token", refreshToken);
            datos.add("grant_type", "refresh_token");

            Map<?, ?> respuesta = clienteHttp.post()
                    .uri("https://oauth2.googleapis.com/token")
                    .contentType(
                            MediaType.APPLICATION_FORM_URLENCODED
                    )
                    .body(datos)
                    .retrieve()
                    .body(Map.class);

            Object token = respuesta == null
                    ? null
                    : respuesta.get("access_token");

            if (!(token instanceof String acceso)
                    || acceso.isBlank()) {
                throw new IllegalStateException(
                        "Google no entrego un access token"
                );
            }

            // Convertir el correo MIME a Base64URL.
            mensaje.saveChanges();

            ByteArrayOutputStream salida =
                    new ByteArrayOutputStream();

            mensaje.writeTo(salida);

            String raw = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(salida.toByteArray());

            // Enviar correo mediante HTTPS.
            clienteHttp.post()
                    .uri(
                        "https://gmail.googleapis.com/gmail/v1/users/me/messages/send"
                    )
                    .headers(h -> h.setBearerAuth(acceso))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("raw", raw))
                    .retrieve()
                    .toBodilessEntity();

        } catch (MessagingException | IOException e) {
            throw new IllegalStateException(
                    "No se pudo preparar el correo MIME", e
            );
        } catch (RestClientException e) {
            throw new IllegalStateException(
                    "Fallo la comunicacion HTTPS con Gmail API", e
            );
        }
    }
}
