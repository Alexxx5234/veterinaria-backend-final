
package com.veterinaria.veterinaria.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.veterinaria.model.AtencionMedica;

// ==========================================================
// REPOSITORIO DE ATENCIONES MEDICAS
// ==========================================================
//
// JpaRepository proporciona las operaciones CRUD:
// guardar, buscar, listar y eliminar.
//
// Las consultas adicionales permiten localizar atenciones
// por cita, mascota, veterinario y rango de fechas.
//
// IMPORTANTE:
// Los permisos se comprobaran en el servicio/controlador.
// ==========================================================

public interface AtencionMedicaRepository
        extends JpaRepository<AtencionMedica, Long> {

    // ======================================================
    // 1. BUSCAR ATENCION POR CITA
    // ======================================================
    // Cada cita puede tener una sola atencion medica.

    Optional<AtencionMedica> findByCita_Id(Long citaId);

    // ======================================================
    // 2. COMPROBAR SI YA EXISTE UNA ATENCION
    // ======================================================
    // Ayuda a evitar registros duplicados.

    boolean existsByCita_Id(Long citaId);

    // ======================================================
    // 3. CONSULTAR HISTORIAL CLINICO DE UNA MASCOTA
    // ======================================================
    // Ordenado desde la atencion mas reciente.

    List<AtencionMedica> findByCita_Mascota_IdOrderByFechaAtencionDesc(
            Long mascotaId
    );

    // ======================================================
    // 4. CONSULTAR ATENCIONES DE UN VETERINARIO
    // ======================================================
    // Permite obtener sus registros medicos.

    List<AtencionMedica> findByCita_Veterinario_IdOrderByFechaAtencionDesc(
            Long veterinarioId
    );

    // ======================================================
    // 5. CONSULTAR ATENCIONES POR RANGO DE FECHAS
    // ======================================================
    //
    // PASO 51 - NUEVA FUNCIONALIDAD
    //
    // Recupera las atenciones cuya fecha se encuentre
    // dentro del periodo solicitado.
    //
    // El limite inicial es inclusivo.
    // El limite final es exclusivo.
    //
    // Ejemplo:
    // desde = 2026-10-01 00:00:00
    // hastaExclusivo = 2026-10-21 00:00:00
    //
    // Esto incluye todas las atenciones realizadas
    // desde el 1 hasta el 20 de octubre.
    //
    // Los resultados se ordenan desde la fecha
    // mas reciente hasta la mas antigua.
    //
    // La autorizacion sera exclusiva para ADMIN
    // y se validara en el Service y Controller.
    //
    // ======================================================

    List<AtencionMedica>
    findByFechaAtencionGreaterThanEqualAndFechaAtencionLessThanOrderByFechaAtencionDesc(
            LocalDateTime desde,
            LocalDateTime hastaExclusivo
    );

}
