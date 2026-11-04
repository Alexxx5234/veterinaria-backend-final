    
package com.veterinaria.veterinaria.repository;

// ==========================================================
// IMPORTACIONES
// ==========================================================

// Permite trabajar con listas de medicamentos.
import java.util.List;

// Permite devolver un medicamento si existe.
import java.util.Optional;

// Proporciona las operaciones CRUD de Spring Data JPA.
import org.springframework.data.jpa.repository.JpaRepository;

// Entidad que representa los medicamentos de la veterinaria.
import com.veterinaria.veterinaria.model.Medicamento;


// ==========================================================
// REPOSITORIO DE MEDICAMENTOS
// ==========================================================
//
// Este repositorio se conecta con la tabla "medicamentos"
// de PostgreSQL.
//
// JpaRepository proporciona automáticamente:
//
// - save(): guardar o actualizar.
// - findAll(): listar todos.
// - findById(): buscar por ID.
// - existsById(): comprobar si existe.
// - deleteById(): eliminar.
// - count(): contar registros.
//
// REGLA DE SEGURIDAD:
//
// El repositorio solamente realiza consultas.
// Los permisos para ADMIN, VETERINARIO y USER
// se implementarán en el servicio y controlador.
//
// ==========================================================

public interface MedicamentoRepository
        extends JpaRepository<Medicamento, Long> {


    // ======================================================
    // 1. BUSCAR MEDICAMENTO POR NOMBRE
    // ======================================================
    //
    // Permite localizar un medicamento por su nombre.
    //
    // IgnoreCase:
    // No distingue entre mayúsculas y minúsculas.
    //
    // ======================================================

    Optional<Medicamento> findByNombreIgnoreCase(
            String nombre
    );


    // ======================================================
    // 2. COMPROBAR SI YA EXISTE UN MEDICAMENTO
    // ======================================================
    //
    // Se utilizará cuando el ADMIN registre medicamentos.
    //
    // Ayuda a evitar registros con el mismo nombre.
    //
    // ======================================================

    boolean existsByNombreIgnoreCase(
            String nombre
    );


    // ======================================================
    // 3. VALIDAR NOMBRE AL EDITAR
    // ======================================================
    //
    // Comprueba si existe otro medicamento con ese nombre,
    // excluyendo el registro que está siendo actualizado.
    //
    // ======================================================

    boolean existsByNombreIgnoreCaseAndIdNot(
            String nombre,
            Long id
    );


    // ======================================================
    // 4. LISTAR MEDICAMENTOS ACTIVOS
    // ======================================================
    //
    // Permite mostrar únicamente medicamentos activos.
    //
    // Se ordenan alfabéticamente por nombre.
    //
    // El veterinario podrá consultar este listado
    // cuando registre una atención médica.
    //
    // ======================================================

    List<Medicamento> findByActivoTrueOrderByNombreAsc();


    // ======================================================
    // 5. BUSCAR MEDICAMENTO ACTIVO POR ID
    // ======================================================
    //
    // Permite seleccionar únicamente medicamentos
    // que estén habilitados por el ADMIN.
    //
    // ======================================================

    Optional<Medicamento> findByIdAndActivoTrue(
            Long id
    );


    // ======================================================
    // 6. LISTAR MEDICAMENTOS INACTIVOS
    // ======================================================
    //
    // Permite al ADMIN consultar productos desactivados.
    //
    // ======================================================

    List<Medicamento> findByActivoFalseOrderByNombreAsc();

}
