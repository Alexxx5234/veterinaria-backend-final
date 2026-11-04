package com.veterinaria.veterinaria.controller;

// ==========================================================
// IMPORTACIONES
// ==========================================================
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;

import com.veterinaria.veterinaria.model.Veterinario;
import com.veterinaria.veterinaria.model.Usuario;
import com.veterinaria.veterinaria.model.Rol;
import com.veterinaria.veterinaria.model.EstadoUsuario;
import com.veterinaria.veterinaria.repository.UsuarioRepository;
import com.veterinaria.veterinaria.service.VeterinarioService;
import com.veterinaria.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.veterinaria.exception.MensajeRespuesta;

// ==========================================================
// CONTROLADOR DE VETERINARIOS
// ==========================================================
// USER: consulta veterinarios.
// ADMIN: consulta, registra, actualiza y elimina.
//
// REGISTRO NUEVO:
// El ADMIN elige una cuenta registrada y verificada.
// El sistema crea su ficha profesional, asigna el rol
// VETERINARIO y aprueba la cuenta, sin otro registro.
//
// COMPATIBILIDAD CON ANGULAR:
// Se conserva el POST antiguo, pero se oculta de Swagger
// mientras actualizamos el formulario de Angular.
// ==========================================================
@RestController
@RequestMapping("/api/veterinarios")
public class VeterinarioController {

    // Dependencias para gestionar fichas y consultar usuarios.
    private final VeterinarioService veterinarioService;
    private final UsuarioRepository usuarioRepository;

    // ======================================================
    // CONSTRUCTOR
    // ======================================================
    public VeterinarioController(
            VeterinarioService veterinarioService,
            UsuarioRepository usuarioRepository
    ) {
        this.veterinarioService = veterinarioService;
        this.usuarioRepository = usuarioRepository;
    }

    // ======================================================
    // 1. LISTAR VETERINARIOS - ORIGINAL SIN CAMBIOS
    // GET /api/veterinarios
    // ======================================================
    @Operation(
        summary = "Listar veterinarios",
        description = "Obtiene todos los veterinarios registrados en el sistema veterinario."
    )
    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public List<Veterinario> listarTodos() {
        return veterinarioService.listarTodos();
    }

    // ======================================================
    // 2. CONSULTAR VETERINARIO POR ID - ORIGINAL SIN CAMBIOS
    // GET /api/veterinarios/{id}
    // ======================================================
    @Operation(
        summary = "Consultar veterinario por ID",
        description = "Permite obtener la información de un veterinario específico mediante su identificador."
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<Veterinario> buscarPorId(@PathVariable Long id) {
        Veterinario veterinario = veterinarioService.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Veterinario no encontrado con id: " + id
                ));
        return ResponseEntity.ok(veterinario);
    }

    // ======================================================
    // 3. REGISTRO ANTIGUO - CONSERVADO PARA ANGULAR
    // POST /api/veterinarios
    // ======================================================
    // @Hidden oculta solamente esta operación de Swagger.
    // La ruta sigue funcionando para no romper Angular.
    // No vincula una cuenta de usuario ni asigna roles.
    // Se eliminará cuando Angular use /desde-usuario.
    // ======================================================
    @Hidden
    @Operation(
        summary = "Registrar veterinario",
        description = "Permite crear un nuevo veterinario dentro del sistema."
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Veterinario crear(@RequestBody Veterinario veterinario) {
        veterinario.setId(null);
        veterinario.setUsuario(null);
        return veterinarioService.guardar(veterinario);
    }

    // ======================================================
    // 4. REGISTRAR VETERINARIO DESDE UN USUARIO EXISTENTE
    // POST /api/veterinarios/desde-usuario
    // ======================================================
    // SOLO ADMIN.
    // Recibe usuarioId, cedula, especialidad y telefono.
    // Nombre y correo se obtienen de la cuenta registrada.
    // La transacción evita guardar solo una parte si falla.
    // ======================================================
    @Operation(
        summary = "Registrar veterinario desde usuario",
        description = "Crea una ficha profesional a partir de una cuenta verificada y le asigna el rol VETERINARIO."
    )
    @PostMapping("/desde-usuario")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Veterinario> crearDesdeUsuario(
            @Valid @RequestBody RegistroVeterinarioDesdeUsuarioRequest request
    ) {
        // 1. Buscar la cuenta que seleccionó el administrador.
        Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id: " + request.getUsuarioId()
                ));

        // 2. El correo debe estar verificado.
        if (!usuario.isVerificado()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El usuario debe verificar su correo primero"
            );
        }

        // 3. No convertir cuentas ADMIN en VETERINARIO.
        if (usuario.getRol() == Rol.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede convertir una cuenta ADMIN en VETERINARIO"
            );
        }

        // 4. No aprobar cuentas rechazadas o bloqueadas.
        if (usuario.getEstado() != EstadoUsuario.PENDIENTE
                && usuario.getEstado() != EstadoUsuario.APROBADA) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La cuenta debe estar PENDIENTE o APROBADA"
            );
        }

        // 5. Evitar dos fichas vinculadas a una misma cuenta.
        if (veterinarioService.buscarPorUsuarioId(usuario.getId()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este usuario ya tiene una ficha veterinaria vinculada"
            );
        }

        // 6. Evitar crear otra ficha con igual cédula o correo.
        String cedula = request.getCedula().trim();
        boolean profesionalDuplicado = veterinarioService.listarTodos()
                .stream()
                .anyMatch(veterinario ->
                        (veterinario.getCedula() != null
                                && veterinario.getCedula().equals(cedula))
                        ||
                        (veterinario.getEmail() != null
                                && veterinario.getEmail().equalsIgnoreCase(
                                        usuario.getCorreo()))
                );

        if (profesionalDuplicado) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe una ficha veterinaria con esta cédula o correo. "
                    + "Se debe revisar esa ficha antes de crear otra"
            );
        }

        // 7. Crear la ficha profesional sin repetir el registro.
        Veterinario veterinario = new Veterinario();
        veterinario.setId(null);
        veterinario.setCedula(cedula);
        veterinario.setNombre(usuario.getNombre());

        // Campo de compatibilidad; el nombre completo está en Usuario.
        veterinario.setApellido("");

        veterinario.setEspecialidad(request.getEspecialidad().trim());
        veterinario.setTelefono(request.getTelefono().trim());
        veterinario.setEmail(usuario.getCorreo());

        // 8. Vincular la ficha con la cuenta existente.
        veterinario.setUsuario(usuario);

        // 9. El administrador asigna el rol y habilita la cuenta.
        usuario.setRol(Rol.VETERINARIO);
        usuario.setEstado(EstadoUsuario.APROBADA);
        usuario.setActivo(true);

        // 10. Guardar ambos cambios dentro de la transacción.
        usuarioRepository.save(usuario);
        Veterinario veterinarioGuardado = veterinarioService.guardar(veterinario);

        // 11. Devolver la ficha creada y el código HTTP 201.
        return ResponseEntity.status(HttpStatus.CREATED).body(veterinarioGuardado);
    }

    // ======================================================
    // 5. ACTUALIZAR VETERINARIO - ORIGINAL SIN CAMBIOS
    // PUT /api/veterinarios/{id}
    // ======================================================
    @Operation(
        summary = "Actualizar veterinario",
        description = "Permite modificar la información de un veterinario existente mediante su identificador."
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Veterinario> actualizar(
            @PathVariable Long id,
            @RequestBody Veterinario datosVeterinario
    ) {
        // Buscar la ficha existente.
        Veterinario veterinarioExistente = veterinarioService.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Veterinario no encontrado con id: " + id
                ));

        // Conservar los cambios de datos profesionales originales.
        veterinarioExistente.setCedula(datosVeterinario.getCedula());
        veterinarioExistente.setNombre(datosVeterinario.getNombre());
        veterinarioExistente.setApellido(datosVeterinario.getApellido());
        veterinarioExistente.setEspecialidad(datosVeterinario.getEspecialidad());
        veterinarioExistente.setTelefono(datosVeterinario.getTelefono());
        veterinarioExistente.setEmail(datosVeterinario.getEmail());

        // No tocar usuario_id: se conserva la cuenta vinculada.
        Veterinario actualizado = veterinarioService.guardar(veterinarioExistente);
        return ResponseEntity.ok(actualizado);
    }

    // ======================================================
    // 6. ELIMINAR VETERINARIO - ORIGINAL SIN CAMBIOS
    // DELETE /api/veterinarios/{id}
    // ======================================================
    @Operation(
        summary = "Eliminar veterinario",
        description = "Permite eliminar un veterinario registrado mediante su identificador."
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        veterinarioService.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Veterinario no encontrado con id: " + id
                ));

        // Se conserva el comportamiento de eliminación original.
        veterinarioService.eliminar(id);
        return ResponseEntity.ok(new MensajeRespuesta(
                200,
                "Veterinario eliminado correctamente",
                "El registro fue eliminado del sistema"
        ));
    }

    // ======================================================
    // 7. DATOS RECIBIDOS PARA EL NUEVO REGISTRO
    // ======================================================
    // Swagger pedirá únicamente los datos profesionales y
    // el ID de una cuenta existente y verificada.
    // ======================================================
    public static class RegistroVeterinarioDesdeUsuarioRequest {

        @NotNull(message = "Debe seleccionar un usuario")
        private Long usuarioId;

        @NotBlank(message = "La cedula es obligatoria")
        @Size(min = 10, max = 10, message = "La cedula debe tener 10 caracteres")
        private String cedula;

        @NotBlank(message = "La especialidad es obligatoria")
        private String especialidad;

        @NotBlank(message = "El telefono es obligatorio")
        private String telefono;

        // Constructor necesario para recibir datos JSON.
        public RegistroVeterinarioDesdeUsuarioRequest() {
        }

        public Long getUsuarioId() {
            return usuarioId;
        }

        public void setUsuarioId(Long usuarioId) {
            this.usuarioId = usuarioId;
        }

        public String getCedula() {
            return cedula;
        }

        public void setCedula(String cedula) {
            this.cedula = cedula;
        }

        public String getEspecialidad() {
            return especialidad;
        }

        public void setEspecialidad(String especialidad) {
            this.especialidad = especialidad;
        }

        public String getTelefono() {
            return telefono;
        }

        public void setTelefono(String telefono) {
            this.telefono = telefono;
        }
    }
}
