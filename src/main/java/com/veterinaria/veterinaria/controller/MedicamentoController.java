
package com.veterinaria.veterinaria.controller;

// ==========================================================
// IMPORTACIONES GENERALES
// ==========================================================

// Permite trabajar con listas de medicamentos.
import java.util.List;

// Permite devolver codigos HTTP y respuestas.
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

// Protege las rutas segun el rol autenticado.
import org.springframework.security.access.prepost.PreAuthorize;

// Permite crear los endpoints REST.
import org.springframework.web.bind.annotation.*;

// Ejecuta las validaciones del modelo.
import jakarta.validation.Valid;

// Documentacion de Swagger.
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

// ==========================================================
// MODELOS, SERVICIOS Y RESPUESTAS
// ==========================================================

import com.veterinaria.veterinaria.model.Medicamento;
import com.veterinaria.veterinaria.service.MedicamentoService;
import com.veterinaria.veterinaria.exception.MensajeRespuesta;


// ==========================================================
// CONTROLADOR DE MEDICAMENTOS
// ==========================================================
//
// Este controlador administra el catalogo de medicamentos
// de nuestra veterinaria.
//
// ==========================================================
// PERMISOS DEL ADMINISTRADOR
// ==========================================================
//
// ADMIN puede:
//
// - Consultar todos los medicamentos.
// - Consultar medicamentos activos e inactivos.
// - Buscar medicamentos por ID.
// - Registrar medicamentos.
// - Actualizar informacion.
// - Modificar precios.
// - Administrar existencias.
// - Activar medicamentos.
// - Desactivar medicamentos.
// - Eliminar mediante baja logica.
//
// ==========================================================
// PERMISOS DEL VETERINARIO
// ==========================================================
//
// VETERINARIO puede:
//
// - Consultar TODOS los medicamentos.
// - Consultar medicamentos activos.
// - Consultar medicamentos inactivos.
// - Buscar cualquier medicamento por ID.
//
// VETERINARIO NO puede:
//
// - Crear medicamentos.
// - Editar medicamentos.
// - Modificar precios.
// - Modificar stock directamente.
// - Activar o desactivar medicamentos.
// - Eliminar medicamentos.
//
// NOTA:
//
// El veterinario podra seleccionar medicamentos
// ACTIVOS para utilizarlos durante una atencion medica.
//
// Los medicamentos INACTIVOS seguiran visibles
// para consulta, pero no para nuevos consumos.
//
// ==========================================================
// PERMISOS DEL USUARIO
// ==========================================================
//
// USER no puede administrar el catalogo.
//
// ==========================================================

@RestController
@RequestMapping("/api/medicamentos")
public class MedicamentoController {


    // ======================================================
    // 1. DEPENDENCIA DEL SERVICIO
    // ======================================================
    //
    // MedicamentoService contiene las validaciones
    // y las operaciones con PostgreSQL.
    //
    // ======================================================

    private final MedicamentoService medicamentoService;


    // ======================================================
    // 2. CONSTRUCTOR
    // ======================================================

    public MedicamentoController(
            MedicamentoService medicamentoService
    ) {

        this.medicamentoService = medicamentoService;

    }


    // ======================================================
    // 3. LISTAR TODOS LOS MEDICAMENTOS
    // ======================================================
    //
    // GET /api/medicamentos
    //
    // ACTUALIZADO:
    //
    // ADMIN y VETERINARIO.
    //
    // Devuelve medicamentos activos e inactivos.
    //
    // No permite modificar datos.
    //
    // ======================================================

    @Operation(
        summary = "Listar todos los medicamentos",
        description = "Permite al ADMIN y VETERINARIO "
                + "consultar todos los medicamentos, "
                + "incluidos activos e inactivos."
    )

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO')")

    public ResponseEntity<List<Medicamento>> listarTodos() {

        List<Medicamento> medicamentos =
                medicamentoService.listarTodos();

        return ResponseEntity.ok(medicamentos);

    }


    // ======================================================
    // 4. LISTAR MEDICAMENTOS ACTIVOS
    // ======================================================
    //
    // GET /api/medicamentos/activos
    //
    // ADMIN y VETERINARIO.
    //
    // Devuelve los productos habilitados.
    //
    // Estos medicamentos podran ser seleccionados
    // posteriormente para una atencion medica.
    //
    // ======================================================

    @Operation(
        summary = "Listar medicamentos activos",
        description = "Permite al ADMIN y VETERINARIO "
                + "consultar los medicamentos habilitados."
    )

    @GetMapping("/activos")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO')")

    public ResponseEntity<List<Medicamento>> listarActivos() {

        List<Medicamento> medicamentos =
                medicamentoService.listarActivos();

        return ResponseEntity.ok(medicamentos);

    }


    // ======================================================
    // 5. LISTAR MEDICAMENTOS INACTIVOS
    // ======================================================
    //
    // GET /api/medicamentos/inactivos
    //
    // ACTUALIZADO:
    //
    // ADMIN y VETERINARIO.
    //
    // El veterinario puede consultar medicamentos
    // que fueron desactivados por el administrador.
    //
    // IMPORTANTE:
    //
    // Consultar un medicamento inactivo NO significa
    // que pueda utilizarlo en una nueva atencion.
    //
    // ======================================================

    @Operation(
        summary = "Listar medicamentos inactivos",
        description = "Permite al ADMIN y VETERINARIO "
                + "consultar medicamentos desactivados."
    )

    @GetMapping("/inactivos")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO')")

    public ResponseEntity<List<Medicamento>> listarInactivos() {

        List<Medicamento> medicamentos =
                medicamentoService.listarInactivos();

        return ResponseEntity.ok(medicamentos);

    }


    // ======================================================
    // 6. BUSCAR MEDICAMENTO POR ID
    // ======================================================
    //
    // GET /api/medicamentos/{id}
    //
    // ACTUALIZADO:
    //
    // ADMIN y VETERINARIO.
    //
    // Permite buscar medicamentos activos o inactivos.
    //
    // Si el ID no existe, devuelve 404.
    //
    // ======================================================

    @Operation(
        summary = "Buscar medicamento por ID",
        description = "Permite al ADMIN y VETERINARIO "
                + "consultar cualquier medicamento "
                + "mediante su identificador."
    )

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO')")

    public ResponseEntity<Medicamento> buscarPorId(
            @PathVariable Long id
    ) {

        Medicamento medicamento =
                medicamentoService.buscarPorId(id);

        return ResponseEntity.ok(medicamento);

    }


    // ======================================================
    // 7. BUSCAR MEDICAMENTO ACTIVO POR ID
    // ======================================================
    //
    // GET /api/medicamentos/activos/{id}
    //
    // ADMIN y VETERINARIO.
    //
    // Solo devuelve medicamentos activos.
    //
    // Este endpoint servira para seleccionar
    // medicamentos disponibles durante las consultas.
    //
    // ======================================================

    @Operation(
        summary = "Buscar medicamento activo por ID",
        description = "Permite al ADMIN y VETERINARIO "
                + "consultar un medicamento activo."
    )

    @GetMapping("/activos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO')")

    public ResponseEntity<Medicamento> buscarActivoPorId(
            @PathVariable Long id
    ) {

        Medicamento medicamento =
                medicamentoService.buscarActivoPorId(id);

        return ResponseEntity.ok(medicamento);

    }


    // ======================================================
    // 8. REGISTRAR NUEVO MEDICAMENTO
    // ======================================================
    //
    // POST /api/medicamentos
    //
    // SOLO ADMIN.
    //
    // El administrador registra:
    //
    // - Nombre.
    // - Descripcion.
    // - Presentacion.
    // - Concentracion.
    // - Unidad de medida.
    // - Precio unitario.
    // - Stock inicial.
    //
    // El veterinario no puede utilizar este endpoint.
    //
    // ======================================================

    @Operation(
        summary = "Registrar medicamento",
        description = "Permite exclusivamente al ADMIN "
                + "registrar un nuevo medicamento."
    )

    @ApiResponse(
        responseCode = "201",
        description = "Medicamento registrado correctamente"
    )

    @ApiResponse(
        responseCode = "400",
        description = "Datos del medicamento invalidos"
    )

    @ApiResponse(
        responseCode = "403",
        description = "El usuario no tiene permisos"
    )

    @ApiResponse(
        responseCode = "409",
        description = "Ya existe un medicamento con ese nombre"
    )

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<Medicamento> crear(
            @Valid @RequestBody Medicamento medicamento
    ) {

        // Validar y guardar desde el servicio.
        Medicamento guardado =
                medicamentoService.crear(medicamento);

        // Devolver 201 Created.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(guardado);

    }


    // ======================================================
    // 9. ACTUALIZAR MEDICAMENTO
    // ======================================================
    //
    // PUT /api/medicamentos/{id}
    //
    // SOLO ADMIN.
    //
    // Puede actualizar:
    //
    // - Nombre.
    // - Descripcion.
    // - Presentacion.
    // - Concentracion.
    // - Unidad de medida.
    // - Precio unitario.
    // - Stock disponible.
    //
    // El veterinario NO puede modificar estos datos.
    //
    // ======================================================

    @Operation(
        summary = "Actualizar medicamento",
        description = "Permite exclusivamente al ADMIN "
                + "modificar la informacion, precio y stock "
                + "de un medicamento existente."
    )

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<Medicamento> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody Medicamento datos
    ) {

        Medicamento actualizado =
                medicamentoService.actualizar(
                        id,
                        datos
                );

        return ResponseEntity.ok(actualizado);

    }


    // ======================================================
    // 10. ACTIVAR MEDICAMENTO
    // ======================================================
    //
    // PUT /api/medicamentos/{id}/activar
    //
    // SOLO ADMIN.
    //
    // Cambia activo = true.
    //
    // El medicamento vuelve a estar disponible
    // para futuras atenciones medicas.
    //
    // ======================================================

    @Operation(
        summary = "Activar medicamento",
        description = "Permite al ADMIN habilitar nuevamente "
                + "un medicamento desactivado."
    )

    @PutMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<Medicamento> activar(
            @PathVariable Long id
    ) {

        Medicamento medicamento =
                medicamentoService.activar(id);

        return ResponseEntity.ok(medicamento);

    }


    // ======================================================
    // 11. DESACTIVAR MEDICAMENTO
    // ======================================================
    //
    // PUT /api/medicamentos/{id}/desactivar
    //
    // SOLO ADMIN.
    //
    // Cambia activo = false.
    //
    // El medicamento permanece en PostgreSQL.
    //
    // Puede seguir siendo consultado tanto
    // por ADMIN como por VETERINARIO.
    //
    // ======================================================

    @Operation(
        summary = "Desactivar medicamento",
        description = "Permite al ADMIN desactivar un medicamento "
                + "sin eliminarlo de PostgreSQL."
    )

    @PutMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<Medicamento> desactivar(
            @PathVariable Long id
    ) {

        Medicamento medicamento =
                medicamentoService.desactivar(id);

        return ResponseEntity.ok(medicamento);

    }


    // ======================================================
    // 12. ELIMINAR MEDICAMENTO (BAJA LOGICA)
    // ======================================================
    //
    // DELETE /api/medicamentos/{id}
    //
    // SOLO ADMIN.
    //
    // IMPORTANTE:
    //
    // No elimina fisicamente el medicamento.
    //
    // Realiza una baja logica:
    //
    // activo = false.
    //
    // Esto permite conservar los registros historicos.
    //
    // ======================================================

    @Operation(
        summary = "Eliminar medicamento",
        description = "Realiza una baja logica del medicamento. "
                + "El registro permanece en PostgreSQL."
    )

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<MensajeRespuesta> eliminar(
            @PathVariable Long id
    ) {

        // Desactivar el medicamento.
        medicamentoService.eliminar(id);

        // Confirmar la operacion.
        MensajeRespuesta respuesta =
                new MensajeRespuesta(
                        200,
                        "Medicamento desactivado correctamente",
                        "El medicamento fue dado de baja sin eliminar su historial"
                );

        return ResponseEntity.ok(respuesta);

    }

}
