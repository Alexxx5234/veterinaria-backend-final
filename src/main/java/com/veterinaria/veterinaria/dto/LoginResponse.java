package com.veterinaria.veterinaria.dto;


public class LoginResponse {


    private String token;


    private String rol;


    private String mensaje;



    public LoginResponse(
            String token,
            String rol,
            String mensaje){


        this.token = token;

        this.rol = rol;

        this.mensaje = mensaje;

    }



    public String getToken(){

        return token;

    }



    public String getRol(){

        return rol;

    }



    public String getMensaje(){

        return mensaje;

    }


}