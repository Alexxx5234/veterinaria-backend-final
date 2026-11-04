
package com.veterinaria.veterinaria.controller;

// ==========================================================
// IMPORTACIONES
// ==========================================================

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;

import com.veterinaria.veterinaria.model.Usuario;
import com.veterinaria.veterinaria.model.EstadoUsuario;
import com.veterinaria.veterinaria.repository.UsuarioRepository;
import com.veterinaria.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.veterinaria.exception.MensajeRespuesta;


// ==========================================================
// CONTROLADOR DEL ADMINISTRADOR
// ==========================================================
//
// Permite gestionar las cuentas registradas:
//
// - Consultar usuarios
// - Consultar usuarios pendientes
// - Aprobar usuarios
// - Rechazar usuarios
// - Bloquear usuarios
// - Activar usuarios
// - Eliminar usuarios
//
// CAMBIO:
// Eliminamos el antiguo endpoint para asignar
// el rol VETERINARIO.
//
// Ahora esa operación se realiza automáticamente
// cuando el ADMIN registra un veterinario desde
// una cuenta existente, utilizando:
//
// POST /api/veterinarios/desde-usuario
//
// ==========================================================

@RestController
@RequestMapping("/api/admin")
public class AdminController {


    // ======================================================
    // REPOSITORIO DE USUARIOS
    // ======================================================

    private final UsuarioRepository usuarioRepository;


    // ======================================================
    // CONSTRUCTOR
    // ======================================================

    public AdminController(
            UsuarioRepository usuarioRepository
    ) {

        this.usuarioRepository = usuarioRepository;

    }


    // ======================================================
    // 1. VER TODOS LOS USUARIOS
    // ======================================================
    //
    // GET /api/admin/usuarios
    //
    // Solo ADMIN puede consultar todos los usuarios.
    //
    // ======================================================

    @Operation(
        summary = "Consultar todos los usuarios",
        description = "Permite al administrador visualizar todos los usuarios registrados en el sistema."
    )
    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<List<Usuario>> listarUsuarios() {

        return ResponseEntity.ok(
                usuarioRepository.findAll()
        );

    }


    // ======================================================
    // 2. VER USUARIOS PENDIENTES
    // ======================================================
    //
    // GET /api/admin/usuarios-pendientes
    //
    // Muestra las cuentas pendientes de aprobación.
    //
    // ======================================================

    @Operation(
        summary = "Consultar usuarios pendientes",
        description = "Permite al administrador visualizar las cuentas nuevas que se encuentran pendientes de aprobación."
    )
    @GetMapping("/usuarios-pendientes")
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<List<Usuario>> usuariosPendientes() {

        return ResponseEntity.ok(

                usuarioRepository.findByEstado(
                        EstadoUsuario.PENDIENTE
                )

        );

    }


    // ======================================================
    // 3. APROBAR USUARIO
    // ======================================================
    //
    // PUT /api/admin/aprobar/{id}
    //
    // MÉTODO ORIGINAL - SIN CAMBIOS
    //
    // ======================================================

    @Operation(
        summary = "Aprobar usuario",
        description = "Permite al administrador aprobar una cuenta pendiente para habilitar su acceso al sistema."
    )
    @PutMapping("/aprobar/{id}")
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<?> aprobar(
            @PathVariable Long id
    ) {

        Usuario usuario = buscarUsuario(id);

        usuario.setEstado(
                EstadoUsuario.APROBADA
        );

        usuarioRepository.save(usuario);

        return ResponseEntity.ok(

                new MensajeRespuesta(
                        200,
                        "Usuario aprobado correctamente",
                        "La cuenta fue habilitada por el administrador"
                )

        );

    }


    // ======================================================
    // 4. RECHAZAR USUARIO
    // ======================================================
    //
    // PUT /api/admin/rechazar/{id}
    //
    // MÉTODO ORIGINAL - SIN CAMBIOS
    //
    // ======================================================

    @Operation(
        summary = "Rechazar usuario",
        description = "Permite al administrador rechazar una solicitud de registro de usuario."
    )
    @PutMapping("/rechazar/{id}")
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<?> rechazar(
            @PathVariable Long id
    ) {

        Usuario usuario = buscarUsuario(id);

        usuario.setEstado(
                EstadoUsuario.RECHAZADA
        );

        usuarioRepository.save(usuario);

        return ResponseEntity.ok(

                new MensajeRespuesta(
                        200,
                        "Usuario rechazado correctamente",
                        "La solicitud de registro fue rechazada por el administrador"
                )

        );

    }


    // ======================================================
    // 5. BLOQUEAR USUARIO
    // ======================================================
    //
    // PUT /api/admin/bloquear/{id}
    //
    // MÉTODO ORIGINAL - SIN CAMBIOS
    //
    // ======================================================

    @Operation(
        summary = "Bloquear usuario",
        description = "Permite al administrador bloquear una cuenta activa impidiendo el acceso al sistema."
    )
    @PutMapping("/bloquear/{id}")
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<?> bloquear(
            @PathVariable Long id
    ) {

        Usuario usuario = buscarUsuario(id);

        // Desactivar la cuenta.
        usuario.setActivo(false);

        // Cambiar su estado.
        usuario.setEstado(
                EstadoUsuario.BLOQUEADA
        );

        usuarioRepository.save(usuario);

        return ResponseEntity.ok(

                new MensajeRespuesta(
                        200,
                        "Usuario bloqueado correctamente",
                        "La cuenta fue bloqueada y no podrá acceder al sistema"
                )

        );

    }


    // ======================================================
    // 6. ACTIVAR USUARIO
    // ======================================================
    //
    // PUT /api/admin/activar/{id}
    //
    // MÉTODO ORIGINAL - SIN CAMBIOS
    //
    // ======================================================

    @Operation(
        summary = "Activar usuario",
        description = "Permite al administrador reactivar una cuenta bloqueada y devolverle el acceso al sistema."
    )
    @PutMapping("/activar/{id}")
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<?> activar(
            @PathVariable Long id
    ) {

        Usuario usuario = buscarUsuario(id);

        // Activar la cuenta.
        usuario.setActivo(true);

        // Cambiar el estado a APROBADA.
        usuario.setEstado(
                EstadoUsuario.APROBADA
        );

        usuarioRepository.save(usuario);

        return ResponseEntity.ok(

                new MensajeRespuesta(
                        200,
                        "Usuario activado correctamente",
                        "La cuenta fue habilitada nuevamente"
                )

        );

    }


    // ======================================================
    // 7. ELIMINAR USUARIO
    // ======================================================
    //
    // DELETE /api/admin/usuarios/{id}
    //
    // MÉTODO ORIGINAL - SIN CAMBIOS
    //
    // Solo ADMIN puede eliminar usuarios.
    //
    // ======================================================

    @Operation(
        summary = "Eliminar usuario",
        description = "Permite al administrador eliminar una cuenta del sistema mediante su identificador."
    )
    @DeleteMapping("/usuarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<?> eliminar(
            @PathVariable Long id
    ) {

        Usuario usuario = buscarUsuario(id);

        usuarioRepository.delete(usuario);

        return ResponseEntity.ok(

                new MensajeRespuesta(
                        200,
                        "Usuario eliminado correctamente",
                        "El registro fue eliminado del sistema"
                )

        );

    }


    // ======================================================
    // MÉTODO AUXILIAR
    // ======================================================
    //
    // Busca un usuario mediante su ID.
    //
    // Si no existe, devuelve un error 404.
    //
    // ======================================================

    private Usuario buscarUsuario(Long id) {

        return usuarioRepository
                .findById(id)

                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Usuario no encontrado con id: " + id
                    )
                );

    }

}
