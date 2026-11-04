package com.veterinaria.veterinaria.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinaria.veterinaria.model.Mascota;


public interface MascotaRepository extends JpaRepository<Mascota, Long> {


    /*
     * Busca todas las mascotas pertenecientes
     * a un usuario específico.
     */

    List<Mascota> findByUsuarioId(Long usuarioId);



    /*
     * Busca una mascota por su ID,
     * verificando además que pertenezca
     * al usuario indicado.
     */

    Optional<Mascota> findByIdAndUsuarioId(
            Long id,
            Long usuarioId
    );

}