package com.veterinaria.veterinaria.security;


// ==========================================================
// IMPORTACIONES GENERALES
// ==========================================================

import java.io.IOException;
import java.util.List;


// ==========================================================
// IMPORTACIONES SPRING SECURITY
// ==========================================================

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.GrantedAuthority;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Component;

import org.springframework.web.filter.OncePerRequestFilter;


// ==========================================================
// MODELOS
// ==========================================================

import com.veterinaria.veterinaria.model.Usuario;

import com.veterinaria.veterinaria.model.EstadoUsuario;


// ==========================================================
// REPOSITORIO
// ==========================================================

import com.veterinaria.veterinaria.repository.UsuarioRepository;


// ==========================================================
// IMPORTACIONES SERVLET
// ==========================================================

import jakarta.servlet.FilterChain;

import jakarta.servlet.ServletException;

import jakarta.servlet.http.HttpServletRequest;

import jakarta.servlet.http.HttpServletResponse;


// ==========================================================
// FILTRO DE AUTENTICACIÓN JWT
// ==========================================================
//
// Este filtro revisa las peticiones que llegan a la API.
//
// Sus funciones son:
//
// 1. Obtener el token JWT.
// 2. Verificar que el token sea válido.
// 3. Identificar al usuario mediante su correo.
// 4. Comprobar el estado actual de la cuenta.
// 5. Obtener el rol desde PostgreSQL.
// 6. Registrar la autenticación en Spring Security.
//
// Es compatible con:
//
// - USER
// - ADMIN
// - VETERINARIO
//
// ==========================================================

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {


    // ======================================================
    // DEPENDENCIAS
    // ======================================================

    private final JwtService jwtService;

    private final UsuarioRepository usuarioRepository;


    // ======================================================
    // CONSTRUCTOR
    // ======================================================

    public JwtAuthenticationFilter(

            JwtService jwtService,

            UsuarioRepository usuarioRepository

    ) {

        this.jwtService = jwtService;

        this.usuarioRepository = usuarioRepository;

    }


    // ======================================================
    // FILTRAR PETICIONES
    // ======================================================
    //
    // Este método se ejecuta durante el procesamiento
    // de las peticiones HTTP.
    //
    // ======================================================

    @Override
    protected void doFilterInternal(

            HttpServletRequest request,

            HttpServletResponse response,

            FilterChain filterChain

    ) throws ServletException, IOException {


        // ==================================================
        // 1. OBTENER CABECERA AUTHORIZATION
        // ==================================================
        //
        // Ejemplo:
        //
        // Authorization: Bearer eyJhbGciOi...
        //
        // ==================================================

        String header = request.getHeader("Authorization");


        // ==================================================
        // 2. COMPROBAR SI EXISTE TOKEN
        // ==================================================
        //
        // Si no existe un token Bearer,
        // continuamos con la petición.
        //
        // Las rutas públicas podrán funcionar normalmente.
        //
        // Las rutas protegidas seguirán requiriendo
        // autenticación mediante Spring Security.
        //
        // ==================================================

        if (
                header == null
                ||
                !header.startsWith("Bearer ")
        ) {

            filterChain.doFilter(request, response);

            return;

        }


        // ==================================================
        // 3. EXTRAER TOKEN
        // ==================================================

        String token = header.substring(7);


        try {

            // ==============================================
            // 4. VALIDAR TOKEN Y EXTRAER CORREO
            // ==============================================
            //
            // JwtService comprueba la firma del token
            // y que todavía no haya expirado.
            //
            // ==============================================

            String correo = jwtService.extraerCorreo(token);


            // ==============================================
            // 5. BUSCAR USUARIO EN POSTGRESQL
            // ==============================================
            //
            // Consultamos el usuario real para comprobar
            // sus permisos y estado actuales.
            //
            // ==============================================

            Usuario usuario = usuarioRepository
                    .findByCorreo(correo)
                    .orElse(null);


            // ==============================================
            // 6. VALIDAR LA CUENTA
            // ==============================================
            //
            // El usuario solamente podrá autenticarse si:
            //
            // - Existe en la base de datos.
            // - Verificó su correo.
            // - Su estado es APROBADA.
            // - Su cuenta está activa.
            // - Tiene un rol asignado.
            //
            // Esto evita que un usuario bloqueado
            // continúe utilizando un token antiguo.
            //
            // ==============================================

            if (

                    usuario != null

                    && usuario.isVerificado()

                    && usuario.isActivo()

                    && usuario.getEstado() == EstadoUsuario.APROBADA

                    && usuario.getRol() != null

            ) {


                // ==========================================
                // 7. OBTENER ROL ACTUAL
                // ==========================================
                //
                // El rol se obtiene desde PostgreSQL.
                //
                // Ejemplos:
                //
                // USER        -> ROLE_USER
                // ADMIN       -> ROLE_ADMIN
                // VETERINARIO -> ROLE_VETERINARIO
                //
                // ==========================================

                List<GrantedAuthority> authorities = List.of(

                        new SimpleGrantedAuthority(

                                "ROLE_" + usuario.getRol().name()

                        )

                );


                // ==========================================
                // 8. CREAR AUTENTICACIÓN
                // ==========================================
                //
                // Registramos el correo del usuario
                // y su rol actual.
                //
                // ==========================================

                UsernamePasswordAuthenticationToken authentication =

                        new UsernamePasswordAuthenticationToken(

                                correo,

                                null,

                                authorities

                        );


                // ==========================================
                // 9. GUARDAR AUTENTICACIÓN
                // ==========================================
                //
                // Spring Security utilizará esta
                // información para comprobar permisos.
                //
                // ==========================================

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);


            } else {


                // ==========================================
                // 10. CUENTA NO AUTORIZADA
                // ==========================================
                //
                // Si el usuario está bloqueado,
                // desactivado, pendiente o no verificado,
                // no se registra su autenticación.
                //
                // ==========================================

                SecurityContextHolder.clearContext();

            }


        } catch (Exception e) {


            // ==============================================
            // 11. TOKEN INVÁLIDO O EXPIRADO
            // ==============================================
            //
            // Si falla la validación del token,
            // eliminamos cualquier autenticación.
            //
            // Spring Security gestionará posteriormente
            // el acceso a las rutas protegidas.
            //
            // ==============================================

            SecurityContextHolder.clearContext();

        }


        // ==================================================
        // 12. CONTINUAR CON LA PETICIÓN
        // ==================================================

        filterChain.doFilter(request, response);

    }

}