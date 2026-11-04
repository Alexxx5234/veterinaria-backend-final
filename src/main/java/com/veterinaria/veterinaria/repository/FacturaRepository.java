
package com.veterinaria.veterinaria.repository;

// ==========================================================
// IMPORTACIONES GENERALES
// ==========================================================

// Permite trabajar con listas de facturas.
import java.util.List;

// Permite buscar registros que pueden no existir.
import java.util.Optional;

// Proporciona las operaciones CRUD de Spring Data JPA.
import org.springframework.data.jpa.repository.JpaRepository;

// Entidad Factura registrada en PostgreSQL.
import com.veterinaria.veterinaria.model.Factura;


// ==========================================================
// REPOSITORIO DE FACTURAS
// ==========================================================
//
// Permite realizar operaciones sobre la tabla facturas.
//
// FUNCIONALIDADES:
//
// - Guardar facturas.
// - Buscar facturas por atencion medica.
// - Evitar facturas duplicadas.
// - Buscar por numero de factura.
// - Consultar todas las facturas.
// - Consultar facturas por veterinario.
// - Consultar facturas de un propietario.
//
// IMPORTANTE:
//
// Los permisos se verificaran en FacturaService
// y FacturaController.
//
// ==========================================================

public interface FacturaRepository
        extends JpaRepository<Factura, Long> {


    // ======================================================
    // 1. BUSCAR FACTURA POR ATENCION MEDICA
    // ======================================================
    //
    // Una atencion medica puede tener una sola factura.
    //
    // Permite recuperar automaticamente la factura
    // de una atencion ya registrada.
    //
    // ======================================================

    Optional<Factura> findByAtencionMedica_Id(
            Long atencionMedicaId
    );


    // ======================================================
    // 2. COMPROBAR SI YA EXISTE UNA FACTURA
    // ======================================================
    //
    // Evita intentar generar dos facturas diferentes
    // para la misma atencion medica.
    //
    // La restriccion UNIQUE de la entidad tambien
    // protegera este registro en PostgreSQL.
    //
    // ======================================================

    boolean existsByAtencionMedica_Id(
            Long atencionMedicaId
    );


    // ======================================================
    // 3. BUSCAR FACTURA POR NUMERO
    // ======================================================
    //
    // Permite localizar una factura utilizando
    // su numero identificador.
    //
    // ======================================================

    Optional<Factura> findByNumeroFactura(
            String numeroFactura
    );


    // ======================================================
    // 4. LISTAR TODAS LAS FACTURAS
    // ======================================================
    //
    // Utilizado por ADMIN.
    //
    // Devuelve las facturas desde la mas reciente
    // hasta la mas antigua.
    //
    // ======================================================

    List<Factura> findAllByOrderByFechaCreacionDesc();


    // ======================================================
    // 5. CONSULTAR FACTURAS POR VETERINARIO
    // ======================================================
    //
    // Permite obtener las facturas correspondientes
    // a las atenciones de un veterinario.
    //
    // El Service verificara que el veterinario
    // autenticado consulte solo las suyas.
    //
    // ======================================================

    List<Factura>
    findByAtencionMedica_Cita_Veterinario_IdOrderByFechaCreacionDesc(
            Long veterinarioId
    );


    // ======================================================
    // 6. CONSULTAR FACTURAS DEL PROPIETARIO
    // ======================================================
    //
    // El propietario puede consultar las facturas
    // correspondientes a sus mascotas.
    //
    // La identificacion se obtendra mediante JWT.
    //
    // ======================================================

    List<Factura>
    findByAtencionMedica_Cita_Mascota_Usuario_IdOrderByFechaCreacionDesc(
            Long usuarioId
    );

}
