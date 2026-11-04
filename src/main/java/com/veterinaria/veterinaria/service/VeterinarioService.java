package com.veterinaria.veterinaria.service;

// ==========================================================
// IMPORTACIONES
// ==========================================================

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.veterinaria.veterinaria.model.Veterinario;
import com.veterinaria.veterinaria.repository.VeterinarioRepository;


// ==========================================================
// SERVICIO DE VETERINARIOS
// ==========================================================
//
// Contiene las operaciones para gestionar los
// veterinarios registrados en el sistema.
//
// ADMIN:
// Puede registrar, consultar, actualizar y eliminar.
//
// USER:
// Puede consultar los veterinarios disponibles.
//
// VETERINARIO:
// Podrá consultar las citas que tenga asignadas
// mediante su cuenta de usuario vinculada.
//
// ==========================================================

@Service
public class VeterinarioService {


    // ======================================================
    // REPOSITORIO DE VETERINARIOS
    // ======================================================

    private final VeterinarioRepository veterinarioRepository;


    // ======================================================
    // CONSTRUCTOR
    // ======================================================

    public VeterinarioService(
            VeterinarioRepository veterinarioRepository
    ) {

        this.veterinarioRepository = veterinarioRepository;

    }


    // ======================================================
    // 1. LISTAR TODOS LOS VETERINARIOS
    // ======================================================
    //
    // MÉTODO ORIGINAL - SIN CAMBIOS
    //
    // Obtiene todos los veterinarios registrados.
    //
    // ======================================================

    public List<Veterinario> listarTodos() {

        return veterinarioRepository.findAll();

    }


    // ======================================================
    // 2. BUSCAR VETERINARIO POR ID
    // ======================================================
    //
    // MÉTODO ORIGINAL - SIN CAMBIOS
    //
    // Busca un veterinario por su identificador.
    //
    // ======================================================

    public Optional<Veterinario> buscarPorId(Long id) {

        return veterinarioRepository.findById(id);

    }


    // ======================================================
    // 3. BUSCAR VETERINARIO POR USUARIO
    // ======================================================
    //
    // NUEVO MÉTODO
    //
    // Permite identificar al veterinario que está
    // vinculado con una cuenta de usuario.
    //
    // Ejemplo:
    //
    // Usuario:
    // ID = 8
    // Rol = VETERINARIO
    //
    // Veterinario:
    // ID = 3
    // usuario_id = 8
    //
    // Si buscamos el usuario 8, el sistema
    // encontrará al veterinario 3.
    //
    // IMPORTANTE:
    //
    // Este método solamente realiza la consulta.
    // La validación de permisos se realiza
    // desde el controlador.
    //
    // ======================================================

    public Optional<Veterinario> buscarPorUsuarioId(
            Long usuarioId
    ) {

        return veterinarioRepository.findByUsuarioId(
                usuarioId
        );

    }


    // ======================================================
    // 4. GUARDAR VETERINARIO
    // ======================================================
    //
    // MÉTODO ORIGINAL - SIN CAMBIOS
    //
    // Se utiliza tanto para registrar un veterinario
    // como para actualizar sus datos.
    //
    // También permite guardar la vinculación
    // entre Veterinario y Usuario.
    //
    // ======================================================

    public Veterinario guardar(Veterinario veterinario) {

        return veterinarioRepository.save(veterinario);

    }


    // ======================================================
    // 5. ELIMINAR VETERINARIO
    // ======================================================
    //
    // MÉTODO ORIGINAL - SIN CAMBIOS
    //
    // Elimina un veterinario por su identificador.
    //
    // ======================================================

    public void eliminar(Long id) {

        veterinarioRepository.deleteById(id);

    }

}