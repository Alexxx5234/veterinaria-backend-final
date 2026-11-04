package com.veterinaria.veterinaria.service;


import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import com.veterinaria.veterinaria.dto.RegistroRequest;

import com.veterinaria.veterinaria.model.EstadoUsuario;
import com.veterinaria.veterinaria.model.Rol;
import com.veterinaria.veterinaria.model.Usuario;

import com.veterinaria.veterinaria.repository.UsuarioRepository;

import com.veterinaria.veterinaria.exception.BadRequestException;
import com.veterinaria.veterinaria.exception.DuplicateResourceException;
import com.veterinaria.veterinaria.exception.ResourceNotFoundException;



@Service
public class UsuarioService {



    // ========================================================
    // DEPENDENCIAS
    // ========================================================

    private final UsuarioRepository usuarioRepository;

    private final PasswordEncoder passwordEncoder;

    private final EmailService emailService;



    // ========================================================
    // CONSTRUCTOR
    // ========================================================

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {

        this.usuarioRepository = usuarioRepository;

        this.passwordEncoder = passwordEncoder;

        this.emailService = emailService;

    }



    // ========================================================
    // REGISTRAR USUARIO
    // ========================================================

    public Usuario registrarUsuario(
            RegistroRequest request
    ) {


        /*
         * ====================================================
         * COMPROBAR SI EL CORREO YA EXISTE
         * ====================================================
         *
         * Si el correo ya está registrado,
         * devolvemos un error 409 CONFLICT.
         *
         * ====================================================
         */

        if (
                usuarioRepository
                        .findByCorreo(
                                request.getCorreo()
                        )
                        .isPresent()
        ) {

            throw new DuplicateResourceException(
                    "El correo ya se encuentra registrado"
            );

        }



        // ====================================================
        // CREAR USUARIO
        // ====================================================

        Usuario usuario =
                new Usuario();



        // ====================================================
        // NOMBRE
        // ====================================================

        usuario.setNombre(
                request.getNombre()
        );



        // ====================================================
        // CORREO
        // ====================================================

        usuario.setCorreo(
                request.getCorreo()
        );



        // ====================================================
        // CONTRASEÑA
        // ====================================================
        //
        // La contraseña se guarda cifrada.
        // ====================================================

        usuario.setPassword(

                passwordEncoder.encode(
                        request.getPassword()
                )

        );



        // ====================================================
        // ROL INICIAL
        // ====================================================

        usuario.setRol(
                Rol.USER
        );



        // ====================================================
        // ESTADO INICIAL
        // ====================================================

        usuario.setEstado(
                EstadoUsuario.PENDIENTE
        );



        // ====================================================
        // CUENTA NO VERIFICADA
        // ====================================================

        usuario.setVerificado(
                false
        );



        // ====================================================
        // CUENTA ACTIVA
        // ====================================================

        usuario.setActivo(
                true
        );



        // ====================================================
        // GENERAR CÓDIGO DE VERIFICACIÓN
        // ====================================================
        //
        // Genera un número entre 1000 y 9999.
        //
        // ====================================================

        String codigo =
                String.valueOf(

                        new Random()
                                .nextInt(9000)
                                + 1000

                );



        usuario.setCodigoVerificacion(
                codigo
        );



        // ====================================================
        // FECHA DE CREACIÓN
        // ====================================================

        usuario.setFechaCreacion(
                LocalDateTime.now()
        );



        // ====================================================
        // GUARDAR EN BASE DE DATOS
        // ====================================================

        Usuario usuarioGuardado =
                usuarioRepository.save(
                        usuario
                );



        // ====================================================
        // ENVIAR CÓDIGO POR CORREO
        // ====================================================

        emailService
                .enviarCodigoVerificacion(

                        usuarioGuardado
                                .getCorreo(),

                        usuarioGuardado
                                .getCodigoVerificacion()

                );



        return usuarioGuardado;

    }



    // ========================================================
    // VERIFICAR CÓDIGO
    // ========================================================

    public void verificarCodigo(
            String correo,
            String codigo
    ) {


        // ====================================================
        // VALIDAR CORREO
        // ====================================================

        if (
                correo == null ||
                correo.trim().isEmpty()
        ) {

            throw new BadRequestException(
                    "El correo es obligatorio"
            );

        }



        // ====================================================
        // VALIDAR CÓDIGO
        // ====================================================

        if (
                codigo == null ||
                codigo.trim().isEmpty()
        ) {

            throw new BadRequestException(
                    "Debe ingresar el código de verificación"
            );

        }



        // ====================================================
        // BUSCAR USUARIO POR CORREO
        // ====================================================
        //
        // Si el usuario no existe:
        // 404 NOT_FOUND
        //
        // ====================================================

        Usuario usuario =
                usuarioRepository
                        .findByCorreo(
                                correo
                        )
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "No existe un usuario registrado con ese correo"
                                )
                        );



        // ====================================================
        // COMPROBAR SI YA ESTÁ VERIFICADO
        // ====================================================

        if (
                usuario.isVerificado()
        ) {

            throw new BadRequestException(
                    "La cuenta ya se encuentra verificada"
            );

        }



        // ====================================================
        // COMPROBAR SI EXISTE CÓDIGO PENDIENTE
        // ====================================================

        if (
                usuario.getCodigoVerificacion() == null
        ) {

            throw new BadRequestException(
                    "No existe un código de verificación pendiente"
            );

        }



        // ====================================================
        // COMPROBAR CÓDIGO
        // ====================================================

        if (
                !usuario
                        .getCodigoVerificacion()
                        .equals(
                                codigo.trim()
                        )
        ) {

            throw new BadRequestException(
                    "El código de verificación es incorrecto"
            );

        }



        // ====================================================
        // MARCAR CUENTA COMO VERIFICADA
        // ====================================================

        usuario.setVerificado(
                true
        );



        // ====================================================
        // ELIMINAR CÓDIGO UTILIZADO
        // ====================================================

        usuario.setCodigoVerificacion(
                null
        );



        // ====================================================
        // GUARDAR CAMBIOS
        // ====================================================

        usuarioRepository.save(
                usuario
        );

    }

}