package com.preparcial.repository;

import com.preparcial.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Busca un usuario por su nombre de usuario (login)
    // Spring Data JPA implementa esto automáticamente basándose en el nombre del método.
    Optional<Usuario> findByUsername(String username);
}