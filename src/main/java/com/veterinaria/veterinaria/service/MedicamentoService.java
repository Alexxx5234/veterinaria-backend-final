
package com.veterinaria.veterinaria.service;

// ==========================================================
// IMPORTACIONES GENERALES
// ==========================================================

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// ==========================================================
// MODELO Y REPOSITORIO
// ==========================================================

import com.veterinaria.veterinaria.model.Medicamento;
import com.veterinaria.veterinaria.repository.MedicamentoRepository;

// ==========================================================
// EXCEPCIONES
// ==========================================================

import com.veterinaria.veterinaria.exception.ResourceNotFoundException;


// ==========================================================
// SERVICIO DE MEDICAMENTOS
// ==========================================================
//
// ADMIN:
// - Consultar medicamentos activos e inactivos.
// - Buscar cualquier medicamento por ID.
// - Crear medicamentos.
// - Actualizar medicamentos, precios y stock.
// - Activar y desactivar medicamentos.
// - Realizar bajas logicas.
//
// VETERINARIO:
// - Consultar TODOS los medicamentos.
// - Consultar medicamentos activos.
// - Consultar medicamentos inactivos.
// - Buscar cualquier medicamento por ID.
// - NO puede crear ni modificar medicamentos.
//
// USER:
// - No tiene acceso al catalogo interno.
//
// IMPORTANTE:
// Los medicamentos inactivos pueden consultarse,
// pero NO deben utilizarse en nuevas atenciones.
//
// ==========================================================

@Service
public class MedicamentoService {

    // ======================================================
    // DEPENDENCIA DEL REPOSITORIO
    // ======================================================

    private final MedicamentoRepository medicamentoRepository;

    // ======================================================
    // CONSTRUCTOR
    // ======================================================

    public MedicamentoService(
            MedicamentoRepository medicamentoRepository
    ) {
        this.medicamentoRepository = medicamentoRepository;
    }


    // ======================================================
    // 1. LISTAR TODOS LOS MEDICAMENTOS
    // ======================================================
    //
    // ACTUALIZADO:
    // ADMIN y VETERINARIO.
    //
    // Incluye medicamentos activos e inactivos.
    //
    // ======================================================

    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO')")
    @Transactional(readOnly = true)
    public List<Medicamento> listarTodos() {

        return medicamentoRepository.findAll();

    }


    // ======================================================
    // 2. LISTAR MEDICAMENTOS ACTIVOS
    // ======================================================
    //
    // ADMIN y VETERINARIO.
    //
    // Muestra los productos habilitados.
    //
    // ======================================================

    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO')")
    @Transactional(readOnly = true)
    public List<Medicamento> listarActivos() {

        return medicamentoRepository
                .findByActivoTrueOrderByNombreAsc();

    }


    // ======================================================
    // 3. LISTAR MEDICAMENTOS INACTIVOS
    // ======================================================
    //
    // ACTUALIZADO:
    // ADMIN y VETERINARIO.
    //
    // Permite consultar medicamentos desactivados.
    //
    // El veterinario puede conocer los productos
    // anteriores, pero no activarlos ni utilizarlos
    // en una nueva atencion medica.
    //
    // ======================================================

    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO')")
    @Transactional(readOnly = true)
    public List<Medicamento> listarInactivos() {

        return medicamentoRepository
                .findByActivoFalseOrderByNombreAsc();

    }


    // ======================================================
    // 4. BUSCAR MEDICAMENTO POR ID
    // ======================================================
    //
    // ACTUALIZADO:
    // ADMIN y VETERINARIO.
    //
    // Puede consultar medicamentos activos o inactivos.
    //
    // ======================================================

    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO')")
    @Transactional(readOnly = true)
    public Medicamento buscarPorId(Long id) {

        return obtenerMedicamento(id);

    }


    // ======================================================
    // 5. BUSCAR MEDICAMENTO ACTIVO POR ID
    // ======================================================
    //
    // ADMIN y VETERINARIO.
    //
    // Solo devuelve productos activos.
    //
    // Este metodo se utilizara al seleccionar
    // medicamentos para nuevas atenciones medicas.
    //
    // ======================================================

    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO')")
    @Transactional(readOnly = true)
    public Medicamento buscarActivoPorId(Long id) {

        return medicamentoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Medicamento activo no encontrado con id: " + id
                    )
                );

    }


    // ======================================================
    // 6. REGISTRAR NUEVO MEDICAMENTO
    // ======================================================
    //
    // SOLO ADMIN.
    //
    // El veterinario no puede crear medicamentos.
    //
    // Valida:
    // - Campos obligatorios.
    // - Nombre no duplicado.
    // - Precio no negativo.
    // - Stock no negativo.
    //
    // ======================================================

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Medicamento crear(Medicamento datos) {

        // 1. Validar que existan datos.
        if (datos == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe enviar los datos del medicamento"
            );

        }

        // 2. Validar los campos enviados.
        validarDatosMedicamento(datos);

        // 3. Evitar nombres duplicados.
        if (
                medicamentoRepository.existsByNombreIgnoreCase(
                        datos.getNombre()
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un medicamento con ese nombre"
            );

        }

        // 4. PostgreSQL genera el identificador.
        datos.setId(null);

        // 5. Los medicamentos nuevos comienzan activos.
        datos.setActivo(true);

        // 6. Guardar en PostgreSQL.
        return medicamentoRepository.save(datos);

    }


    // ======================================================
    // 7. ACTUALIZAR MEDICAMENTO
    // ======================================================
    //
    // SOLO ADMIN.
    //
    // Permite modificar:
    // - Nombre.
    // - Descripcion.
    // - Presentacion.
    // - Concentracion.
    // - Unidad de medida.
    // - Precio.
    // - Existencias.
    //
    // El veterinario no tiene acceso a esta funcion.
    //
    // ======================================================

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Medicamento actualizar(
            Long id,
            Medicamento datos
    ) {

        // 1. Buscar el registro existente.
        Medicamento existente = obtenerMedicamento(id);

        // 2. Comprobar que se enviaron datos.
        if (datos == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe enviar los datos del medicamento"
            );

        }

        // 3. Validar informacion.
        validarDatosMedicamento(datos);

        // 4. Evitar nombres duplicados.
        if (
                medicamentoRepository
                        .existsByNombreIgnoreCaseAndIdNot(
                                datos.getNombre(),
                                id
                        )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Otro medicamento ya utiliza ese nombre"
            );

        }

        // 5. Actualizar datos generales.
        existente.setNombre(
                datos.getNombre()
        );

        existente.setDescripcion(
                datos.getDescripcion()
        );

        existente.setPresentacion(
                datos.getPresentacion()
        );

        existente.setConcentracion(
                datos.getConcentracion()
        );

        existente.setUnidadMedida(
                datos.getUnidadMedida()
        );

        // 6. Actualizar precio.
        // Esta operacion es exclusiva del ADMIN.
        existente.setPrecioUnitario(
                datos.getPrecioUnitario()
        );

        // 7. Actualizar existencias.
        // Esta operacion es exclusiva del ADMIN.
        existente.setStockDisponible(
                datos.getStockDisponible()
        );

        // 8. Guardar sin modificar el ID ni activo.
        return medicamentoRepository.save(existente);

    }


    // ======================================================
    // 8. ACTIVAR MEDICAMENTO
    // ======================================================
    //
    // SOLO ADMIN.
    //
    // Cambia activo = true.
    //
    // ======================================================

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Medicamento activar(Long id) {

        Medicamento medicamento = obtenerMedicamento(id);

        medicamento.setActivo(true);

        return medicamentoRepository.save(medicamento);

    }


    // ======================================================
    // 9. DESACTIVAR MEDICAMENTO
    // ======================================================
    //
    // SOLO ADMIN.
    //
    // Cambia activo = false.
    //
    // El registro permanece en PostgreSQL.
    //
    // ======================================================

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Medicamento desactivar(Long id) {

        Medicamento medicamento = obtenerMedicamento(id);

        medicamento.setActivo(false);

        return medicamentoRepository.save(medicamento);

    }


    // ======================================================
    // 10. ELIMINAR MEDICAMENTO
    // ======================================================
    //
    // SOLO ADMIN.
    //
    // Se realiza una baja logica:
    // activo = false.
    //
    // No se borra fisicamente de PostgreSQL.
    //
    // Esto permite conservar el historial
    // para futuras atenciones y facturas.
    //
    // ======================================================

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Medicamento eliminar(Long id) {

        Medicamento medicamento = obtenerMedicamento(id);

        medicamento.setActivo(false);

        return medicamentoRepository.save(medicamento);

    }


    // ======================================================
    // METODO AUXILIAR: BUSCAR MEDICAMENTO
    // ======================================================
    //
    // Busca un medicamento por ID.
    //
    // Si no existe, genera una excepcion 404.
    //
    // ======================================================

    private Medicamento obtenerMedicamento(Long id) {

        return medicamentoRepository.findById(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Medicamento no encontrado con id: " + id
                    )
                );

    }


    // ======================================================
    // METODO AUXILIAR: VALIDAR DATOS
    // ======================================================
    //
    // Comprueba todos los campos obligatorios,
    // los limites de texto, precio y stock.
    //
    // ======================================================

    private void validarDatosMedicamento(
            Medicamento medicamento
    ) {

        // 1. Validar nombre.
        medicamento.setNombre(
                validarTextoObligatorio(
                        medicamento.getNombre(),
                        "nombre",
                        150
                )
        );

        // 2. Validar descripcion opcional.
        medicamento.setDescripcion(
                validarTextoOpcional(
                        medicamento.getDescripcion(),
                        "descripcion",
                        1000
                )
        );

        // 3. Validar presentacion.
        medicamento.setPresentacion(
                validarTextoObligatorio(
                        medicamento.getPresentacion(),
                        "presentacion",
                        100
                )
        );

        // 4. Validar concentracion opcional.
        medicamento.setConcentracion(
                validarTextoOpcional(
                        medicamento.getConcentracion(),
                        "concentracion",
                        100
                )
        );

        // 5. Validar unidad de medida.
        String unidad = validarTextoObligatorio(
                medicamento.getUnidadMedida(),
                "unidadMedida",
                30
        );

        medicamento.setUnidadMedida(
                unidad.toUpperCase(Locale.ROOT)
        );

        // 6. Validar precio.
        medicamento.setPrecioUnitario(
                validarCantidad(
                        medicamento.getPrecioUnitario(),
                        "precioUnitario"
                )
        );

        // 7. Validar stock.
        medicamento.setStockDisponible(
                validarCantidad(
                        medicamento.getStockDisponible(),
                        "stockDisponible"
                )
        );

    }


    // ======================================================
    // METODO AUXILIAR: TEXTO OBLIGATORIO
    // ======================================================

    private String validarTextoObligatorio(
            String valor,
            String campo,
            int longitudMaxima
    ) {

        if (valor == null || valor.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El campo " + campo + " es obligatorio"
            );

        }

        String texto = valor.trim();

        if (texto.length() > longitudMaxima) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El campo " + campo
                    + " no puede superar "
                    + longitudMaxima + " caracteres"
            );

        }

        return texto;

    }


    // ======================================================
    // METODO AUXILIAR: TEXTO OPCIONAL
    // ======================================================

    private String validarTextoOpcional(
            String valor,
            String campo,
            int longitudMaxima
    ) {

        if (valor == null || valor.isBlank()) {
            return null;
        }

        String texto = valor.trim();

        if (texto.length() > longitudMaxima) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El campo " + campo
                    + " no puede superar "
                    + longitudMaxima + " caracteres"
            );

        }

        return texto;

    }


    // ======================================================
    // METODO AUXILIAR: VALIDAR PRECIO Y STOCK
    // ======================================================
    //
    // - No puede ser null.
    // - No puede ser negativo.
    // - Maximo dos decimales.
    // - Maximo diez digitos enteros.
    //
    // ======================================================

    private BigDecimal validarCantidad(
            BigDecimal valor,
            String campo
    ) {

        // Validar que el dato exista.
        if (valor == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El campo " + campo + " es obligatorio"
            );

        }

        // Evitar valores negativos.
        if (valor.compareTo(BigDecimal.ZERO) < 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El campo " + campo + " no puede ser negativo"
            );

        }

        // Normalizar ceros finales.
        BigDecimal normalizado = valor.stripTrailingZeros();

        // Permitir como maximo dos decimales.
        if (normalizado.scale() > 2) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El campo " + campo
                    + " admite un maximo de dos decimales"
            );

        }

        // Comprobar limite de NUMERIC(12,2).
        int digitosEnteros =
                normalizado.precision() - normalizado.scale();

        if (digitosEnteros > 10) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El campo " + campo
                    + " supera el valor maximo permitido"
            );

        }

        // Guardar el valor con dos decimales.
        return valor.setScale(
                2,
                RoundingMode.UNNECESSARY
        );

    }

}
