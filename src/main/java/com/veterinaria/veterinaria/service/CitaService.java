package com.veterinaria.veterinaria.service;

// ==========================================================
// IMPORTACIONES
// ==========================================================

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.veterinaria.veterinaria.model.Cita;
import com.veterinaria.veterinaria.repository.CitaRepository;


// ==========================================================
// SERVICIO DE CITAS
// ==========================================================
//
// Contiene las operaciones necesarias para gestionar
// las citas registradas en el sistema veterinario.
//
// ADMIN:
// Puede gestionar todas las citas.
//
// USER:
// Puede consultar las citas de sus mascotas.
//
// VETERINARIO:
// Puede consultar las citas asignadas a su registro.
//
// ==========================================================

@Service
public class CitaService {


    // ======================================================
    // REPOSITORIO DE CITAS
    // ======================================================

    private final CitaRepository citaRepository;


    // ======================================================
    // CONSTRUCTOR
    // ======================================================

    public CitaService(CitaRepository citaRepository) {

        this.citaRepository = citaRepository;

    }


    // ======================================================
    // 1. LISTAR TODAS LAS CITAS
    // ======================================================
    //
    // MÉTODO ORIGINAL - SIN CAMBIOS
    //
    // Utilizado principalmente por ADMIN.
    //
    // ======================================================

    public List<Cita> listarTodos() {

        return citaRepository.findAll();

    }


    // ======================================================
    // 2. LISTAR CITAS DE UN USUARIO
    // ======================================================
    //
    // MÉTODO ORIGINAL - SIN CAMBIOS
    //
    // Devuelve las citas correspondientes a las
    // mascotas de un usuario.
    //
    // Relación:
    //
    // Usuario -> Mascota -> Cita
    //
    // ======================================================

    public List<Cita> listarPorUsuario(Long usuarioId) {

        return citaRepository.findByMascotaUsuarioId(usuarioId);

    }


    // ======================================================
    // 3. BUSCAR CITA POR ID
    // ======================================================
    //
    // MÉTODO ORIGINAL - SIN CAMBIOS
    //
    // Permite buscar cualquier cita por su identificador.
    //
    // ======================================================

    public Optional<Cita> buscarPorId(Long id) {

        return citaRepository.findById(id);

    }


    // ======================================================
    // 4. BUSCAR CITA POR ID Y USUARIO
    // ======================================================
    //
    // MÉTODO ORIGINAL - SIN CAMBIOS
    //
    // Comprueba que la cita pertenezca al usuario indicado.
    //
    // ======================================================

    public Optional<Cita> buscarPorIdYUsuario(
            Long id,
            Long usuarioId
    ) {

        return citaRepository.findByIdAndMascotaUsuarioId(
                id,
                usuarioId
        );

    }


    // ======================================================
    // 5. LISTAR CITAS DE UN VETERINARIO
    // ======================================================
    //
    // NUEVO MÉTODO
    //
    // Devuelve únicamente las citas asignadas
    // al veterinario indicado.
    //
    // Ejemplo:
    //
    // veterinarioId = 3
    //
    // PostgreSQL buscará las citas relacionadas
    // con el veterinario cuyo ID sea 3.
    //
    // IMPORTANTE:
    //
    // Este método recibe el ID del veterinario.
    // El controlador deberá obtener ese ID desde
    // la cuenta autenticada, nunca confiar en
    // un ID enviado libremente por el frontend.
    //
    // ======================================================

    public List<Cita> listarPorVeterinario(
            Long veterinarioId
    ) {

        return citaRepository.findByVeterinarioId(
                veterinarioId
        );

    }


    // ======================================================
    // 6. BUSCAR CITA POR ID Y VETERINARIO
    // ======================================================
    //
    // NUEVO MÉTODO
    //
    // Busca una cita específica y comprueba
    // que esté asignada al veterinario indicado.
    //
    // Ejemplo:
    //
    // citaId = 10
    // veterinarioId = 3
    //
    // Solo devuelve la cita si pertenece
    // al veterinario 3.
    //
    // ======================================================

    public Optional<Cita> buscarPorIdYVeterinario(
            Long id,
            Long veterinarioId
    ) {

        return citaRepository.findByIdAndVeterinarioId(
                id,
                veterinarioId
        );

    }


    // ======================================================
    // 7. GUARDAR CITA
    // ======================================================
    //
    // MÉTODO ORIGINAL - SIN CAMBIOS
    //
    // Permite crear o actualizar una cita.
    //
    // ======================================================

    public Cita guardar(Cita cita) {

        return citaRepository.save(cita);

    }


    // ======================================================
    // 8. ELIMINAR CITA
    // ======================================================
    //
    // MÉTODO ORIGINAL - SIN CAMBIOS
    //
    // La autorización para eliminar citas
    // se controla desde el controlador.
    //
    // ======================================================

    public void eliminar(Long id) {

        citaRepository.deleteById(id);

    }

}