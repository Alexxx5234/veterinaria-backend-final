package com.veterinaria.veterinaria.exception;


public class MensajeRespuesta {


    private int codigo;

    private String mensaje;

    private String detalle;



    public MensajeRespuesta(
            int codigo,
            String mensaje,
            String detalle) {

        this.codigo = codigo;
        this.mensaje = mensaje;
        this.detalle = detalle;

    }



    public int getCodigo() {
        return codigo;
    }


    public String getMensaje() {
        return mensaje;
    }


    public String getDetalle() {
        return detalle;
    }

}