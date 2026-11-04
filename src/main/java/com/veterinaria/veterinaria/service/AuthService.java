package com.veterinaria.veterinaria.service;


import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import com.veterinaria.veterinaria.dto.LoginRequest;
import com.veterinaria.veterinaria.dto.LoginResponse;

import com.veterinaria.veterinaria.model.Usuario;

import com.veterinaria.veterinaria.repository.UsuarioRepository;

import com.veterinaria.veterinaria.security.JwtService;

import com.veterinaria.veterinaria.exception.ForbiddenException;
import com.veterinaria.veterinaria.exception.UnauthorizedException;



@Service
public class AuthService {



    private final UsuarioRepository usuarioRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;



    /*
     * ========================================================
     * CONSTRUCTOR
     * ========================================================
     */

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {

        this.usuarioRepository = usuarioRepository;

        this.passwordEncoder = passwordEncoder;

        this.jwtService = jwtService;

    }



    /*
     * ========================================================
     * LOGIN
     * ========================================================
     */

    public LoginResponse login(
            LoginRequest request
    ) {



        /*
         * ====================================================
         * BUSCAR USUARIO POR CORREO
         * ====================================================
         */

        Usuario usuario =
                usuarioRepository
                        .findByCorreo(
                                request.getCorreo()
                        )
                        .orElseThrow(
                                () -> new UnauthorizedException(
                                        "Usuario no encontrado"
                                )
                        );



        /*
         * ====================================================
         * COMPROBAR VERIFICACIÓN DEL CORREO
         * ====================================================
         */

        if (!usuario.isVerificado()) {

            throw new ForbiddenException(
                    "La cuenta no ha sido verificada"
            );

        }



        /*
         * ====================================================
         * COMPROBAR ESTADO DE LA CUENTA
         * ====================================================
         */

        if (usuario.getEstado() == null) {

            throw new ForbiddenException(
                    "La cuenta no tiene un estado válido"
            );

        }


        String estado =
                usuario
                        .getEstado()
                        .toString();



        /*
         * CUENTA PENDIENTE
         */

        if (estado.equals("PENDIENTE")) {

            throw new ForbiddenException(
                    "La cuenta aún no ha sido aprobada por el administrador"
            );

        }



        /*
         * CUENTA RECHAZADA
         */

        if (estado.equals("RECHAZADA")) {

            throw new ForbiddenException(
                    "La cuenta fue rechazada por el administrador"
            );

        }



        /*
         * CUENTA BLOQUEADA
         */

        if (estado.equals("BLOQUEADA")) {

            throw new ForbiddenException(
                    "La cuenta se encuentra bloqueada"
            );

        }



        /*
         * CUALQUIER OTRO ESTADO DIFERENTE
         * DE APROBADA
         */

        if (!estado.equals("APROBADA")) {

            throw new ForbiddenException(
                    "La cuenta no está habilitada para iniciar sesión"
            );

        }



        /*
         * ====================================================
         * COMPROBAR SI LA CUENTA ESTÁ ACTIVA
         * ====================================================
         */

        if (!usuario.isActivo()) {

            throw new ForbiddenException(
                    "La cuenta está desactivada"
            );

        }



        /*
         * ====================================================
         * VALIDAR CONTRASEÑA
         * ====================================================
         */

        if (
                !passwordEncoder.matches(
                        request.getPassword(),
                        usuario.getPassword()
                )
        ) {

            throw new UnauthorizedException(
                    "Contraseña incorrecta"
            );

        }



        /*
         * ====================================================
         * GENERAR JWT
         * ====================================================
         */

        String token =
                jwtService.generarToken(

                        usuario.getCorreo(),

                        usuario
                                .getRol()
                                .toString()

                );



        /*
         * ====================================================
         * RESPUESTA DEL LOGIN
         * ====================================================
         */

        return new LoginResponse(

                token,

                usuario
                        .getRol()
                        .toString(),

                "Inicio de sesión correcto"

        );

    }

}