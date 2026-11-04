package com.veterinaria.veterinaria.config;


import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.veterinaria.veterinaria.model.EstadoUsuario;
import com.veterinaria.veterinaria.model.Rol;
import com.veterinaria.veterinaria.model.Usuario;
import com.veterinaria.veterinaria.repository.UsuarioRepository;



@Configuration
public class DataInitializer {


    @Bean
    CommandLineRunner crearAdministrador(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder){


        return args -> {


            if(usuarioRepository.findByCorreo(
                    "admin@veterinaria.com"
            ).isEmpty()){



                Usuario admin = new Usuario();



                admin.setNombre(
                        "Administrador"
                );


                admin.setCorreo(
                        "admin@veterinaria.com"
                );


                admin.setPassword(
                        passwordEncoder.encode(
                                "Admin123"
                        )
                );


                admin.setRol(
                        Rol.ADMIN
                );


                admin.setEstado(
                        EstadoUsuario.APROBADA
                );


                admin.setVerificado(true);


                admin.setActivo(true);



                usuarioRepository.save(admin);



                System.out.println(
                    "ADMIN CREADO: admin@veterinaria.com / Admin123"
                );

            }


        };

    }

}