package com.veterinaria.veterinaria.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.veterinaria.veterinaria.model.Mascota;
import com.veterinaria.veterinaria.repository.MascotaRepository;


@Service
public class MascotaService {


    private final MascotaRepository mascotaRepository;



    public MascotaService(
            MascotaRepository mascotaRepository
    ) {

        this.mascotaRepository = mascotaRepository;

    }



    /*
     * ========================================================
     * LISTAR TODAS LAS MASCOTAS
     *
     * Se utilizará principalmente por ADMIN.
     * ========================================================
     */

    public List<Mascota> listarTodos() {

        return mascotaRepository.findAll();

    }



    /*
     * ========================================================
     * LISTAR MASCOTAS DE UN USUARIO
     * ========================================================
     */

    public List<Mascota> listarPorUsuario(
            Long usuarioId
    ) {

        return mascotaRepository.findByUsuarioId(usuarioId);

    }



    /*
     * ========================================================
     * BUSCAR MASCOTA POR ID
     * ========================================================
     */

    public Optional<Mascota> buscarPorId(
            Long id
    ) {

        return mascotaRepository.findById(id);

    }



    /*
     * ========================================================
     * BUSCAR UNA MASCOTA QUE PERTENEZCA
     * A UN USUARIO ESPECÍFICO
     * ========================================================
     */

    public Optional<Mascota> buscarPorIdYUsuario(
            Long id,
            Long usuarioId
    ) {

        return mascotaRepository.findByIdAndUsuarioId(
                id,
                usuarioId
        );

    }



    /*
     * ========================================================
     * GUARDAR / ACTUALIZAR
     * ========================================================
     */

    public Mascota guardar(
            Mascota mascota
    ) {

        return mascotaRepository.save(mascota);

    }



    /*
     * ========================================================
     * ELIMINAR
     *
     * El Controller será el encargado de permitir
     * esta operación solamente al ADMIN.
     * ========================================================
     */

    public void eliminar(
            Long id
    ) {

        mascotaRepository.deleteById(id);

    }

}