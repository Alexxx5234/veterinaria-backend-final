
package com.veterinaria.veterinaria.model;

// ==========================================================
// IMPORTACIONES
// ==========================================================

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import jakarta.validation.constraints.NotNull;

// ==========================================================
// ENTIDAD FACTURA
// ==========================================================
//
// Una factura pertenece a una sola atencion medica.
//
// El sistema recuperara automaticamente:
// - Propietario y correo.
// - Mascota.
// - Veterinario.
// - Servicio y precio.
// - Medicamentos registrados y sus importes.
//
// El veterinario podra emitir la factura.
// ADMIN podra consultar y supervisar las facturas.
//
// No se permite generar dos facturas para
// una misma atencion medica.
//
// ==========================================================

@Entity
@Table(name = "facturas")
public class Factura {

    // ======================================================
    // 1. IDENTIFICADOR
    // ======================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ======================================================
    // 2. NUMERO DE FACTURA
    // ======================================================
    //
    // Se asignara al emitir la factura.
    // Debe ser unico.
    //
    // ======================================================

    @Column(
        name = "numero_factura",
        unique = true,
        length = 30
    )
    private String numeroFactura;

    // ======================================================
    // 3. ATENCION MEDICA
    // ======================================================
    //
    // Relacion uno a uno.
    // Una atencion solamente puede tener una factura.
    //
    // ======================================================

    @NotNull
    @OneToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "atencion_medica_id",
        nullable = false,
        unique = true
    )
    @JsonIgnore
    private AtencionMedica atencionMedica;

    // ======================================================
    // 4. DATOS DEL PROPIETARIO
    // ======================================================
    //
    // Se obtendran desde Usuario.
    // Se conserva una copia historica.
    //
    // ======================================================

    @Column(
        name = "nombre_cliente",
        nullable = false
    )
    private String nombreCliente;

    @Column(
        name = "correo_cliente",
        nullable = false
    )
    private String correoCliente;

    // ======================================================
    // 5. DATOS DE LA MASCOTA
    // ======================================================

    @Column(
        name = "nombre_mascota",
        nullable = false
    )
    private String nombreMascota;

    // ======================================================
    // 6. VETERINARIO RESPONSABLE
    // ======================================================

    @Column(
        name = "nombre_veterinario",
        nullable = false
    )
    private String nombreVeterinario;

    // ======================================================
    // 7. SERVICIO REALIZADO
    // ======================================================
    //
    // Nombre y precio tomados de la cita.
    //
    // ======================================================

    @Column(
        name = "nombre_servicio",
        nullable = false
    )
    private String nombreServicio;

    @Column(
        name = "precio_servicio",
        nullable = false,
        precision = 12,
        scale = 2
    )
    private BigDecimal precioServicio = BigDecimal.ZERO;

    // ======================================================
    // 8. TOTAL DE MEDICAMENTOS FACTURABLES
    // ======================================================
    //
    // Suma los subtotales de:
    // UTILIZADO y RECETADO_ENTREGADO.
    //
    // RECETADO no genera cobro.
    //
    // ======================================================

    @Column(
        name = "total_medicamentos",
        nullable = false,
        precision = 12,
        scale = 2
    )
    private BigDecimal totalMedicamentos = BigDecimal.ZERO;

    // ======================================================
    // 9. SUBTOTAL, IMPUESTOS Y TOTAL
    // ======================================================
    //
    // Los importes seran calculados por el Service.
    //
    // El impuesto comienza en cero hasta configurar
    // las reglas tributarias aplicables.
    //
    // ======================================================

    @Column(
        nullable = false,
        precision = 12,
        scale = 2
    )
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(
        nullable = false,
        precision = 12,
        scale = 2
    )
    private BigDecimal impuestos = BigDecimal.ZERO;

    @Column(
        nullable = false,
        precision = 12,
        scale = 2
    )
    private BigDecimal total = BigDecimal.ZERO;

    // ======================================================
    // 10. ESTADO DE LA FACTURA
    // ======================================================

    public enum EstadoFactura {
        PENDIENTE,
        EMITIDA
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoFactura estado = EstadoFactura.PENDIENTE;

    // ======================================================
    // 11. CONTROL DEL ENVIO POR CORREO
    // ======================================================
    //
    // Solo se marcara como enviado cuando el servicio
    // de correo confirme la entrega al servidor SMTP.
    //
    // ======================================================

    @Column(
        name = "correo_enviado",
        nullable = false
    )
    private boolean correoEnviado = false;

    @Column(name = "fecha_envio_correo")
    private LocalDateTime fechaEnvioCorreo;

    // ======================================================
    // 12. FECHAS
    // ======================================================

    @Column(
        name = "fecha_creacion",
        nullable = false,
        updatable = false
    )
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_emision")
    private LocalDateTime fechaEmision;

    // ======================================================
    // 13. CONSTRUCTOR
    // ======================================================

    public Factura() {
    }

    // ======================================================
    // 14. FECHA AUTOMATICA
    // ======================================================

    @PrePersist
    public void establecerFechaCreacion() {

        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }

    }

    // ======================================================
    // 15. GETTERS Y SETTERS
    // ======================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroFactura() {
        return numeroFactura;
    }

    public void setNumeroFactura(String numeroFactura) {
        this.numeroFactura = numeroFactura;
    }

    public AtencionMedica getAtencionMedica() {
        return atencionMedica;
    }

    public void setAtencionMedica(AtencionMedica atencionMedica) {
        this.atencionMedica = atencionMedica;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getCorreoCliente() {
        return correoCliente;
    }

    public void setCorreoCliente(String correoCliente) {
        this.correoCliente = correoCliente;
    }

    public String getNombreMascota() {
        return nombreMascota;
    }

    public void setNombreMascota(String nombreMascota) {
        this.nombreMascota = nombreMascota;
    }

    public String getNombreVeterinario() {
        return nombreVeterinario;
    }

    public void setNombreVeterinario(String nombreVeterinario) {
        this.nombreVeterinario = nombreVeterinario;
    }

    public String getNombreServicio() {
        return nombreServicio;
    }

    public void setNombreServicio(String nombreServicio) {
        this.nombreServicio = nombreServicio;
    }

    public BigDecimal getPrecioServicio() {
        return precioServicio;
    }

    public void setPrecioServicio(BigDecimal precioServicio) {
        this.precioServicio = precioServicio;
    }

    public BigDecimal getTotalMedicamentos() {
        return totalMedicamentos;
    }

    public void setTotalMedicamentos(BigDecimal totalMedicamentos) {
        this.totalMedicamentos = totalMedicamentos;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getImpuestos() {
        return impuestos;
    }

    public void setImpuestos(BigDecimal impuestos) {
        this.impuestos = impuestos;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public EstadoFactura getEstado() {
        return estado;
    }

    public void setEstado(EstadoFactura estado) {
        this.estado = estado;
    }

    public boolean isCorreoEnviado() {
        return correoEnviado;
    }

    public void setCorreoEnviado(boolean correoEnviado) {
        this.correoEnviado = correoEnviado;
    }

    public LocalDateTime getFechaEnvioCorreo() {
        return fechaEnvioCorreo;
    }

    public void setFechaEnvioCorreo(
            LocalDateTime fechaEnvioCorreo
    ) {
        this.fechaEnvioCorreo = fechaEnvioCorreo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDateTime fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

}
