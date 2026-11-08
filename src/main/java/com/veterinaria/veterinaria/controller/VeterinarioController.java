
package com.veterinaria.veterinaria.controller;

// ==========================================================
// VETERINARIA ARÉVALO
// CONTROLADOR DE VETERINARIOS
//
// FUNCIONES:
// 1. Listar veterinarios.
// 2. Consultar veterinario por ID.
// 3. Mantener ruta antigua de registro.
// 4. Registrar veterinario desde usuario existente.
// 5. Actualizar datos profesionales.
// 6. Eliminar ficha profesional de forma segura.
//
// SEGURIDAD:
// - USER y ADMIN pueden consultar.
// - Solo ADMIN puede registrar, actualizar y eliminar.
// - No se crean cuentas duplicadas.
// - Se conserva la contraseña del usuario.
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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;

import com.veterinaria.veterinaria.model.Veterinario;
import com.veterinaria.veterinaria.model.Usuario;
import com.veterinaria.veterinaria.model.Rol;
import com.veterinaria.veterinaria.model.EstadoUsuario;

import com.veterinaria.veterinaria.repository.UsuarioRepository;
import com.veterinaria.veterinaria.repository.CitaRepository;

import com.veterinaria.veterinaria.service.VeterinarioService;

import com.veterinaria.veterinaria.exception.ResourceNotFoundException;
import com.veterinaria.veterinaria.exception.MensajeRespuesta;


// ==========================================================
// CONTROLADOR PRINCIPAL
// ==========================================================

@RestController
@RequestMapping("/api/veterinarios")
public class VeterinarioController {

    // ======================================================
    // 1. DEPENDENCIAS
    // ======================================================

    private final VeterinarioService veterinarioService;

    private final UsuarioRepository usuarioRepository;

    private final CitaRepository citaRepository;


    // ======================================================
    // 2. CONSTRUCTOR
    // ======================================================

    public VeterinarioController(
            VeterinarioService veterinarioService,
            UsuarioRepository usuarioRepository,
            CitaRepository citaRepository
    ) {

        this.veterinarioService = veterinarioService;

        this.usuarioRepository = usuarioRepository;

        this.citaRepository = citaRepository;

    }


    // ======================================================
    // 3. LISTAR TODOS LOS VETERINARIOS
    //
    // GET /api/veterinarios
    //
    // USER y ADMIN pueden consultar.
    // ======================================================

    @Operation(
        summary = "Listar veterinarios",
        description = "Obtiene todos los veterinarios registrados."
    )
    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public List<Veterinario> listarTodos() {

        return veterinarioService.listarTodos();

    }


    // ======================================================
    // 4. CONSULTAR VETERINARIO POR ID
    //
    // GET /api/veterinarios/{id}
    // ======================================================

    @Operation(
        summary = "Consultar veterinario por ID",
        description = "Obtiene la información de un veterinario."
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<Veterinario> buscarPorId(
            @PathVariable Long id
    ) {

        Veterinario veterinario = veterinarioService.buscarPorId(id)

                .orElseThrow(() -> new ResourceNotFoundException(
                        "Veterinario no encontrado con id: " + id
                ));

        return ResponseEntity.ok(veterinario);

    }


    // ======================================================
    // 5. REGISTRO ANTIGUO
    //
    // POST /api/veterinarios
    //
    // Se conserva temporalmente por compatibilidad.
    // El nuevo formulario debe utilizar /desde-usuario.
    // ======================================================

    @Hidden
    @Operation(
        summary = "Registro antiguo de veterinario",
        description = "Ruta conservada temporalmente por compatibilidad."
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Veterinario crear(
            @RequestBody Veterinario veterinario
    ) {

        veterinario.setId(null);

        veterinario.setUsuario(null);

        return veterinarioService.guardar(veterinario);

    }


    // ======================================================
    // 6. REGISTRAR VETERINARIO DESDE USUARIO EXISTENTE
    //
    // POST /api/veterinarios/desde-usuario
    //
    // DATOS RECIBIDOS:
    // - usuarioId
    // - cedula
    // - especialidad
    // - telefono
    //
    // NOMBRE Y CORREO:
    // Se obtienen de la cuenta seleccionada.
    //
    // RESULTADO:
    // - Se crea la ficha profesional.
    // - Se vincula con la cuenta original.
    // - Se asigna rol VETERINARIO.
    // - Se aprueba y activa la cuenta.
    // - Se conserva la contraseña.
    // ======================================================

    @Operation(
        summary = "Registrar veterinario desde usuario",
        description = "Crea una ficha profesional desde una cuenta USER verificada."
    )
    @PostMapping("/desde-usuario")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Veterinario> crearDesdeUsuario(
            @Valid @RequestBody RegistroVeterinarioDesdeUsuarioRequest request
    ) {

        // --------------------------------------------------
        // 6.1. BUSCAR LA CUENTA
        // --------------------------------------------------

        Usuario usuario = usuarioRepository.findById(
                request.getUsuarioId()
        ).orElseThrow(() -> new ResourceNotFoundException(
                "Usuario no encontrado con id: " + request.getUsuarioId()
        ));


        // --------------------------------------------------
        // 6.2. COMPROBAR VERIFICACIÓN DEL CORREO
        // --------------------------------------------------

        if (!usuario.isVerificado()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El usuario debe verificar su correo primero"
            );

        }


        // --------------------------------------------------
        // 6.3. COMPROBAR ROL ACTUAL
        // --------------------------------------------------

        if (usuario.getRol() != Rol.USER) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Solo se pueden asignar cuentas con rol USER"
            );

        }


        // --------------------------------------------------
        // 6.4. COMPROBAR ESTADO
        // --------------------------------------------------

        if (
                usuario.getEstado() != EstadoUsuario.PENDIENTE
                &&
                usuario.getEstado() != EstadoUsuario.APROBADA
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La cuenta debe estar PENDIENTE o APROBADA"
            );

        }


        // --------------------------------------------------
        // 6.5. EVITAR CUENTAS DUPLICADAS
        // --------------------------------------------------

        if (
                veterinarioService
                        .buscarPorUsuarioId(usuario.getId())
                        .isPresent()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este usuario ya tiene una ficha veterinaria vinculada"
            );

        }


        // --------------------------------------------------
        // 6.6. EVITAR CÉDULAS Y CORREOS DUPLICADOS
        // --------------------------------------------------

        String cedula = request.getCedula().trim();

        String correo = usuario.getCorreo();

        boolean profesionalDuplicado =
                veterinarioService.listarTodos()

                .stream()

                .anyMatch(veterinario ->

                        (
                            veterinario.getCedula() != null
                            &&
                            veterinario.getCedula()
                                    .trim()
                                    .equals(cedula)
                        )

                        ||

                        (
                            veterinario.getEmail() != null
                            &&
                            correo != null
                            &&
                            veterinario.getEmail()
                                    .trim()
                                    .equalsIgnoreCase(correo.trim())
                        )

                );


        if (profesionalDuplicado) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe una ficha veterinaria con esta cédula o correo."
            );

        }


        // --------------------------------------------------
        // 6.7. CREAR LA FICHA PROFESIONAL
        // --------------------------------------------------

        Veterinario veterinario = new Veterinario();

        veterinario.setId(null);

        veterinario.setCedula(cedula);

        veterinario.setNombre(usuario.getNombre());

        // Se conserva el campo por compatibilidad
        // con la entidad Veterinario.
        veterinario.setApellido("");

        veterinario.setEspecialidad(
                request.getEspecialidad().trim()
        );

        veterinario.setTelefono(
                request.getTelefono().trim()
        );

        veterinario.setEmail(correo);


        // --------------------------------------------------
        // 6.8. VINCULAR LA CUENTA EXISTENTE
        // --------------------------------------------------

        veterinario.setUsuario(usuario);


        // --------------------------------------------------
        // 6.9. ASIGNAR EL ROL PROFESIONAL
        // --------------------------------------------------

        usuario.setRol(Rol.VETERINARIO);

        usuario.setEstado(EstadoUsuario.APROBADA);

        usuario.setActivo(true);


        // --------------------------------------------------
        // 6.10. GUARDAR AMBOS REGISTROS
        // --------------------------------------------------

        usuarioRepository.save(usuario);

        Veterinario veterinarioGuardado =
                veterinarioService.guardar(veterinario);


        // --------------------------------------------------
        // 6.11. RESPUESTA
        // --------------------------------------------------

        return ResponseEntity

                .status(HttpStatus.CREATED)

                .body(veterinarioGuardado);

    }


    // ======================================================
    // 7. ACTUALIZAR VETERINARIO
    //
    // PUT /api/veterinarios/{id}
    //
    // No se modifica la cuenta vinculada.
    // ======================================================

    @Operation(
        summary = "Actualizar veterinario",
        description = "Actualiza los datos profesionales del veterinario."
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Veterinario> actualizar(

            @PathVariable Long id,

            @RequestBody Veterinario datosVeterinario

    ) {

        Veterinario veterinarioExistente =
                veterinarioService.buscarPorId(id)

                .orElseThrow(() -> new ResourceNotFoundException(
                        "Veterinario no encontrado con id: " + id
                ));


        veterinarioExistente.setCedula(
                datosVeterinario.getCedula()
        );

        veterinarioExistente.setNombre(
                datosVeterinario.getNombre()
        );

        veterinarioExistente.setApellido(
                datosVeterinario.getApellido()
        );

        veterinarioExistente.setEspecialidad(
                datosVeterinario.getEspecialidad()
        );

        veterinarioExistente.setTelefono(
                datosVeterinario.getTelefono()
        );

        veterinarioExistente.setEmail(
                datosVeterinario.getEmail()
        );


        // Se conserva usuario_id y su relación original.

        Veterinario actualizado =
                veterinarioService.guardar(veterinarioExistente);

        return ResponseEntity.ok(actualizado);

    }


    // ======================================================
    // 8. ELIMINAR VETERINARIO DE FORMA SEGURA
    //
    // DELETE /api/veterinarios/{id}
    //
    // REGLAS:
    // - Solo ADMIN.
    // - No eliminar si existen citas asociadas.
    // - Devolver la cuenta vinculada al rol USER.
    // - Conservar contraseña, estado y activo.
    // ======================================================

    @Operation(
        summary = "Eliminar ficha de veterinario",
        description = "Elimina una ficha sin citas y devuelve la cuenta vinculada al rol USER."
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<MensajeRespuesta> eliminar(
            @PathVariable Long id
    ) {

        // Buscar la ficha profesional.

        Veterinario veterinario =
                veterinarioService.buscarPorId(id)

                .orElseThrow(() -> new ResourceNotFoundException(
                        "Veterinario no encontrado con id: " + id
                ));


        // No eliminar profesionales con citas asociadas.

        if (!citaRepository.findByVeterinarioId(id).isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede eliminar este veterinario porque tiene citas asociadas. "
                    + "Primero debes gestionar o reasignar sus citas."
            );

        }


        // Recuperar la cuenta vinculada.

        Usuario usuario = veterinario.getUsuario();


        // Devolver el rol a USER.
        // Se conserva el estado y el indicador activo.

        if (usuario != null) {

            usuario.setRol(Rol.USER);

            usuarioRepository.save(usuario);

        }


        // Eliminar únicamente la ficha profesional.

        veterinarioService.eliminar(id);


        String detalle = usuario != null

                ? "La ficha fue eliminada. La cuenta conserva su contraseña, estado y condición de activación, y vuelve al rol USER."

                : "La ficha profesional fue eliminada. No tenía una cuenta vinculada.";


        return ResponseEntity.ok(

                new MensajeRespuesta(
                        200,
                        "Veterinario eliminado correctamente",
                        detalle
                )

        );

    }


    // ======================================================
    // 9. DTO PARA REGISTRO DESDE USUARIO
    //
    // ESTRUCTURA JSON:
    //
    // {
    //   "usuarioId": 5,
    //   "cedula": "0102030405",
    //   "especialidad": "Medicina general",
    //   "telefono": "0991234567"
    // }
    // ======================================================

    public static class RegistroVeterinarioDesdeUsuarioRequest {


        @NotNull(
            message = "Debe seleccionar un usuario"
        )
        private Long usuarioId;


        @NotBlank(
            message = "La cédula es obligatoria"
        )
        @Size(
            min = 10,
            max = 10,
            message = "La cédula debe tener 10 caracteres"
        )
        @Pattern(
            regexp = "^[0-9]{10}$",
            message = "La cédula debe contener exactamente 10 números"
        )
        private String cedula;


        @NotBlank(
            message = "La especialidad es obligatoria"
        )
        private String especialidad;


        @NotBlank(
            message = "El teléfono es obligatorio"
        )
        private String telefono;


        // Constructor vacío para Jackson.

        public RegistroVeterinarioDesdeUsuarioRequest() {
        }


        // GETTERS Y SETTERS

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
