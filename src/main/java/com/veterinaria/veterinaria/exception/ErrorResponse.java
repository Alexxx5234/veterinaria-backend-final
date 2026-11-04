package com.veterinaria.veterinaria.exception;


import java.time.LocalDateTime;



public class ErrorResponse {


    private LocalDateTime fecha;

    private int codigo;

    private String error;

    private String mensaje;

    private String detalle;




    public ErrorResponse(
            LocalDateTime fecha,
            int codigo,
            String error,
            String mensaje,
            String detalle){


        this.fecha = fecha;
        this.codigo = codigo;
        this.error = error;
        this.mensaje = mensaje;
        this.detalle = detalle;

    }




    public LocalDateTime getFecha() {

        return fecha;

    }



    public int getCodigo() {

        return codigo;

    }



    public String getError() {

        return error;

    }



    public String getMensaje() {

        return mensaje;

    }



    public String getDetalle() {

        return detalle;

    }



}