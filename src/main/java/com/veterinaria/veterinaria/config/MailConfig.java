package com.veterinaria.veterinaria.config;

import java.util.Properties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;


@Configuration
public class MailConfig {


    @Bean
    public JavaMailSender javaMailSender() {

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();


        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);


        mailSender.setUsername("gogre18@gmail.com");
        mailSender.setPassword("eqybribxvjiyuhsg");


        Properties properties = mailSender.getJavaMailProperties();


        properties.put(
            "mail.smtp.auth",
            "true"
        );


        properties.put(
            "mail.smtp.starttls.enable",
            "true"
        );


        return mailSender;
    }

}