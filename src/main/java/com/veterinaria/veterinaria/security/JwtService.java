package com.veterinaria.veterinaria.security;


import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;



@Service
public class JwtService {


    private final String SECRET_KEY =
            "veterinaria-api-seguridad-jwt-clave-super-secreta-2026";


    private SecretKey getKey(){

        return Keys.hmacShaKeyFor(
                SECRET_KEY.getBytes()
        );

    }



    // GENERAR TOKEN JWT

    public String generarToken(
            String correo,
            String rol){


        return Jwts.builder()

                .subject(correo)

                .claim(
                        "rol",
                        rol
                )

                .issuedAt(
                        new Date()
                )

                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                + 1000 * 60 * 60 * 5
                        )
                )

                .signWith(
                        getKey()
                )

                .compact();

    }





    // EXTRAER CORREO DEL TOKEN

    public String extraerCorreo(String token){


        return Jwts.parser()

                .verifyWith(
                        getKey()
                )

                .build()

                .parseSignedClaims(token)

                .getPayload()

                .getSubject();


    }



    // VALIDAR TOKEN

    public boolean validarToken(String token){


        try{


            Jwts.parser()

                .verifyWith(
                        getKey()
                )

                .build()

                .parseSignedClaims(token);


            return true;


        }catch(Exception e){


            return false;


        }


    }


}