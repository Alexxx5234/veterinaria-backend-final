package com.veterinaria.veterinaria.controller;


import java.util.List;


import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;


import com.veterinaria.veterinaria.model.Usuario;
import com.veterinaria.veterinaria.repository.UsuarioRepository;
import com.veterinaria.veterinaria.exception.ResourceNotFoundException;



@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {



    private final UsuarioRepository usuarioRepository;



    public UsuarioController(
            UsuarioRepository usuarioRepository){


        this.usuarioRepository = usuarioRepository;

    }









    @Operation(
        summary = "Listar usuarios",
        description = "Permite consultar todos los usuarios registrados. Acceso exclusivo para administradores."
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Usuario>> listarUsuarios(){


        return ResponseEntity.ok(
                usuarioRepository.findAll()
        );


    }









    @Operation(
        summary = "Consultar usuario por ID",
        description = "Permite consultar la información de un usuario específico. Acceso exclusivo para administradores."
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Usuario> buscarUsuario(
            @PathVariable Long id){



        Usuario usuario = usuarioRepository.findById(id)

                .orElseThrow(() ->

                    new ResourceNotFoundException(
                        "Usuario no encontrado con id: " + id
                    )

                );



        return ResponseEntity.ok(usuario);


    }



}