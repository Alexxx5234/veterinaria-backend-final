        package com.veterinaria.veterinaria.controller;


        import java.util.List;


        import org.springframework.http.HttpStatus;
        import org.springframework.http.ResponseEntity;

        import org.springframework.security.access.prepost.PreAuthorize;
        import org.springframework.security.core.Authentication;

        import org.springframework.web.bind.annotation.*;

        import org.springframework.web.server.ResponseStatusException;


        import io.swagger.v3.oas.annotations.Operation;


        import com.veterinaria.veterinaria.model.Mascota;
        import com.veterinaria.veterinaria.model.Usuario;

        import com.veterinaria.veterinaria.repository.UsuarioRepository;

        import com.veterinaria.veterinaria.service.MascotaService;


        import com.veterinaria.veterinaria.exception.ResourceNotFoundException;
        import com.veterinaria.veterinaria.exception.MensajeRespuesta;



        @RestController
        @RequestMapping("/api/mascotas")
        public class MascotaController {


        private final MascotaService mascotaService;

        private final UsuarioRepository usuarioRepository;



        /*
        * ========================================================
        * CONSTRUCTOR
        * ========================================================
        */

        public MascotaController(
                MascotaService mascotaService,
                UsuarioRepository usuarioRepository
        ) {

                this.mascotaService = mascotaService;

                this.usuarioRepository = usuarioRepository;

        }



        /*
        * ========================================================
        * LISTAR TODAS LAS MASCOTAS
        *
        * Solo ADMIN puede ver todas las mascotas
        * registradas en el sistema.
        * ========================================================
        */

        @Operation(
                summary = "Listar todas las mascotas",
                description = "Obtiene todas las mascotas registradas. Disponible únicamente para administradores."
        )

        @GetMapping
        @PreAuthorize("hasRole('ADMIN')")

        public List<Mascota> listarTodos() {

                return mascotaService.listarTodos();

        }



        /*
        * ========================================================
        * LISTAR MIS MASCOTAS
        *
        * El usuario autenticado solamente verá
        * las mascotas asociadas a su propia cuenta.
        * ========================================================
        */

        @Operation(
                summary = "Listar mis mascotas",
                description = "Obtiene únicamente las mascotas pertenecientes al usuario autenticado."
        )

        @GetMapping("/mis-mascotas")
        @PreAuthorize("hasAnyRole('USER','ADMIN')")

        public List<Mascota> listarMisMascotas(
                Authentication authentication
        ) {


                Usuario usuario =
                        obtenerUsuarioAutenticado(
                                authentication
                        );


                return mascotaService.listarPorUsuario(
                        usuario.getId()
                );

        }



        /*
        * ========================================================
        * CONSULTAR MASCOTA POR ID
        *
        * USER:
        * Solo puede consultar una mascota propia.
        *
        * ADMIN:
        * Puede consultar cualquier mascota.
        * ========================================================
        */

        @Operation(
                summary = "Consultar mascota por ID",
                description = "El usuario puede consultar únicamente sus propias mascotas. El administrador puede consultar cualquier mascota."
        )

        @GetMapping("/{id}")
        @PreAuthorize("hasAnyRole('USER','ADMIN')")

        public ResponseEntity<Mascota> buscarPorId(

                @PathVariable Long id,

                Authentication authentication
        ) {


                Mascota mascota =
                        mascotaService.buscarPorId(id)

                        .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Mascota no encontrada con id: " + id
                        )
                        );



                /*
                * Comprueba que el usuario tenga permiso
                * para acceder a la mascota.
                */

                validarAccesoMascota(
                        mascota,
                        authentication
                );



                return ResponseEntity.ok(mascota);

        }



        /*
        * ========================================================
        * REGISTRAR MASCOTA
        *
        * USER:
        * La mascota queda asociada automáticamente
        * al usuario autenticado.
        *
        * No necesitamos enviar usuario_id desde Angular.
        *
        * ADMIN:
        * Puede enviar un usuario con su ID
        * para registrar una mascota a nombre de otro usuario.
        * ========================================================
        */

        @Operation(
                summary = "Registrar mascota",
                description = "El usuario registra una mascota que se asocia automáticamente a su cuenta."
        )

        @PostMapping
        @PreAuthorize("hasAnyRole('USER','ADMIN')")

        public Mascota crear(

                @RequestBody Mascota mascota,

                Authentication authentication
        ) {


                Usuario usuarioAutenticado =
                        obtenerUsuarioAutenticado(
                                authentication
                        );



                /*
                * Si es ADMIN y envía un usuario,
                * puede registrar una mascota
                * para ese usuario.
                */

                if (

                esAdmin(authentication)

                &&

                mascota.getUsuario() != null

                &&

                mascota.getUsuario().getId() != null

                ) {


                Long usuarioId =
                        mascota
                        .getUsuario()
                        .getId();



                Usuario usuarioDueno =
                        usuarioRepository
                        .findById(usuarioId)

                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                "Usuario no encontrado con id: "
                                + usuarioId
                                )
                        );



                mascota.setUsuario(
                        usuarioDueno
                );


                } else {


                /*
                * Si es USER:
                *
                * Siempre se utiliza el usuario
                * identificado mediante el JWT.
                *
                * Aunque intente enviar otro usuario_id,
                * será reemplazado.
                */

                mascota.setUsuario(
                        usuarioAutenticado
                );

                }



                /*
                * Evita que el usuario intente
                * crear una mascota enviando manualmente
                * un ID existente.
                */

                mascota.setId(null);



                return mascotaService.guardar(
                        mascota
                );

        }



        /*
        * ========================================================
        * ACTUALIZAR MASCOTA
        *
        * USER:
        * Puede modificar solamente sus mascotas.
        *
        * ADMIN:
        * Puede modificar cualquier mascota.
        * ========================================================
        */

        @Operation(
                summary = "Actualizar mascota",
                description = "El usuario puede modificar únicamente sus mascotas. El administrador puede modificar cualquier mascota."
        )

        @PutMapping("/{id}")
        @PreAuthorize("hasAnyRole('USER','ADMIN')")

        public ResponseEntity<Mascota> actualizar(

                @PathVariable Long id,

                @RequestBody Mascota datosMascota,

                Authentication authentication
        ) {


                Mascota mascotaExistente =
                        mascotaService.buscarPorId(id)

                        .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Mascota no encontrada con id: " + id
                        )
                        );



                /*
                * Verifica que la mascota pertenezca
                * al usuario.
                */

                validarAccesoMascota(
                        mascotaExistente,
                        authentication
                );



                /*
                * Actualizamos únicamente
                * los datos permitidos.
                */

                mascotaExistente.setNombre(
                        datosMascota.getNombre()
                );


                mascotaExistente.setEspecie(
                        datosMascota.getEspecie()
                );


                mascotaExistente.setRaza(
                        datosMascota.getRaza()
                );


                mascotaExistente.setEdad(
                        datosMascota.getEdad()
                );


                mascotaExistente.setSexo(
                        datosMascota.getSexo()
                );



                /*
                * SOLO EL ADMIN puede cambiar
                * el dueño de una mascota.
                */

                if (

                esAdmin(authentication)

                &&

                datosMascota.getUsuario() != null

                &&

                datosMascota
                        .getUsuario()
                        .getId() != null

                ) {


                Long nuevoUsuarioId =
                        datosMascota
                        .getUsuario()
                        .getId();



                Usuario nuevoDueno =
                        usuarioRepository
                        .findById(nuevoUsuarioId)

                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                "Usuario no encontrado con id: "
                                + nuevoUsuarioId
                                )
                        );



                mascotaExistente.setUsuario(
                        nuevoDueno
                );

                }



                Mascota mascotaActualizada =
                        mascotaService.guardar(
                                mascotaExistente
                        );



                return ResponseEntity.ok(
                        mascotaActualizada
                );

        }



        /*
        * ========================================================
        * ELIMINAR MASCOTA
        *
        * Solo ADMIN.
        *
        * Un USER no puede eliminar mascotas.
        * ========================================================
        */

        @Operation(
                summary = "Eliminar mascota",
                description = "Permite eliminar una mascota. Disponible únicamente para administradores."
        )

        @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")

        public ResponseEntity<?> eliminar(
                @PathVariable Long id
        ) {


                mascotaService.buscarPorId(id)

                        .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Mascota no encontrada con id: " + id
                        )
                        );



                mascotaService.eliminar(id);



                return ResponseEntity.ok(

                new MensajeRespuesta(

                        200,

                        "Mascota eliminada correctamente",

                        "El registro fue eliminado del sistema"

                )

                );

        }



        /*
        * ========================================================
        * OBTENER USUARIO AUTENTICADO
        *
        * El JwtAuthenticationFilter guarda
        * el correo como nombre del usuario
        * autenticado.
        * ========================================================
        */

        private Usuario obtenerUsuarioAutenticado(
                Authentication authentication
        ) {


                String correo =
                        authentication.getName();



                return usuarioRepository
                        .findByCorreo(correo)

                        .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Usuario autenticado no encontrado"
                        )
                        );

        }



        /*
        * ========================================================
        * COMPROBAR SI ES ADMIN
        * ========================================================
        */

        private boolean esAdmin(
                Authentication authentication
        ) {


                return authentication
                        .getAuthorities()

                        .stream()

                        .anyMatch(

                        authority ->

                                authority
                                .getAuthority()
                                .equals("ROLE_ADMIN")

                        );

        }



        /*
        * ========================================================
        * VALIDAR QUE LA MASCOTA PERTENEZCA AL USER
        *
        * ADMIN:
        * Puede acceder siempre.
        *
        * USER:
        * Solo puede acceder si usuario_id
        * coincide con su propio ID.
        * ========================================================
        */

        private void validarAccesoMascota(

                Mascota mascota,

                Authentication authentication
        ) {


                /*
                * Si es ADMIN no se hace
                * ninguna restricción.
                */

                if (esAdmin(authentication)) {

                return;

                }



                Usuario usuarioAutenticado =
                        obtenerUsuarioAutenticado(
                                authentication
                        );



                /*
                * Comprobamos el dueño.
                */

                if (

                mascota.getUsuario() == null

                ||

                !mascota
                        .getUsuario()
                        .getId()
                        .equals(
                        usuarioAutenticado.getId()
                        )

                ) {


                throw new ResponseStatusException(

                        HttpStatus.FORBIDDEN,

                        "No tienes permiso para acceder a esta mascota"

                );

                }

        }


        }