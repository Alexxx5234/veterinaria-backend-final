package com.veterinaria.veterinaria.model;

// ==========================================================
// IMPORTACIONES JPA
// ==========================================================

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

// ==========================================================
// IMPORTACIONES DE VALIDACIONES
// ==========================================================

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// ==========================================================
// IMPORTACIÓN JSON
// ==========================================================

import com.fasterxml.jackson.annotation.JsonIgnore;


// ==========================================================
// ENTIDAD VETERINARIO
// ==========================================================
//
// Representa la información profesional de un veterinario.
//
// NUEVA LÓGICA:
//
// 1. La persona se registra como usuario.
// 2. Verifica su correo electrónico.
// 3. El ADMIN selecciona el ID de ese usuario.
// 4. El ADMIN registra sus datos profesionales.
// 5. El sistema vincula las dos entidades.
// 6. El sistema asigna el rol VETERINARIO.
//
// El nombre completo y el correo se obtendrán
// de la cuenta del usuario.
//
// IMPORTANTE:
//
// La vinculación y el cambio de rol se realizarán
// desde el backend, únicamente por el ADMIN.
//
// ==========================================================

@Entity
@Table(name = "veterinarios")
public class Veterinario {


    // ======================================================
    // 1. IDENTIFICADOR DEL VETERINARIO
    // ======================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // ======================================================
    // 2. CÉDULA
    // ======================================================
    //
    // Dato profesional ingresado por el ADMIN.
    //
    // ======================================================

    @NotBlank(message = "La cedula es obligatoria")
    @Size(
        min = 10,
        max = 10,
        message = "La cedula debe tener 10 caracteres"
    )
    private String cedula;


    // ======================================================
    // 3. NOMBRE COMPLETO
    // ======================================================
    //
    // En el nuevo registro NO se solicitará nuevamente.
    //
    // Se copiará automáticamente desde:
    //
    // usuario.getNombre()
    //
    // Ejemplo:
    // Alexander Arévalo
    //
    // Conservamos este campo para mantener compatibilidad
    // con el sistema actual y sus respuestas JSON.
    //
    // ======================================================

    @NotBlank(
        message = "El nombre del veterinario es obligatorio"
    )
    private String nombre;


    // ======================================================
    // 4. APELLIDO (COMPATIBILIDAD)
    // ======================================================
    //
    // CAMBIO:
    // Ya no es obligatorio.
    //
    // El nuevo formulario NO solicitará este campo,
    // porque el nombre completo ya estará guardado
    // en Usuario.nombre.
    //
    // Se conserva temporalmente para evitar errores
    // en los controladores y frontend existentes.
    //
    // Más adelante podremos retirar este campo cuando
    // hayamos actualizado todas sus referencias.
    //
    // ======================================================

    private String apellido;


    // ======================================================
    // 5. ESPECIALIDAD
    // ======================================================
    //
    // El ADMIN registra la especialidad profesional.
    //
    // ======================================================

    @NotBlank(
        message = "La especialidad es obligatoria"
    )
    private String especialidad;


    // ======================================================
    // 6. TELÉFONO
    // ======================================================
    //
    // El ADMIN registra el teléfono de contacto.
    //
    // ======================================================

    @NotBlank(
        message = "El telefono es obligatorio"
    )
    private String telefono;


    // ======================================================
    // 7. CORREO ELECTRÓNICO
    // ======================================================
    //
    // En el nuevo registro se obtendrá automáticamente
    // desde la cuenta de usuario:
    //
    // usuario.getCorreo()
    //
    // No será necesario escribirlo nuevamente.
    //
    // Conservamos el campo para mantener compatibilidad
    // con las consultas y pantallas existentes.
    //
    // ======================================================

    @NotBlank(
        message = "El email es obligatorio"
    )
    @Email(
        message = "El email debe tener un formato válido"
    )
    private String email;


    // ======================================================
    // 8. RELACIÓN CON USUARIO
    // ======================================================
    //
    // Cada veterinario estará vinculado como máximo
    // a una cuenta de usuario.
    //
    // Ejemplo:
    //
    // USUARIOS
    // id = 2
    // nombre = Alexander Arévalo
    // correo = alexander@gmail.com
    // rol = VETERINARIO
    //
    // VETERINARIOS
    // id = 1
    // cedula = 0102345678
    // especialidad = Medicina general
    // usuario_id = 2
    //
    // @OneToOne:
    // Relación de uno a uno.
    //
    // unique = true:
    // Una cuenta no puede vincularse con varios
    // registros veterinarios.
    //
    // nullable = true:
    // Mantenemos compatibilidad con veterinarios
    // antiguos que aún no tengan cuenta vinculada.
    //
    // @JsonIgnore:
    // Evita exponer los datos de la cuenta de usuario
    // y recibir modificaciones de esa relación
    // desde los JSON normales de veterinarios.
    //
    // La asignación se realizará posteriormente
    // en una operación exclusiva para ADMIN.
    //
    // ======================================================

    @OneToOne(
        fetch = FetchType.LAZY,
        optional = true
    )
    @JoinColumn(
        name = "usuario_id",
        unique = true,
        nullable = true
    )
    @JsonIgnore
    private Usuario usuario;


    // ======================================================
    // 9. CONSTRUCTOR VACÍO
    // ======================================================
    //
    // Necesario para JPA.
    //
    // ======================================================

    public Veterinario() {

    }


    // ======================================================
    // 10. CONSTRUCTOR ORIGINAL
    // ======================================================
    //
    // Se conserva para no afectar otras clases
    // que puedan utilizar este constructor.
    //
    // ======================================================

    public Veterinario(
            Long id,
            String cedula,
            String nombre,
            String apellido,
            String especialidad,
            String telefono,
            String email
    ) {

        this.id = id;
        this.cedula = cedula;
        this.nombre = nombre;
        this.apellido = apellido;
        this.especialidad = especialidad;
        this.telefono = telefono;
        this.email = email;

    }


    // ======================================================
    // 11. GETTERS Y SETTERS
    // ======================================================
    //
    // Conservamos todos los métodos originales.
    //
    // ======================================================


    // ID

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    // CÉDULA

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }


    // NOMBRE COMPLETO

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }


    // APELLIDO (COMPATIBILIDAD)

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }


    // ESPECIALIDAD

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }


    // TELÉFONO

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }


    // EMAIL

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    // ======================================================
    // 12. USUARIO VINCULADO
    // ======================================================
    //
    // Estos métodos serán utilizados por el backend
    // para vincular el registro profesional con
    // su cuenta de usuario.
    //
    // ======================================================

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

}