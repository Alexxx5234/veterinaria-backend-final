
package com.veterinaria.veterinaria.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// ==========================================================
// ENTIDAD: ATENCION MEDICA VETERINARIA
// ==========================================================
// Guarda lo realizado durante una consulta.
// Cada atencion corresponde a una unica cita.
// Los medicamentos son opcionales: una consulta puede
// finalizar sin utilizar ni recetar medicamentos.
// ==========================================================
@Entity
@Table(name = "atenciones_medicas")
public class AtencionMedica {

    // Identificador automatico de la atencion.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Cita atendida. unique=true impide registrar dos
    // atenciones diferentes para la misma cita.
    // No usamos cascade: guardar una atencion no debe
    // crear ni modificar automaticamente otras citas.
    @NotNull(message = "La cita es obligatoria")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cita_id", nullable = false, unique = true)
    private Cita cita;

    // Diagnostico realizado por el veterinario.
    @NotBlank(message = "El diagnostico es obligatorio")
    @Size(max = 2000, message = "El diagnostico no debe superar 2000 caracteres")
    @Column(nullable = false, length = 2000)
    private String diagnostico;

    // Tratamiento o indicaciones de la consulta.
    @NotBlank(message = "El tratamiento es obligatorio")
    @Size(max = 2000, message = "El tratamiento no debe superar 2000 caracteres")
    @Column(nullable = false, length = 2000)
    private String tratamiento;

    // Notas complementarias: campo opcional.
    @Size(max = 2000, message = "Las observaciones no deben superar 2000 caracteres")
    @Column(length = 2000)
    private String observaciones;

    // Medicamentos efectivamente utilizados en la clinica.
    // OPCIONAL: es un registro descriptivo por ahora.
    // El descuento real de existencias se implementara
    // mas adelante, con el modulo de inventario.
    @Size(max = 2000, message = "Los medicamentos utilizados no deben superar 2000 caracteres")
    @Column(name = "medicamentos_utilizados", length = 2000)
    private String medicamentosUtilizados;

    // Medicamentos prescritos para su uso posterior.
    // OPCIONAL: recetar no significa consumir inventario.
    @Size(max = 2000, message = "Los medicamentos recetados no deben superar 2000 caracteres")
    @Column(name = "medicamentos_recetados", length = 2000)
    private String medicamentosRecetados;

    // Fecha en que se registro la atencion.
    @Column(name = "fecha_atencion", nullable = false, updatable = false)
    private LocalDateTime fechaAtencion;

    public AtencionMedica() {
    }

    // Asigna la fecha automaticamente antes del primer INSERT.
    @PrePersist
    public void establecerFechaAtencion() {
        if (fechaAtencion == null) {
            fechaAtencion = LocalDateTime.now();
        }
    }

    // ==========================================================
    // GETTERS Y SETTERS
    // ==========================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cita getCita() {
        return cita;
    }

    public void setCita(Cita cita) {
        this.cita = cita;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public void setDiagnostico(String diagnostico) {
        this.diagnostico = diagnostico;
    }

    public String getTratamiento() {
        return tratamiento;
    }

    public void setTratamiento(String tratamiento) {
        this.tratamiento = tratamiento;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getMedicamentosUtilizados() {
        return medicamentosUtilizados;
    }

    public void setMedicamentosUtilizados(String medicamentosUtilizados) {
        this.medicamentosUtilizados = medicamentosUtilizados;
    }

    public String getMedicamentosRecetados() {
        return medicamentosRecetados;
    }

    public void setMedicamentosRecetados(String medicamentosRecetados) {
        this.medicamentosRecetados = medicamentosRecetados;
    }

    public LocalDateTime getFechaAtencion() {
        return fechaAtencion;
    }
}
