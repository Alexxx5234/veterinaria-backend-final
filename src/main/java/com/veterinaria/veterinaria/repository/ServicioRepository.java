package com.veterinaria.veterinaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.veterinaria.veterinaria.model.Servicio;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {

}