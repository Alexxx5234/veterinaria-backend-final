package com.veterinaria.veterinaria.model;


import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;



/*
 * ============================================================
 * ENTIDAD MASCOTA
 *
 * Cada mascota pertenece directamente a un Usuario.
 *
 * Un usuario con rol USER representa al cliente
 * dentro del sistema.
 *
 * Relación:
 *
 * USUARIO 1 -------- N MASCOTAS
 * ============================================================
 */

@Entity
@Table(name = "mascotas")
public class Mascota {


    /* ========================================================
       ID DE LA MASCOTA
       ======================================================== */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    /* ========================================================
       NOMBRE
       ======================================================== */

    @NotBlank(message = "El nombre de la mascota es obligatorio")
    private String nombre;



    /* ========================================================
       ESPECIE
       Ejemplo:
       Perro, Gato, Conejo, etc.
       ======================================================== */

    @NotBlank(message = "La especie es obligatoria")
    private String especie;



    /* ========================================================
       RAZA
       ======================================================== */

    @NotBlank(message = "La raza es obligatoria")
    private String raza;



    /* ========================================================
       EDAD
       ======================================================== */

    @NotNull(message = "La edad es obligatoria")
    @PositiveOrZero(message = "La edad no puede ser negativa")
    private Integer edad;



    /* ========================================================
       SEXO
       ======================================================== */

    @NotBlank(message = "El sexo es obligatorio")
    private String sexo;



    /* ========================================================
       USUARIO DUEÑO DE LA MASCOTA

       Muchas mascotas pueden pertenecer
       a un mismo usuario.

       Antes esta relación utilizaba Cliente.
       Ahora Usuario representa directamente al cliente.

       En PostgreSQL se utilizará:

       usuario_id
       ======================================================== */

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    @NotNull(message = "El usuario dueño es obligatorio")

    /*
     * Evita mostrar datos internos o sensibles
     * del usuario cuando se devuelve una mascota.
     */

    @JsonIgnoreProperties({
            "password",
            "codigoVerificacion",
            "verificado",
            "activo",
            "fechaCreacion"
    })
    private Usuario usuario;



    /* ========================================================
       CONSTRUCTOR VACÍO
       ======================================================== */

    public Mascota() {

    }



    /* ========================================================
       CONSTRUCTOR
       ======================================================== */

    public Mascota(
            Long id,
            String nombre,
            String especie,
            String raza,
            Integer edad,
            String sexo,
            Usuario usuario
    ) {

        this.id = id;
        this.nombre = nombre;
        this.especie = especie;
        this.raza = raza;
        this.edad = edad;
        this.sexo = sexo;
        this.usuario = usuario;

    }



    /* ========================================================
       GETTERS Y SETTERS
       ======================================================== */

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



    public String getEspecie() {
        return especie;
    }


    public void setEspecie(String especie) {
        this.especie = especie;
    }



    public String getRaza() {
        return raza;
    }


    public void setRaza(String raza) {
        this.raza = raza;
    }



    public Integer getEdad() {
        return edad;
    }


    public void setEdad(Integer edad) {
        this.edad = edad;
    }



    public String getSexo() {
        return sexo;
    }


    public void setSexo(String sexo) {
        this.sexo = sexo;
    }



    public Usuario getUsuario() {
        return usuario;
    }


    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

}