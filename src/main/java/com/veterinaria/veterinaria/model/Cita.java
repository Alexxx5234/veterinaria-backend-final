package com.veterinaria.veterinaria.model;


import java.time.LocalDateTime;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;


import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;



@Entity
@Table(name = "citas")
public class Cita {


    /*
     * ========================================================
     * ID DE LA CITA
     * ========================================================
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    /*
     * ========================================================
     * FECHA Y HORA
     * ========================================================
     */

    @NotNull(message = "La fecha y hora de la cita es obligatoria")
    @FutureOrPresent(message = "La cita no puede ser de una fecha pasada")
    private LocalDateTime fechaHora;



    /*
     * ========================================================
     * MOTIVO DE LA CITA
     * ========================================================
     */

    @NotBlank(message = "El motivo de la cita es obligatorio")
    private String motivo;



    /*
     * ========================================================
     * ESTADO DE LA CITA
     *
     * Ejemplos:
     * PENDIENTE
     * CONFIRMADA
     * CANCELADA
     * COMPLETADA
     * ========================================================
     */

    private String estado;



    /*
     * ========================================================
     * MASCOTA
     *
     * Muchas citas pueden pertenecer
     * a una misma mascota.
     *
     * La mascota permite saber también
     * a qué usuario pertenece la cita.
     * ========================================================
     */

    @ManyToOne
    @JoinColumn(name = "mascota_id")
    @NotNull(message = "La mascota es obligatoria")
    private Mascota mascota;



    /*
     * ========================================================
     * VETERINARIO
     *
     * Muchas citas pueden ser atendidas
     * por un mismo veterinario.
     * ========================================================
     */

    @ManyToOne
    @JoinColumn(name = "veterinario_id")
    @NotNull(message = "El veterinario es obligatorio")
    private Veterinario veterinario;



    /*
     * ========================================================
     * SERVICIO
     *
     * NUEVA RELACIÓN.
     *
     * Muchas citas pueden utilizar
     * el mismo servicio.
     *
     * Ejemplos:
     * - Consulta general
     * - Vacunación
     * - Desparasitación
     * - Cirugía
     * ========================================================
     */

    @ManyToOne
    @JoinColumn(name = "servicio_id")
    @NotNull(message = "El servicio es obligatorio")
    private Servicio servicio;



    /*
     * ========================================================
     * CONSTRUCTOR VACÍO
     * ========================================================
     */

    public Cita() {

    }



    /*
     * ========================================================
     * CONSTRUCTOR COMPLETO
     * ========================================================
     */

    public Cita(
            Long id,
            LocalDateTime fechaHora,
            String motivo,
            String estado,
            Mascota mascota,
            Veterinario veterinario,
            Servicio servicio
    ) {

        this.id = id;
        this.fechaHora = fechaHora;
        this.motivo = motivo;
        this.estado = estado;
        this.mascota = mascota;
        this.veterinario = veterinario;
        this.servicio = servicio;

    }



    /*
     * ========================================================
     * GETTERS Y SETTERS
     * ========================================================
     */

    public Long getId() {
        return id;
    }



    public void setId(Long id) {
        this.id = id;
    }



    public LocalDateTime getFechaHora() {
        return fechaHora;
    }



    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }



    public String getMotivo() {
        return motivo;
    }



    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }



    public String getEstado() {
        return estado;
    }



    public void setEstado(String estado) {
        this.estado = estado;
    }



    public Mascota getMascota() {
        return mascota;
    }



    public void setMascota(Mascota mascota) {
        this.mascota = mascota;
    }



    public Veterinario getVeterinario() {
        return veterinario;
    }



    public void setVeterinario(Veterinario veterinario) {
        this.veterinario = veterinario;
    }



    public Servicio getServicio() {
        return servicio;
    }



    public void setServicio(Servicio servicio) {
        this.servicio = servicio;
    }

}