package com.veterinaria.veterinaria.security;


// ==========================================================
// IMPORTACIONES GENERALES
// ==========================================================

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


// ==========================================================
// IMPORTACIONES PARA CORS
// ==========================================================

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;



@Configuration

// Permite utilizar anotaciones como:
// @PreAuthorize("hasRole('ADMIN')")
@EnableMethodSecurity
public class SecurityConfig {


    // ======================================================
    // FILTRO JWT
    // ======================================================

    private final JwtAuthenticationFilter jwtAuthenticationFilter;


    // ======================================================
    // MANEJO DE ERRORES
    // ======================================================

    private final CustomAccessDeniedHandler accessDeniedHandler;

    private final CustomAuthenticationEntryPoint authenticationEntryPoint;



    // ======================================================
    // CONSTRUCTOR
    // ======================================================

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomAccessDeniedHandler accessDeniedHandler,
            CustomAuthenticationEntryPoint authenticationEntryPoint
    ) {

        this.jwtAuthenticationFilter =
                jwtAuthenticationFilter;

        this.accessDeniedHandler =
                accessDeniedHandler;

        this.authenticationEntryPoint =
                authenticationEntryPoint;

    }



    // ======================================================
    // CONFIGURACIÓN PRINCIPAL DE SPRING SECURITY
    // ======================================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {


        http


            // ==================================================
            // CORS
            // ==================================================
            //
            // Permite que Angular y React puedan
            // comunicarse con nuestra API.
            //
            // ==================================================

            .cors(cors ->

                    cors.configurationSource(
                            corsConfigurationSource()
                    )

            )


            // ==================================================
            // CSRF
            // ==================================================
            //
            // Se desactiva porque nuestra API REST
            // utiliza autenticación mediante JWT.
            //
            // ==================================================

            .csrf(csrf ->
                    csrf.disable()
            )



            // ==================================================
            // CONTROL DE ERRORES 401 Y 403
            // ==================================================

            .exceptionHandling(ex -> ex


                    // ==========================================
                    // ERROR 403
                    // ==========================================
                    //
                    // El usuario está autenticado,
                    // pero no tiene permisos.
                    //
                    // ==========================================

                    .accessDeniedHandler(
                            accessDeniedHandler
                    )


                    // ==========================================
                    // ERROR 401
                    // ==========================================
                    //
                    // El usuario no está autenticado
                    // o el token JWT no es válido.
                    //
                    // ==========================================

                    .authenticationEntryPoint(
                            authenticationEntryPoint
                    )

            )



            // ==================================================
            // AUTORIZACIÓN DE ENDPOINTS
            // ==================================================

            .authorizeHttpRequests(auth -> auth


                    // ==========================================
                    // SWAGGER PÚBLICO
                    // ==========================================

                    .requestMatchers(
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/swagger-ui.html"
                    )
                    .permitAll()



                    // ==========================================
                    // AUTENTICACIÓN PÚBLICA
                    // ==========================================
                    //
                    // Estas rutas no necesitan JWT.
                    //
                    // Se utilizan para:
                    //
                    // - Registrarse
                    // - Verificar cuenta
                    // - Iniciar sesión
                    //
                    // ==========================================

                    .requestMatchers(
                            "/api/auth/registro",
                            "/api/auth/login",
                            "/api/auth/verificar-codigo"
                    )
                    .permitAll()



                    // ==========================================
                    // RESTO DE ENDPOINTS
                    // ==========================================
                    //
                    // Cualquier otra petición necesita
                    // un JWT válido.
                    //
                    // ==========================================

                    .anyRequest()
                    .authenticated()

            )



            // ==================================================
            // FILTRO JWT
            // ==================================================
            //
            // El filtro JWT se ejecuta antes
            // del filtro estándar de Spring Security.
            //
            // ==================================================

            .addFilterBefore(
                    jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class
            );


        return http.build();

    }



    // ==========================================================
    // CONFIGURACIÓN CORS
    // ==========================================================
    //
    // CORS permite que aplicaciones ejecutándose
    // desde diferentes puertos puedan consumir la API.
    //
    // Angular:
    // http://localhost:4200
    //
    // React:
    // http://localhost:5173
    //
    // Spring Boot:
    // http://localhost:8080
    //
    // ==========================================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {


        // Creamos la configuración de CORS.
        CorsConfiguration configuration =
                new CorsConfiguration();



        // ======================================================
        // ORÍGENES PERMITIDOS
        // ======================================================
        //
        // Permitimos nuestros dos frontends:
        //
        // Angular → puerto 4200
        // React   → puerto 5173
        //
        // ======================================================

        configuration.setAllowedOrigins(
                List.of(

                        "http://localhost:4200",

                        "http://localhost:5173"

                )
        );



        // ======================================================
        // MÉTODOS HTTP PERMITIDOS
        // ======================================================
        //
        // Son los métodos que podrán utilizar
        // Angular y React para consumir la API.
        //
        // ======================================================

        configuration.setAllowedMethods(
                List.of(

                        "GET",

                        "POST",

                        "PUT",

                        "DELETE",

                        "OPTIONS"

                )
        );



        // ======================================================
        // CABECERAS PERMITIDAS
        // ======================================================
        //
        // Authorization es necesaria para enviar JWT.
        //
        // Content-Type indica el tipo de contenido.
        //
        // Accept indica el formato que puede recibir
        // el frontend.
        //
        // ======================================================

        configuration.setAllowedHeaders(
                List.of(

                        "Authorization",

                        "Content-Type",

                        "Accept"

                )
        );



        // ======================================================
        // CREDENCIALES
        // ======================================================
        //
        // Permite trabajar con credenciales
        // y cabeceras de autenticación.
        //
        // ======================================================

        configuration.setAllowCredentials(
                true
        );



        // ======================================================
        // FUENTE DE CONFIGURACIÓN
        // ======================================================

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();



        // ======================================================
        // APLICAR CORS
        // ======================================================
        //
        // Aplicamos esta configuración
        // a todos los endpoints del backend.
        //
        // ======================================================

        source.registerCorsConfiguration(
                "/**",
                configuration
        );


        return source;

    }

}