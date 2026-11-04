
package com.veterinaria.veterinaria.model;

// ==========================================================
// IMPORTACIONES
// ==========================================================

// BigDecimal permite manejar precios y cantidades
// sin los errores de precision de float o double.
import java.math.BigDecimal;

// Anotaciones de JPA para guardar datos en PostgreSQL.
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Validaciones para evitar registros incorrectos.
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


// ==========================================================
// ENTIDAD: MEDICAMENTO
// ==========================================================
//
// Esta entidad representa los medicamentos registrados
// en el sistema de la veterinaria.
//
// REGLAS DEL SISTEMA:
//
// ADMIN:
// - Crear medicamentos.
// - Editar medicamentos.
// - Eliminar o desactivar medicamentos.
// - Administrar el inventario.
// - Establecer precios.
//
// VETERINARIO:
// - Consultar medicamentos disponibles.
// - Seleccionar medicamentos registrados por ADMIN.
// - Registrar los medicamentos utilizados o recetados.
//
// USER:
// - No puede crear ni modificar medicamentos.
//
// IMPORTANTE:
// Los permisos se implementaran en el controlador
// y en el servicio mediante Spring Security.
//
// ==========================================================

@Entity
@Table(name = "medicamentos")
public class Medicamento {

    // ======================================================
    // 1. IDENTIFICADOR DEL MEDICAMENTO
    // ======================================================
    //
    // PostgreSQL genera el ID automaticamente.
    //
    // ======================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // ======================================================
    // 2. NOMBRE DEL MEDICAMENTO
    // ======================================================
    //
    // Ejemplo:
    // Antiseptico veterinario
    //
    // ======================================================

    @NotBlank(message = "El nombre del medicamento es obligatorio")
    @Size(
        max = 150,
        message = "El nombre no puede superar 150 caracteres"
    )
    @Column(nullable = false, length = 150)
    private String nombre;


    // ======================================================
    // 3. DESCRIPCION
    // ======================================================
    //
    // Informacion general del producto.
    // Este campo es opcional.
    //
    // ======================================================

    @Size(
        max = 1000,
        message = "La descripcion no puede superar 1000 caracteres"
    )
    @Column(length = 1000)
    private String descripcion;


    // ======================================================
    // 4. PRESENTACION
    // ======================================================
    //
    // Ejemplos:
    // - Frasco de 100 ml
    // - Caja de 20 tabletas
    // - Ampolla de 2 ml
    //
    // ======================================================

    @NotBlank(message = "La presentacion es obligatoria")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String presentacion;


    // ======================================================
    // 5. CONCENTRACION
    // ======================================================
    //
    // Ejemplos:
    // - 10 mg/ml
    // - 250 mg
    //
    // Campo opcional.
    //
    // ======================================================

    @Size(max = 100)
    @Column(length = 100)
    private String concentracion;


    // ======================================================
    // 6. UNIDAD DE MEDIDA
    // ======================================================
    //
    // Indica como se contabiliza el inventario.
    //
    // Ejemplos:
    // - UNIDAD
    // - TABLETA
    // - ML
    // - DOSIS
    //
    // Tambien define a que cantidad corresponde
    // el precio unitario del medicamento.
    //
    // ======================================================

    @NotBlank(message = "La unidad de medida es obligatoria")
    @Size(max = 30)
    @Column(
        name = "unidad_medida",
        nullable = false,
        length = 30
    )
    private String unidadMedida;


    // ======================================================
    // 7. PRECIO UNITARIO
    // ======================================================
    //
    // SOLO ADMIN podra establecer este precio.
    //
    // Se utilizara despues para calcular el importe
    // de los medicamentos incluidos en una factura.
    //
    // Ejemplo:
    // Precio por tableta: 0.50
    //
    // ======================================================

    @NotNull(message = "El precio unitario es obligatorio")
    @DecimalMin(
        value = "0.00",
        message = "El precio no puede ser negativo"
    )
    @Digits(
        integer = 10,
        fraction = 2,
        message = "El precio admite maximo dos decimales"
    )
    @Column(
        name = "precio_unitario",
        nullable = false,
        precision = 12,
        scale = 2
    )
    private BigDecimal precioUnitario;


    // ======================================================
    // 8. STOCK DISPONIBLE
    // ======================================================
    //
    // Cantidad disponible en inventario.
    //
    // Ejemplos:
    // - 30 tabletas
    // - 150 ml
    // - 10 dosis
    //
    // SOLO ADMIN podra ajustar manualmente el stock.
    //
    // Mas adelante, cuando el veterinario utilice
    // un medicamento durante una atencion,
    // descontaremos automaticamente la cantidad.
    //
    // ======================================================

    @NotNull(message = "El stock es obligatorio")
    @DecimalMin(
        value = "0.00",
        message = "El stock no puede ser negativo"
    )
    @Digits(
        integer = 10,
        fraction = 2,
        message = "El stock admite maximo dos decimales"
    )
    @Column(
        name = "stock_disponible",
        nullable = false,
        precision = 12,
        scale = 2
    )
    private BigDecimal stockDisponible;


    // ======================================================
    // 9. ESTADO DEL MEDICAMENTO
    // ======================================================
    //
    // true: medicamento activo.
    // false: medicamento desactivado.
    //
    // Esto permitira dejar de ofrecer un medicamento
    // sin tener que borrar su historial.
    //
    // ======================================================

    @Column(nullable = false)
    private boolean activo = true;


    // ======================================================
    // 10. CONSTRUCTOR VACIO
    // ======================================================
    //
    // Necesario para JPA y Hibernate.
    //
    // ======================================================

    public Medicamento() {

    }


    // ======================================================
    // 11. GETTERS Y SETTERS
    // ======================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }


    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }


    public String getPresentacion() {
        return presentacion;
    }

    public void setPresentacion(String presentacion) {
        this.presentacion = presentacion;
    }


    public String getConcentracion() {
        return concentracion;
    }

    public void setConcentracion(String concentracion) {
        this.concentracion = concentracion;
    }


    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }


    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }


    public BigDecimal getStockDisponible() {
        return stockDisponible;
    }

    public void setStockDisponible(BigDecimal stockDisponible) {
        this.stockDisponible = stockDisponible;
    }


    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

}
