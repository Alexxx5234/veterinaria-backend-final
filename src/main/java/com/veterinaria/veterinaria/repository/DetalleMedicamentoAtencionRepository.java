
package com.veterinaria.veterinaria.repository;

// ==========================================================
// IMPORTACIONES GENERALES
// ==========================================================

// Permite trabajar con listas de registros.
import java.util.List;

// Proporciona las operaciones CRUD de Spring Data JPA.
import org.springframework.data.jpa.repository.JpaRepository;

// Entidad que relaciona medicamentos con atenciones.
import com.veterinaria.veterinaria.model.DetalleMedicamentoAtencion;

// Enum que identifica si un medicamento fue
// utilizado o solamente recetado.
import com.veterinaria.veterinaria.model.DetalleMedicamentoAtencion.TipoRegistro;


// ==========================================================
// REPOSITORIO DE DETALLES DE MEDICAMENTOS
// ==========================================================
//
// Se conecta con la tabla:
//
// detalle_medicamento_atencion
//
// JpaRepository proporciona automaticamente:
//
// - save(): guardar un detalle.
// - findAll(): listar registros.
// - findById(): buscar por ID.
// - existsById(): comprobar existencia.
// - deleteById(): eliminar un registro.
//
// IMPORTANTE:
//
// Este repositorio NO permite crear medicamentos
// en el catalogo.
//
// El catalogo y el inventario son administrados
// exclusivamente por ADMIN.
//
// Los permisos se comprobaran en el servicio
// y en el controlador.
//
// ==========================================================

public interface DetalleMedicamentoAtencionRepository
        extends JpaRepository<DetalleMedicamentoAtencion, Long> {


    // ======================================================
    // 1. LISTAR MEDICAMENTOS DE UNA ATENCION MEDICA
    // ======================================================
    //
    // Permite consultar todos los medicamentos
    // utilizados o recetados en una atencion.
    //
    // Ejemplo:
    //
    // Atencion ID 1:
    // - Antiseptico veterinario: UTILIZADO
    // - Amoxicilina: RECETADO
    //
    // Los resultados se ordenan por ID.
    //
    // ======================================================

    List<DetalleMedicamentoAtencion>
            findByAtencionMedica_IdOrderByIdAsc(
                    Long atencionMedicaId
            );


    // ======================================================
    // 2. COMPROBAR SI EXISTE UN DETALLE
    // ======================================================
    //
    // Permite consultar si una atencion ya tiene
    // un registro de un determinado medicamento
    // y tipo de uso.
    //
    // Ejemplo:
    //
    // Atencion: 1
    // Medicamento: 2
    // Tipo: UTILIZADO
    //
    // Este metodo ayudara a detectar posibles
    // registros repetidos.
    //
    // IMPORTANTE:
    //
    // La proteccion definitiva contra descuentos
    // duplicados se implementara en el servicio.
    //
    // ======================================================

    boolean existsByAtencionMedica_IdAndMedicamento_IdAndTipoRegistro(
            Long atencionMedicaId,
            Long medicamentoId,
            TipoRegistro tipoRegistro
    );


    // ======================================================
    // 3. CONSULTAR HISTORIAL DE UN MEDICAMENTO
    // ======================================================
    //
    // Permite conocer en que atenciones aparece
    // un medicamento determinado.
    //
    // Puede incluir registros UTILIZADOS y RECETADOS.
    //
    // Los resultados se ordenan desde el mas reciente.
    //
    // ======================================================

    List<DetalleMedicamentoAtencion>
            findByMedicamento_IdOrderByFechaRegistroDesc(
                    Long medicamentoId
            );


    // ======================================================
    // 4. CONSULTAR MEDICAMENTOS DE UNA MASCOTA
    // ======================================================
    //
    // Permite obtener los medicamentos registrados
    // en todas las atenciones de una mascota.
    //
    // Relacion:
    //
    // DetalleMedicamentoAtencion
    //            |
    //       AtencionMedica
    //            |
    //           Cita
    //            |
    //          Mascota
    //
    // Este metodo sera util para el historial
    // clinico de la mascota.
    //
    // ======================================================

    List<DetalleMedicamentoAtencion>
            findByAtencionMedica_Cita_Mascota_IdOrderByFechaRegistroDesc(
                    Long mascotaId
            );


    // ======================================================
    // 5. CONSULTAR SOLO MEDICAMENTOS UTILIZADOS
    // ======================================================
    //
    // Permite recuperar medicamentos segun el tipo.
    //
    // Ejemplo:
    //
    // Atencion ID 1
    // Tipo: UTILIZADO
    //
    // Los medicamentos UTILIZADOS tendran subtotal
    // y se consideraran para la futura factura.
    //
    // Los RECETADOS no se cobraran automaticamente.
    //
    // ======================================================

    List<DetalleMedicamentoAtencion>
            findByAtencionMedica_IdAndTipoRegistroOrderByIdAsc(
                    Long atencionMedicaId,
                    TipoRegistro tipoRegistro
            );


}
