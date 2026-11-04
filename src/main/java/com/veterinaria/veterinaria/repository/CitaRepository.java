package com.veterinaria.veterinaria.repository;

// ==========================================================
// IMPORTACIONES
// ==========================================================

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.veterinaria.model.Cita;


// ==========================================================
// REPOSITORIO DE CITAS
// ==========================================================
//
// Permite consultar, guardar, actualizar y eliminar citas.
//
// También contiene consultas específicas para:
//
// - USER: consultar las citas de sus mascotas.
// - VETERINARIO: consultar las citas que tiene asignadas.
// - ADMIN: gestionar todas las citas mediante JpaRepository.
//
// ==========================================================

public interface CitaRepository extends JpaRepository<Cita, Long> {


    // ======================================================
    // 1. BUSCAR TODAS LAS CITAS DE UN USUARIO
    // ======================================================
    //
    // MÉTODO ORIGINAL - SIN CAMBIOS
    //
    // Relación:
    //
    // Cita
    //   ↓
    // Mascota
    //   ↓
    // Usuario
    //
    // Se utiliza para:
    //
    // GET /api/citas/mis-citas
    //
    // ======================================================

    List<Cita> findByMascotaUsuarioId(
            Long usuarioId
    );


    // ======================================================
    // 2. BUSCAR UNA CITA POR ID Y USUARIO
    // ======================================================
    //
    // MÉTODO ORIGINAL - SIN CAMBIOS
    //
    // Comprueba que la cita pertenezca
    // a las mascotas del usuario indicado.
    //
    // ======================================================

    Optional<Cita> findByIdAndMascotaUsuarioId(
            Long id,
            Long usuarioId
    );


    // ======================================================
    // 3. LISTAR CITAS DE UN VETERINARIO
    // ======================================================
    //
    // NUEVO MÉTODO
    //
    // Busca las citas asignadas a un veterinario.
    //
    // Relación:
    //
    // Cita
    //   ↓
    // Veterinario
    //   ↓
    // ID del veterinario
    //
    // Ejemplo:
    //
    // veterinarioId = 3
    //
    // Devuelve solamente las citas que tengan
    // veterinario_id = 3.
    //
    // Se utilizará para:
    //
    // GET /api/citas/mis-citas-veterinario
    //
    // ======================================================

    List<Cita> findByVeterinarioId(
            Long veterinarioId
    );


    // ======================================================
    // 4. BUSCAR CITA POR ID Y VETERINARIO
    // ======================================================
    //
    // NUEVO MÉTODO
    //
    // Permite comprobar que una cita específica
    // se encuentre asignada al veterinario indicado.
    //
    // Ejemplo:
    //
    // citaId = 10
    // veterinarioId = 3
    //
    // Solo devuelve la cita si realmente pertenece
    // al veterinario 3.
    //
    // Este método nos ayudará a proteger las
    // operaciones de aceptar, rechazar y completar.
    //
    // IMPORTANTE:
    // El veterinarioId debe obtenerse de la cuenta
    // autenticada, nunca confiar en un ID que
    // el usuario envíe libremente desde Angular.
    //
    // ======================================================

    Optional<Cita> findByIdAndVeterinarioId(
            Long id,
            Long veterinarioId
    );

}