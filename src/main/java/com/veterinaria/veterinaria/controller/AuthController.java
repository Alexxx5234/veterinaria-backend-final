package com.veterinaria.veterinaria.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;


import com.veterinaria.veterinaria.dto.RegistroRequest;
import com.veterinaria.veterinaria.dto.LoginRequest;
import com.veterinaria.veterinaria.dto.LoginResponse;
import com.veterinaria.veterinaria.dto.VerificarCodigoRequest;


import com.veterinaria.veterinaria.model.Usuario;


import com.veterinaria.veterinaria.service.UsuarioService;
import com.veterinaria.veterinaria.service.AuthService;


import com.veterinaria.veterinaria.exception.MensajeRespuesta;




@RestController
@RequestMapping("/api/auth")
public class AuthController {



    private final UsuarioService usuarioService;

    private final AuthService authService;





    public AuthController(
            UsuarioService usuarioService,
            AuthService authService){


        this.usuarioService = usuarioService;

        this.authService = authService;

    }









    // ==================================
    // REGISTRO DE USUARIO
    // ==================================


    @Operation(
        summary = "Registrar nuevo usuario",
        description = "Permite crear una cuenta nueva. El usuario queda en estado PENDIENTE hasta que un administrador apruebe su acceso."
    )
    @ApiResponses(value = {

        @ApiResponse(
            responseCode = "200",
            description = "Usuario registrado correctamente"
        ),

        @ApiResponse(
            responseCode = "400",
            description = "Datos enviados incorrectamente"
        ),

        @ApiResponse(
            responseCode = "409",
            description = "El correo ya se encuentra registrado"
        )

    })
    @PostMapping("/registro")
    public ResponseEntity<?> registrarUsuario(
            @Valid @RequestBody RegistroRequest request){


        Usuario usuario =
                usuarioService.registrarUsuario(request);



        return ResponseEntity.ok(

            new MensajeRespuesta(
                200,
                "Usuario registrado correctamente",
                "La cuenta fue creada y se envió un código de verificación al correo"
            )

        );

    }









    // ==================================
    // VERIFICAR CUENTA CON CODIGO
    // ==================================


    @Operation(
        summary = "Verificar cuenta mediante código",
        description = "Permite activar la cuenta ingresando el código de 4 dígitos enviado al correo."
    )
    @ApiResponses(value = {

        @ApiResponse(
            responseCode = "200",
            description = "Cuenta verificada correctamente"
        ),

        @ApiResponse(
            responseCode = "400",
            description = "Código incorrecto"
        )

    })
    @PostMapping("/verificar-codigo")
    public ResponseEntity<?> verificarCodigo(
            @RequestBody VerificarCodigoRequest request){



        usuarioService.verificarCodigo(
                request.getCorreo(),
                request.getCodigo()
        );



        return ResponseEntity.ok(

            new MensajeRespuesta(
                200,
                "Cuenta verificada correctamente",
                "El código fue validado correctamente"
            )

        );

    }









    // ==================================
    // LOGIN
    // ==================================


    @Operation(
        summary = "Iniciar sesión y obtener JWT",
        description = "Autentica al usuario mediante correo y contraseña."
    )
    @ApiResponses(value = {

        @ApiResponse(
            responseCode = "200",
            description = "Inicio de sesión exitoso"
        ),

        @ApiResponse(
            responseCode = "401",
            description = "Credenciales incorrectas"
        ),

        @ApiResponse(
            responseCode = "403",
            description = "Usuario pendiente, rechazado o bloqueado"
        )

    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request){



        LoginResponse respuesta =
                authService.login(request);



        return ResponseEntity.ok(respuesta);

    }


}