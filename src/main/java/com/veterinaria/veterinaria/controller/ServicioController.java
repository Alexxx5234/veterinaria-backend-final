package com.veterinaria.veterinaria.controller;


import java.util.List;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;


import com.veterinaria.veterinaria.model.Servicio;

import com.veterinaria.veterinaria.service.ServicioService;


import com.veterinaria.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.veterinaria.exception.MensajeRespuesta;



@RestController
@RequestMapping("/api/servicios")
public class ServicioController {


    private final ServicioService servicioService;



    /*
     * ========================================================
     * CONSTRUCTOR
     * ========================================================
     */

    public ServicioController(
            ServicioService servicioService
    ) {

        this.servicioService = servicioService;

    }



    /*
     * ========================================================
     * LISTAR SERVICIOS
     *
     * USER:
     * Puede consultar los servicios disponibles
     * para poder seleccionarlos al crear una cita.
     *
     * ADMIN:
     * También puede consultar todos.
     * ========================================================
     */

    @Operation(
        summary = "Listar servicios",
        description = "Obtiene todos los servicios veterinarios disponibles. Disponible para usuarios y administradores."
    )

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")

    public List<Servicio> listarTodos() {

        return servicioService.listarTodos();

    }



    /*
     * ========================================================
     * CONSULTAR SERVICIO POR ID
     *
     * Disponible para USER y ADMIN.
     * ========================================================
     */

    @Operation(
        summary = "Consultar servicio por ID",
        description = "Permite consultar la información de un servicio veterinario."
    )

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")

    public ResponseEntity<Servicio> buscarPorId(
            @PathVariable Long id
    ) {


        Servicio servicio =
                servicioService.buscarPorId(id)

                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Servicio no encontrado con id: " + id
                    )
                );


        return ResponseEntity.ok(servicio);

    }



    /*
     * ========================================================
     * REGISTRAR SERVICIO
     *
     * Solo ADMIN.
     * ========================================================
     */

    @Operation(
        summary = "Registrar servicio",
        description = "Permite registrar un nuevo servicio veterinario. Disponible únicamente para administradores."
    )

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")

    public Servicio crear(
            @RequestBody Servicio servicio
    ) {


        /*
         * Evita crear manualmente
         * un registro con ID existente.
         */

        servicio.setId(null);


        return servicioService.guardar(
                servicio
        );

    }



    /*
     * ========================================================
     * ACTUALIZAR SERVICIO
     *
     * Solo ADMIN.
     * ========================================================
     */

    @Operation(
        summary = "Actualizar servicio",
        description = "Permite modificar un servicio veterinario. Disponible únicamente para administradores."
    )

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<Servicio> actualizar(

            @PathVariable Long id,

            @RequestBody Servicio servicio
    ) {


        servicioService.buscarPorId(id)

                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Servicio no encontrado con id: " + id
                    )
                );


        servicio.setId(id);


        return ResponseEntity.ok(
                servicioService.guardar(servicio)
        );

    }



    /*
     * ========================================================
     * ELIMINAR SERVICIO
     *
     * Solo ADMIN.
     * ========================================================
     */

    @Operation(
        summary = "Eliminar servicio",
        description = "Permite eliminar un servicio veterinario. Disponible únicamente para administradores."
    )

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<?> eliminar(
            @PathVariable Long id
    ) {


        servicioService.buscarPorId(id)

                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Servicio no encontrado con id: " + id
                    )
                );


        servicioService.eliminar(id);


        return ResponseEntity.ok(

            new MensajeRespuesta(

                200,

                "Servicio eliminado correctamente",

                "El registro fue eliminado del sistema"

            )

        );

    }


}