
package com.veterinaria.veterinaria.model;

// ==========================================================
// IMPORTACIONES GENERALES
// ==========================================================

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


// ==========================================================
// ENTIDAD: DETALLE DE MEDICAMENTOS EN UNA ATENCION
// ==========================================================
//
// Esta entidad permite relacionar una atencion medica
// con los medicamentos utilizados o recetados.
//
// Una atencion medica puede tener varios medicamentos.
//
// Un medicamento puede aparecer en diferentes atenciones.
//
// REGLAS DEL SISTEMA:
//
// ADMIN:
// - Crea y administra los medicamentos del catalogo.
// - Modifica precios y existencias.
//
// VETERINARIO:
// - Selecciona medicamentos ya registrados.
// - Indica la cantidad utilizada o recetada.
// - No puede crear medicamentos ni cambiar sus precios.
//
// USER:
// - No puede modificar medicamentos ni inventario.
//
// IMPORTANTE:
// Los permisos y el descuento de stock se aplicaran
// en el servicio y controlador, no en esta entidad.
//
// ==========================================================

@Entity
@Table(name = "detalle_medicamento_atencion")
public class DetalleMedicamentoAtencion {


    // ======================================================
    // 1. IDENTIFICADOR
    // ======================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // ======================================================
    // 2. ATENCION MEDICA
    // ======================================================
    //
    // Cada detalle pertenece a una atencion.
    //
    // Una atencion puede tener varios detalles.
    //
    // No utilizamos CascadeType.REMOVE para evitar
    // eliminar registros clinicos accidentalmente.
    //
    // ======================================================

    @NotNull(message = "La atencion medica es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atencion_medica_id", nullable = false)
    private AtencionMedica atencionMedica;


    // ======================================================
    // 3. MEDICAMENTO
    // ======================================================
    //
    // Se selecciona un medicamento que ADMIN
    // registro previamente en el catalogo.
    //
    // Nunca se crea un medicamento desde esta entidad.
    //
    // ======================================================

    @NotNull(message = "El medicamento es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;


    // ======================================================
    // 4. TIPO DE REGISTRO
    // ======================================================
    //
    // UTILIZADO:
    // Medicamento administrado durante la consulta.
    // Descontara stock y tendra un importe de cobro.
    //
    // RECETADO:
    // Medicamento prescrito para uso posterior.
    // No descuenta stock ni se cobra automaticamente.
    //
    // RECETADO_ENTREGADO:
    // Medicamento recetado y entregado por la veterinaria.
    // Descuenta stock y se cobra en la factura.
    //
    // ======================================================

    public enum TipoRegistro {
        UTILIZADO,
        RECETADO,
        RECETADO_ENTREGADO
    }

    @NotNull(message = "El tipo de registro es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoRegistro tipoRegistro;


    // ======================================================
    // 5. CANTIDAD
    // ======================================================
    //
    // Ejemplos:
    // 5.00 ML
    // 2.00 TABLETAS
    // 1.00 DOSIS
    //
    // La cantidad debe ser mayor que cero.
    //
    // ======================================================

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(
        value = "0.01",
        message = "La cantidad debe ser mayor que cero"
    )
    @Digits(
        integer = 10,
        fraction = 2,
        message = "La cantidad admite maximo dos decimales"
    )
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidad;


    // ======================================================
    // 6. NOMBRE DEL MEDICAMENTO REGISTRADO
    // ======================================================
    //
    // Guardaremos una copia del nombre del medicamento
    // al momento de registrar la atencion.
    //
    // Asi, si ADMIN cambia posteriormente su nombre,
    // el historial conservara el nombre original.
    //
    // ======================================================

    @Column(
        name = "nombre_medicamento",
        nullable = false,
        length = 150
    )
    private String nombreMedicamento;


    // ======================================================
    // 7. UNIDAD DE MEDIDA REGISTRADA
    // ======================================================
    //
    // Guarda la unidad que tenia el medicamento
    // cuando se utilizo o receto.
    //
    // Ejemplos:
    // ML, TABLETA, DOSIS, GRAMO.
    //
    // ======================================================

    @Column(
        name = "unidad_medida",
        nullable = false,
        length = 30
    )
    private String unidadMedida;


    // ======================================================
    // 8. PRECIO UNITARIO APLICADO
    // ======================================================
    //
    // Copia el precio desde el catalogo.
    //
    // El veterinario NO podra modificar este valor
    // al registrar una atencion.
    //
    // Se guardara el precio correspondiente
    // al momento de utilizar el medicamento.
    //
    // Esto sera importante para la futura factura.
    //
    // ======================================================

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(
        value = "0.00",
        message = "El precio no puede ser negativo"
    )
    @Column(
        name = "precio_unitario_aplicado",
        nullable = false,
        precision = 12,
        scale = 2
    )
    private BigDecimal precioUnitarioAplicado;


    // ======================================================
    // 9. SUBTOTAL
    // ======================================================
    //
    // Para UTILIZADO:
    // subtotal = cantidad * precioUnitarioAplicado
    //
    // Para RECETADO:
    // subtotal = 0.00
    //
    // El servicio calculara este valor.
    //
    // No se aceptara un subtotal enviado libremente
    // por el veterinario desde el frontend.
    //
    // ======================================================

    @NotNull(message = "El subtotal es obligatorio")
    @DecimalMin(
        value = "0.00",
        message = "El subtotal no puede ser negativo"
    )
    @Column(
        nullable = false,
        precision = 12,
        scale = 2
    )
    private BigDecimal subtotal;


    // ======================================================
    // 10. INDICACIONES
    // ======================================================
    //
    // Campo opcional.
    //
    // Permite registrar observaciones sobre el uso
    // o prescripcion de un medicamento.
    //
    // No es obligatorio cuando no existen indicaciones.
    //
    // ======================================================

    @Size(
        max = 1000,
        message = "Las indicaciones no pueden superar 1000 caracteres"
    )
    @Column(length = 1000)
    private String indicaciones;


    // ======================================================
    // 11. FECHA DE REGISTRO
    // ======================================================

    @Column(
        name = "fecha_registro",
        nullable = false,
        updatable = false
    )
    private LocalDateTime fechaRegistro;


    // ======================================================
    // 12. CONSTRUCTOR VACIO
    // ======================================================

    public DetalleMedicamentoAtencion() {

    }


    // ======================================================
    // 13. ASIGNAR FECHA AUTOMATICAMENTE
    // ======================================================

    @PrePersist
    public void establecerFechaRegistro() {

        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }

    }


    // ======================================================
    // 14. GETTERS Y SETTERS
    // ======================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public AtencionMedica getAtencionMedica() {
        return atencionMedica;
    }

    public void setAtencionMedica(AtencionMedica atencionMedica) {
        this.atencionMedica = atencionMedica;
    }


    public Medicamento getMedicamento() {
        return medicamento;
    }

    public void setMedicamento(Medicamento medicamento) {
        this.medicamento = medicamento;
    }


    public TipoRegistro getTipoRegistro() {
        return tipoRegistro;
    }

    public void setTipoRegistro(TipoRegistro tipoRegistro) {
        this.tipoRegistro = tipoRegistro;
    }


    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }


    public String getNombreMedicamento() {
        return nombreMedicamento;
    }

    public void setNombreMedicamento(String nombreMedicamento) {
        this.nombreMedicamento = nombreMedicamento;
    }


    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }


    public BigDecimal getPrecioUnitarioAplicado() {
        return precioUnitarioAplicado;
    }

    public void setPrecioUnitarioAplicado(
            BigDecimal precioUnitarioAplicado
    ) {
        this.precioUnitarioAplicado = precioUnitarioAplicado;
    }


    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }


    public String getIndicaciones() {
        return indicaciones;
    }

    public void setIndicaciones(String indicaciones) {
        this.indicaciones = indicaciones;
    }


    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

}
