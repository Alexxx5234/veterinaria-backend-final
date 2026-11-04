package com.veterinaria.veterinaria.repository;

// ==========================================================
// IMPORTACIONES
// ==========================================================

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.veterinaria.model.Veterinario;


// ==========================================================
// REPOSITORIO DE VETERINARIOS
// ==========================================================
//
// Permite realizar operaciones sobre la tabla
// veterinarios de PostgreSQL.
//
// OPERACIONES ORIGINALES:
//
// - Registrar veterinarios.
// - Consultar veterinarios.
// - Actualizar veterinarios.
// - Eliminar veterinarios.
//
// NUEVA FUNCIONALIDAD:
//
// - Buscar un veterinario mediante el ID de
//   su cuenta de usuario vinculada.
//
// ==========================================================

public interface VeterinarioRepository
        extends JpaRepository<Veterinario, Long> {


    // ======================================================
    // 1. BUSCAR VETERINARIO POR USUARIO
    // ======================================================
    //
    // NUEVO MÉTODO
    //
    // Permite identificar qué veterinario está
    // vinculado con una cuenta de usuario.
    //
    // Ejemplo:
    //
    // TABLA USUARIOS:
    //
    // id = 8
    // correo = maria@gmail.com
    // rol = VETERINARIO
    //
    // TABLA VETERINARIOS:
    //
    // id = 3
    // nombre = Maria
    // usuario_id = 8
    //
    // Al buscar por usuarioId = 8,
    // PostgreSQL encontrará al veterinario ID 3.
    //
    // Optional:
    // Permite controlar cuando una cuenta todavía
    // no está vinculada a ningún veterinario.
    //
    // Spring Data JPA genera automáticamente
    // la consulta utilizando la relación
    // Veterinario -> Usuario.
    //
    // ======================================================

    Optional<Veterinario> findByUsuarioId(
            Long usuarioId
    );

}