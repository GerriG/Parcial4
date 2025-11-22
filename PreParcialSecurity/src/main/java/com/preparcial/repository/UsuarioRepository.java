package com.preparcial.repository;

import com.preparcial.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);

    // NUEVO: Buscar por Email y Fecha de Nacimiento (Validación de seguridad)
    @Query("SELECT u FROM Usuario u WHERE u.perfil.email = :email AND u.perfil.fechaNacimiento = :fecha")
    Optional<Usuario> findByEmailAndFechaNacimiento(@Param("email") String email, @Param("fecha") LocalDate fecha);
}