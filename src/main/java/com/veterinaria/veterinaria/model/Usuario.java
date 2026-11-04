package com.veterinaria.veterinaria.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


/*
 * ============================================================
 * ENTIDAD USUARIO
 *
 * Representa a las personas que utilizan el sistema.
 *
 * Un usuario con rol USER funcionará directamente
 * como cliente de la veterinaria.
 * ============================================================
 */

@Entity
@Table(name = "usuarios")
public class Usuario {


    /* ========================================================
       ID DEL USUARIO
       ======================================================== */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    /* ========================================================
       DATOS DEL USUARIO
       ======================================================== */

    @Column(nullable = false)
    private String nombre;


    @Column(nullable = false, unique = true)
    private String correo;



    /*
     * La contraseña se puede recibir desde JSON,
     * pero no se mostrará en las respuestas de la API.
     */

    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;



    /* ========================================================
       ROL DEL USUARIO
       USER  = cliente
       ADMIN = administrador
       ======================================================== */

    @Enumerated(EnumType.STRING)
    private Rol rol;



    /* ========================================================
       ESTADO DE LA CUENTA
       ======================================================== */

    @Enumerated(EnumType.STRING)
    private EstadoUsuario estado;



    /*
     * Indica si el usuario verificó su correo.
     */

    private boolean verificado;



    /*
     * Indica si la cuenta está activa.
     */

    private boolean activo;



    /* ========================================================
       CÓDIGO DE VERIFICACIÓN

       Se envía al correo durante el registro.

       También se evita mostrarlo en las respuestas JSON.
       ======================================================== */

    @Column(length = 4)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String codigoVerificacion;



    /* ========================================================
       FECHA DE CREACIÓN
       ======================================================== */

    private LocalDateTime fechaCreacion;



    /* ========================================================
       CONSTRUCTOR VACÍO
       ======================================================== */

    public Usuario() {

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



    public String getCorreo() {
        return correo;
    }


    public void setCorreo(String correo) {
        this.correo = correo;
    }



    public String getPassword() {
        return password;
    }


    public void setPassword(String password) {
        this.password = password;
    }



    public Rol getRol() {
        return rol;
    }


    public void setRol(Rol rol) {
        this.rol = rol;
    }



    public EstadoUsuario getEstado() {
        return estado;
    }


    public void setEstado(EstadoUsuario estado) {
        this.estado = estado;
    }



    public boolean isVerificado() {
        return verificado;
    }


    public void setVerificado(boolean verificado) {
        this.verificado = verificado;
    }



    public boolean isActivo() {
        return activo;
    }


    public void setActivo(boolean activo) {
        this.activo = activo;
    }



    public String getCodigoVerificacion() {
        return codigoVerificacion;
    }


    public void setCodigoVerificacion(String codigoVerificacion) {
        this.codigoVerificacion = codigoVerificacion;
    }



    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }


    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

}